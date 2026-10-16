# Evidencia TDD: repositorios JDBC de Clientes y cableado transaccional

**Fecha:** 2026-10-15 · **Rama:** `julio` · **Autorización:** respuesta del usuario «Repositorios JDBC de Clientes» a la pregunta de qué parte del plan seguía (ver la entrada de la Bitácora «Repositorios JDBC de Clientes»).
**Plan de origen:** no hubo archivo `*.plan.md`. Las garantías salen de la entrada de la Bitácora «Eventos de Identidad y Clientes por el bus de Spring y el outbox» (sus pendientes), de D-103 y de `docs/diseno/clientes.md` (secciones 5, 6 y 7).

## Historias de usuario

- Como dueño de un negocio, quiero que mis clientes, avisos y consentimientos vivan en PostgreSQL y que ningún otro negocio los vea, para no perderlos al reiniciar ni mezclarlos.
- Como locatario, quiero registrar un cliente y sus consentimientos en una sola operación con sus eventos, para que nunca quede un cliente sin su evento ni al revés.
- Como persona cuyos datos se guardan, quiero que el comando preparado, que lleva mis datos, no quede guardado una vez usado o fallido (SC-10).
- Como plataforma, quiero que un token se use una sola vez aunque lleguen dos confirmaciones, y que quede usado aunque el comando falle.

## Ciclo TDD

**RED real (compilación).** Las pruebas se escribieron antes que el código y no compilaron por lo que faltaba: `EjecutorEnNegocio`, `ServicioPreparacion.hashDelComando` y el puerto `RepositorioPreparaciones` con negocio. `mvn test -Dtest=RepositoriosClientesJdbcPostgresTest,ClientesDePuntaAPuntaPostgresTest,PreparacionTest` terminó en `COMPILATION ERROR`.
**GREEN.** Con el código, las tres clases pasan a la primera: 19, 6 y 31 pruebas, 0 fallos.
**Línea base antes de empezar:** 1291 pruebas, 0 fallos. **Después:** `mvn -o -q verify`, **1318 pruebas, 0 fallos**.

## Hallazgos que cambiaron el diseño

1. **La seguridad por fila exige el negocio en cada operación.** `RepositorioPreparaciones` no lo recibía (`porHash(hash)`): la base no habría mostrado ninguna fila. Ahora `porHash`, `marcarUsada` y `vaciar` reciben `negocioId`.
2. **`jsonb` reordena el comando.** El hash del comando se calculaba sobre el texto original y no habría coincidido al leerlo de la base (todo token daría `COMANDO_CAMBIO`). `ServicioPreparacion.hashDelComando` es ahora canónico: ordena claves e ignora espacios; un cambio de valor sí lo cambia.
3. **La base impone el vaciado.** `usado_en IS NULL OR comando IS NULL` y `usado_en IS NOT NULL OR comando IS NOT NULL`: `marcarUsada` vacía el comando en la misma sentencia y `vaciar` de una preparación sin usar la marca como usada (el token queda quemado; una segunda confirmación da `TOKEN_USADO` y no `TOKEN_EXPIRADO` ni `COMANDO_CAMBIO`).
4. **Un error de Postgres aborta la transacción.** Por eso el único parcial de teléfono se resuelve con `ON CONFLICT DO NOTHING` y no con un `catch`: así aún se puede consultar quién tiene el número y citarlo en `TELEFONO_DUPLICADO`.

## Especificación de pruebas

| # | Qué se garantiza | Prueba | Tipo | Resultado |
|---|---|---|---|---|
| 1 | Un cliente guardado se lee igual (acentos, fecha, versión) | `RepositoriosClientesJdbcPostgresTest#cliente_insertadoSeLeeIgual` | integración | PASS |
| 2 | Un negocio no ve clientes de otro (por id, búsqueda y teléfono) | `#cliente_otroNegocioNoLoVe` | integración | PASS |
| 3 | Teléfono repetido: `TELEFONO_DUPLICADO` citando al existente; en otro negocio es válido | `#cliente_telefonoDuplicado_*`, `#cliente_mismoTelefonoEnOtroNegocio_esValido` | integración | PASS |
| 4 | Bloqueo optimista: versión esperada guarda y sube; vieja falla con `CONCURRENCIA`; teléfono ocupado falla | `#cliente_actualizar_*` | integración | PASS |
| 5 | Búsqueda por nombre sin acentos, por teléfono completo, solo activos, paginada por id | `#cliente_buscar_*` | integración | PASS |
| 6 | Aviso: publicar cierra el vigente; versión repetida falla con `CONCURRENCIA` sin dejar dos vigentes | `#aviso_*` | integración | PASS |
| 7 | Historial de consentimientos en orden y aislado por negocio | `#consentimiento_historialEnOrden` | integración | PASS |
| 8 | Preparación: guardar y leer; otro negocio no la ve; `marcarUsada` atómica; `vaciar` quema el token | `#preparacion_*` | integración | PASS |
| 9 | `marcarUsada` sobrevive al rollback de la transacción de fuera | `#preparacion_marcarUsada_sobreviveAlRollbackDeFuera` | integración | PASS |
| 10 | Una transacción no puede cambiar de negocio a la mitad | `#ejecutor_noCambiaDeNegocioEnLaMismaTransaccion` | integración | PASS |
| 11 | Publicar aviso, registrar cliente con 2 consentimientos y buscarlo: eventos en el outbox y completados | `ClientesDePuntaAPuntaPostgresTest#alta_guardaYPublicaEnElOutbox` | integración | PASS |
| 12 | Token de un solo uso y ligado al negocio | `#token_seUsaUnaVez`, `#token_deOtroNegocio_noSirve` | integración | PASS |
| 13 | Un alta que falla en la confirmación no deja cliente ni evento | `#alta_fallida_noDejaClienteNiEvento` | integración | PASS |
| 14 | Tras confirmar, la fila no conserva el comando (SC-10) | `#preparacion_trasConfirmar_quedaVacia` | integración | PASS |
| 15 | El comando guardado como `jsonb` (otro orden y espacios) sigue confirmando; el hash es canónico | `PreparacionTest#confirmar_comandoReformateado*`, `#hashDelComando_esCanonico` | unidad | PASS |

## Cobertura y huecos conocidos

`mvn -o -q verify` (JaCoCo): `RepositorioClientesJdbc` 97 % (instrucciones), `RepositorioAvisosJdbc` 98 %, `RepositorioPreparacionesJdbc`, `RepositorioConsentimientosJdbc`, `EjecutorEnNegocio`, `ManejadorTransaccional` y `ClientesConfiguracion` 100 %. Sin cubrir: la rama de `DuplicateKeyException` en `actualizar` (la carrera entre dos altas simultáneas, que no se simuló) y una ruta de error de lectura del JSON de finalidades.
No hay prueba de concurrencia real (dos hilos confirmando el mismo token): el conteo del `UPDATE` lo garantiza por diseño y la prueba #8 cubre el resultado secuencial.
