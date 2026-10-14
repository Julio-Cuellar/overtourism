# Evidencia TDD: Clientes, aviso de privacidad, alta, edición, búsqueda y preparar → confirmar (HTC-25 a HTC-29)

**Fecha:** 2026-10-14 · **Rama:** `julio` · **Autorización:** «autorizo programar Clientes», registrada en `docs/BITACORA.md` (2026-10-13).
**Alcance:** etapa I2 del Mapa de Clientes **sin base de datos** (el equipo no tiene Docker encendido). Todo corre sobre puertos y dobles en memoria; los adaptadores reales llegan con HTC-24.
**Historias:** HTC-25 `PublicarAvisoPrivacidad` · HTC-26 `RegistrarCliente` · HTC-27 `EditarCliente` · HTC-28 búsqueda y sugerencias · HTC-29 patrón preparar → confirmar.

## Recorridos de usuario que cubre esta etapa

1. Como locatario, quiero publicar el aviso de privacidad de mi negocio y que no se pueda registrar a nadie sin uno vigente (L-01, L-02).
2. Como locatario, quiero dar de alta a un cliente con su teléfono, correo y consentimientos, que el sistema los normalice, avise si ya existe y no guarde nada hasta que yo confirme (I-01 a I-03, I-07, I-08, P-04, L-03).
3. Como locatario, quiero corregir los datos de un cliente sin pisar la edición de otra persona (I-06).
4. Como locatario, quiero encontrar a un cliente por parte del nombre o por su teléfono completo, ver una lista cuando haya varios y que las listas no muestren datos de más (I-04, I-05, P-05, SC-02, SC-03).
5. Como equipo, quiero que una confirmación sirva una sola vez, ligada a su negocio, su actor y su comando, y que el comando con datos personales no quede guardado (D-23, SC-10).

## Ciclo RED → GREEN

| Paso | Qué se hizo | Resultado |
|---|---|---|
| Línea base | `mvn -B verify` sin tocar nada | 143 pruebas en verde |
| **RED 1** | 24 clases de prueba y helpers escritos sin código de producción | `mvn test-compile`: no compila; javac corta en 100 errores, todos «cannot find symbol» o «package does not exist» sobre las clases que aún no existían |
| **GREEN 1** | Dominio, puertos y casos de uso | `mvn -B verify`: **341 de 341** a la primera, con la verificación de Spring Modulith y 97.7 % de líneas |
| Mutaciones | Se rompió a propósito cada una de 3 reglas y se restauró | Cada defecto hizo fallar la prueba esperada: token vencido que seguía sirviendo (`confirmar_aLosCincoMinutos_fallaConTokenExpirado`), teléfono duplicado sin revisar (`preparar_telefonoDuplicado_fallaConElIdDelExistente`) y edición sin bloqueo de versión (`preparar_versionVieja_fallaConConcurrencia`) |
| Revisión | `ecc:java-reviewer` y `ecc:security-reviewer` (solo lectura) | Java: 2 ALTOS, 7 MEDIOS, 6 BAJOS. Seguridad: 0 críticos, 1 ALTO, 3 MEDIOS, 6 BAJOS. Ver abajo |
| **RED 2** | Pruebas nuevas para lo que se decidió corregir, con el código sin cambiar (solo lo mínimo para compilar) | 409 pruebas, **52 fallan y 2 dan error**, todas por un hallazgo |
| **GREEN final** | Correcciones | **410 de 410**, `BUILD SUCCESS`, «All coverage checks have been met». **98.4 % de líneas y 95.3 % de ramas** |

**Limitación, dicha con franqueza:** `ZonaMexicoTest` (6 pruebas) pasó a la primera porque `ZonaMexico` se escribió antes del RED 2 para que el proyecto compilara. Las 52 + 2 fallas del RED 2 son las demás.

## Hallazgos de la revisión y qué se hizo

| Hallazgo (quién) | Qué se hizo |
|---|---|
| `toString` de los records vuelca datos personales y el token (ambos, ALTO/MEDIO) | `DatosCliente`, `Cliente`, `RegistrarClienteComando`, `EditarClienteComando`, `Preparacion`, `PreparacionCreada`, `ClienteListado` y `PaginaClientes` imprimen una forma redactada. `ToStringSinDatosPersonalesTest` siembra un nombre, un teléfono, un correo, una fecha, unas notas y un token, y exige que no salgan |
| Comando con datos personales sin vaciar si el token vence o el comando cambia (seguridad ALTO) | Se vacía en esos dos caminos. No se vacía si el token es de otro negocio u otro actor (un tercero no puede borrar preparaciones ajenas) |
| `vaciar` que falla enmascara el error original, o convierte un alta exitosa en error (Java ALTO, parte C) | Con error previo, el fallo de `vaciar` queda como suprimido; tras un éxito, se registra solo el nombre de la clase y se devuelve el resultado |
| Caracteres de control, NUL, invisibles y espacios Unicode (ambos, MEDIO) | Nombre y apellidos rechazan `Cc`, `Cf`, separadores de línea y de párrafo; los espacios Unicode de los extremos se recortan y los de en medio pasan a espacio normal. El correo admite solo ASCII de la lista permitida. Las notas descartan lo prohibido y conservan `\n` y `\t`. Los textos del aviso lo rechazan |
| Teléfono con NBSP, tope de entrada (Java MEDIO, seguridad BAJO) | Los espacios Unicode son separadores válidos; una entrada de más de 40 caracteres se rechaza sin analizarla |
| Un texto que normaliza a vacío busca a todos (Java MEDIO) | `CriterioBusqueda` devuelve `Ninguno`; y `Ninguno` ya ni consulta el repositorio |
| Invariantes que se podían saltar (Java MEDIO) | `Cliente` y `Consentimiento` exigen versiones ≥ 1; un aviso no se cierra antes de empezar |
| `LocalDate.now(reloj)` depende de la zona del reloj (Java MEDIO) | Nueva `ZonaMexico.hoy(reloj)`, probada con relojes en UTC |
| Tope de token, de finalidades; cursor permisivo; `ObjectMapper` externo (BAJOS) | Token ≤ 64 caracteres, ≤ 20 finalidades, cursor solo en forma canónica y `ServicioPreparacion` crea siempre su propio mapeador estricto |
| Pruebas débiles (Java 12, 18, 19) | Se probó la rama de JSON ilegible con hash coincidente, el flake de «1990» se eliminó comparando solo los valores de `datos`, y la prueba de reflexión de `ClientesApi` revisa toda operación que reciba un texto |

### Lo que NO se corrigió y por qué

| Hallazgo | Dónde queda |
|---|---|
| Purga a las 24 h de preparaciones que nunca se confirman (ALTO seguridad, MEDIO Java 11) | **HTC-38**. Hoy esas filas conservan el comando hasta que exista la purga. El puerto aún no tiene el método |
| Contrato transaccional de `confirmar` y atomicidad del alta (ALTO Java 1) | **HTC-24**. Se dejó la política propuesta en el Javadoc de `ServicioPreparacion`; hay que confirmarla con el equipo |
| Consentimiento contra las finalidades del aviso, y `APP_TURISTA` desde el panel (MEDIO ambos) | **HTC-34 (I4)**. Necesita decidir cómo se relaciona el código de texto libre del aviso con el enum `Finalidad` |
| Reintento idempotente de `confirmar` (Java 8) | Decisión abierta: exige agregar `recurso_id` a `preparacion` (cambia la sección 5.2 del diseño) |
| `sugerir` trunca sin avisar y ordena por antigüedad (Java 10) | Decisión abierta: ranking o indicador de truncado |
| `paginaEfectiva` recorta en silencio, y SC-03 dice «pedir 500 fichas falla» (Java 13) | Decisión abierta: alinear código o diseño |
| Escape de `LIKE`, HMAC del comando, índice único `(negocio_id, version)` del aviso, traducción de `TELEFONO_DUPLICADO` con el id existente | Notas para el adaptador de **HTC-24** |

## Especificación de pruebas

| N.º | Qué se garantiza | Prueba | Resultado |
|---|---|---|---|
| 1 | UUIDv7 con versión y variante correctas, instante recuperable, ordenable por milisegundo, único dentro del milisegundo | `UuidV7Test` (6) | PASA |
| 2 | El contexto exige negocio y actor | `ContextoNegocioTest` (5) | PASA |
| 3 | La fecha «de hoy» es la de México aunque el reloj esté en UTC | `ZonaMexicoTest` (6) | PASA |
| 4 | Teléfono E.164, rechazo de inválidos y de entradas enormes, máscara con 4 dígitos | `TelefonoTest` (29) | PASA |
| 5 | Correo en minúsculas y lista permitida; texto de búsqueda sin acentos | `CorreoYTextoBusquedaTest` (20) | PASA |
| 6 | I-01, I-02, I-07, I-08, P-01: normalización y códigos de error de los datos | `DatosClienteTest` (11) | PASA |
| 7 | Caracteres prohibidos en nombre, correo, notas y aviso | `CaracteresTest` (32) | PASA |
| 8 | Ningún `toString` imprime datos personales ni el token | `ToStringSinDatosPersonalesTest` (6) | PASA |
| 9 | Los códigos de error y su mensaje (solo el código) | `ClientesExceptionTest` (3) | PASA |
| 10 | Invariantes de `Cliente`, `Consentimiento` y `AvisoPrivacidad` | `InvariantesTest` (3), `AvisoPrivacidadTest` (7) | PASA |
| 11 | Reglas de búsqueda: nombre, teléfono completo, nunca parcial, nunca correo, `Ninguno` | `CriterioBusquedaTest` (21) | PASA |
| 12 | Aviso: versión 1, versión 2 que cierra la 1, contenido incompleto, aislamiento por negocio | `PublicarAvisoTest` (19) | PASA |
| 13 | Token: opaco, solo su hash, 5 minutos, de un solo uso, ligado a negocio, actor y comando, comando vaciado, errores sin datos | `PreparacionTest` (29) | PASA |
| 14 | Alta: normalización, duplicados, aviso, consentimientos, eventos sin datos personales | `RegistrarClienteTest` (26) | PASA |
| 15 | Edición: bloqueo optimista, anonimizado, duplicados, borrar opcionales | `EditarClienteTest` (16) | PASA |
| 16 | Búsqueda: acentos, teléfono completo, listas, paginación, tope de 50, aislamiento | `BuscarClientesTest` (24) | PASA |
| 17 | Cada código de error tiene su plantilla, sin repetirse | `PlantillasClientesTest` (4) | PASA |

## Cobertura y huecos conocidos

- 98.4 % de líneas (674/685) y 95.3 % de ramas (328/344). El `pom.xml` exige 80 %.
- Sin cubrir en esta etapa: `NoSuchAlgorithmException` de SHA-256 y el `catch` de serialización de `ServicioPreparacion` (no se pueden provocar sin manipular la JVM), y ramas defensivas de `FinalidadAviso`, `Telefono` y `AvisoPrivacidad`.

## Lo que esta etapa NO verifica

- **Nada toca una base de datos.** La unicidad del teléfono, el bloqueo optimista, la versión única del aviso y que `marcarUsada` sea atómico están modelados en dobles en memoria. Hace falta una prueba de integración con Postgres (HTC-24) que corra la misma batería contra el doble y contra la base.
- **No hay cableado de Spring ni transacciones.** Las clases no llevan `@Service`; el contexto de la aplicación no las ve. Tampoco hay outbox: `PublicadorEventos` es un puerto.
- **Sin autenticación ni resolvedores GraphQL.** El negocio y el actor llegan en un `ContextoNegocio` que hoy construye la prueba. `/graphql` sigue sin autenticación hasta HTC-44.
- **El aviso se valida estructuralmente.** No puede juzgar si el texto cumple el art. 15 de la LFPDPPP: lo debe revisar una persona con formación jurídica (HTC-52).
