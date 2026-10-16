# Evidencia de TDD: Identidad, registro ampliado e IDN-2 (membresías, roles y alta de negocio por eventos)

Fecha: 2026-10-15. Rama `julio`, sin commits. Autorización: entrada «2026-10-15 — Autorización de IDN-2 y del registro ampliado de Identidad» de `docs/BITACORA.md`.
Plan de origen: el plan presentado con `/ecc:plan` en la sesión del 2026-10-15 (no hay archivo `*.plan.md`), con la corrección del usuario sobre el alta de negocios (D-78). Las historias de Jira que toca: IDN-1 (cambio de contrato), IDN-2.1 a IDN-2.6 (HTC-108 a HTC-113) y, por la corrección, un reemplazo de IDN-3.1 (HTC-114): Identidad ya no registra el negocio, publica un evento.

**Alcance de esta evidencia:** código en memoria más el esquema de Postgres (migraciones `V021` y `V022`, probadas contra PostgreSQL 17 real con Testcontainers). Los repositorios JDBC de Identidad, la confirmación reforzada (IDN-3.2) y la gestión del equipo por REST (IDN-3.3 a 3.5) **no** se hicieron: faltan autorizaciones y están fuera del plan aprobado.

## Qué se hizo

| Etapa | Qué queda |
|---|---|
| 1. Registro ampliado | `POST /registro` con nombre, apellidos, correo, celular y aviso de privacidad. Acceso con celular, correo o id (`Identificador`). `POST /auth/sesion` ya no crea cuentas: un celular sin cuenta recibe `REGISTRO_REQUERIDO` sin gastar el código. El código sale en la consola (`[CANAL SIMULADO]`, celular enmascarado). Migración `V021`. |
| 2. IDN-2 | `compartido.seguridad` (permisos como datos, `Autorizacion`), `RolesBase` (8 roles y 3 plantillas por giro), membresías con estados, negocio activo (`PUT /auth/negocio-activo` emite otro token), `GET /roles`, `GET /permisos`, perfil con rol y permisos, administración del equipo a nivel de servicio (`EquipoApi`). Migración `V022`. |
| D-78 | Identidad **no** registra negocios: `POST /registro/negocio` solo publica `negocio.registro_solicitado` (con el id que Identidad asigna) y `EscuchaDeNegocios` consume `negocio.registrado` para dejar a quien lo pidió como DUENO. Mientras el módulo de negocios no exista, `POST /dev/negocios/{id}/registrado` (solo desarrollo) hace sus veces. |
| Postman | `identidad.postman_collection.json` (44 solicitudes, 5 carpetas) y entornos `identidad.local` e `identidad.ejemplo` (este último se eliminó después; ver la Bitácora del 2026-10-15). |

## Resultado final

| Medida | Resultado | Comando |
|---|---|---|
| Suite completa | **1125 pruebas, 0 fallas, 0 omitidas** (824 al inicio del día; +301; 1101 antes de corregir las revisiones) | `mvn -B verify` |
| Cobertura de Identidad | 99.5 % de líneas (985/990), 95.8 % de ramas (369/385) | JaCoCo, `target/site/jacoco/jacoco.csv` |
| Cobertura de Clientes | 99.3 % de líneas, 96.6 % de ramas | ídem |
| Cobertura de `compartido` | 96.5 % de líneas, 93.6 % de ramas | ídem |
| Regla de cobertura (80 %) | cumplida: «All coverage checks have been met» | `mvn -B verify` |
| Estructura de módulos | `EstructuraModulosTest` en verde: `identidad` solo depende de `compartido`; Clientes declara sus permisos sin que Identidad lo importe | `mvn -B verify` |

## Ciclos RED → GREEN

| Ciclo | RED observado | GREEN observado |
|---|---|---|
| 1. Registro ampliado, identificadores y canal por consola | `mvn test-compile` falla con **144 errores de compilación**, todos por clases o métodos que no existían (`Correo`, `Identificador`, `SolicitudDeRegistro`, `registrar`, `NOMBRE_DEL_REGISTRO`, …) | 928 pruebas; **2 fallas, ambas errores de mis expectativas y no del código**: `CorreoTest` esperaba rechazar `ana@x.com\n`, pero `strip()` lo deja válido (se quitó el caso), y una prueba de Postgres esperaba el SQLSTATE 23514 para un correo de 126 caracteres cuando la columna `VARCHAR(120)` da 22001 (se corrigió la expectativa) |
| 2. IDN-2 y alta de negocio por eventos | `mvn test-compile` falla con **100 errores (el tope de `javac`)** por los tipos nuevos (`PermisoDeclarado`, `RolesBase`, `Membresia`, `EquipoServicio`, `EscuchaDeNegocios`, `elegirNegocio`, …) | 1101 pruebas; **1 falla en una prueba existente**: `EsquemaClientesPostgresTest.aislamiento_todasLasTablasConNegocioLaTienenForzada` exigía seguridad por fila en toda tabla con columna `negocio_id`; `identidad.negocio_ref` es la copia global del negocio y su `negocio_id` es la llave. Se agregó a la excepción que ya existía para `identidad.sesion` (decisión mía: se cambió una prueba existente, queda a revisión) |

Nota honesta sobre el RED: en los dos ciclos fue **de compilación** (las pruebas referencian clases que aún no existían). No se ejecutó cada prueba nueva contra un código anterior para verla fallar una por una, y esta vez **no se hicieron pruebas de mutación a mano** como en IDN-1. Quedan como pendiente para quien revise.

## Qué garantizan las pruebas

| # | Qué se garantiza | Dónde | Tipo | Resultado |
|---|---|---|---|---|
| 1 | Un registro completo guarda nombre, apellidos, correo (en minúsculas), celular E.164, aviso aceptado y abre sesión; el correo queda sin verificar | `RegistrarCuentaTest` | unidad | PASS |
| 2 | Los errores de forma (nombre, apellidos, correo, aviso, celular) se detectan **antes** de tocar el código, que no se gasta | `RegistrarCuentaTest` | unidad | PASS |
| 3 | Celular o correo ya registrados fallan con `CUENTA_YA_EXISTE` o `CORREO_NO_DISPONIBLE` sin gastar el código | `RegistrarCuentaTest` | unidad | PASS |
| 4 | Ocho registros simultáneos con el mismo código dejan una cuenta y una sesión | `RegistrarCuentaTest` | concurrencia | PASS |
| 5 | Pedir el código con el correo o el id lo envía al celular de la cuenta; con una cuenta que no existe no falla ni envía nada (SI-02) | `AccesoPorIdentificadorTest` | unidad | PASS |
| 6 | El tope de 3 códigos por 15 minutos lo comparten celular, correo e id | `AccesoPorIdentificadorTest` | unidad | PASS |
| 7 | El código se escribe en un solo registro, `identidad.canal.simulado`, con el celular enmascarado; ningún otro registro lleva códigos, tokens ni datos personales | `CanalSimuladoTest`, `RegistrosSinSecretosTest` | unidad e integración | PASS |
| 8 | Los 8 roles base, las 3 plantillas, y que un rol que cita un permiso sin declarar impide el arranque (AZ-03) | `RolesBaseTest`, `CatalogoDePermisosPlataformaTest` | unidad e integración | PASS |
| 9 | Pedir un negocio publica un solo evento `negocio.registro_solicitado` sin datos personales y **no crea** el negocio ni la membresía | `AltaDeNegocioTest` | unidad | PASS |
| 10 | Al llegar `negocio.registrado`, quien lo pidió queda DUENO y se publica `membresia_agregada`; entregas repetidas son idempotentes; avisos que no se pueden aplicar se ignoran | `AltaDeNegocioTest`, `EscuchaDeNegociosTest`, `NegocioRegistradoV1Test` | unidad | PASS |
| 11 | Con un negocio la sesión entra directa; con varios elige; elegir emite otro token, revoca el anterior y no alarga el tope; un negocio ajeno da el mismo error que uno inexistente | `NegocioActivoTest` | unidad | PASS |
| 12 | Si la membresía deja de estar activa, el token cae en la siguiente petición; el rol se renueva sin esperar a que venza la sesión (AM-06, AM-08) | `NegocioActivoTest`, `GestionDeEquipoTest` | unidad | PASS |
| 13 | Anti-escalada: nadie otorga permisos que no tiene, un administrador no toca a un dueño, nadie opera sobre sí mismo, siempre queda un dueño activo, solo roles de la plantilla del giro | `GestionDeEquipoTest` | unidad | PASS |
| 14 | `V021` y `V022` se aplican sobre PostgreSQL real; los catálogos sembrados coinciden con el código; la seguridad por fila de `membresia` aísla por negocio y deja a cada persona leer las suyas | `EsquemaIdentidadPostgresTest`, `EsquemaEquipoPostgresTest` | integración | PASS |
| 15 | Contratos REST: códigos y estados HTTP de cada error, cabeceras de no-cache, un `negocioId` en el cuerpo se ignora | `RegistroControllerTest`, `NegociosControllerTest`, `AuthControllerTest`, `ErroresYRespuestasTest` | integración | PASS |

## Verificación en una instancia real

- Se arrancó el backend con `-Dspring-boot.run.profiles=desarrollo` contra la base de Docker. Flyway aplicó `V021` y `V022` (la base de desarrollo quedó en la versión 22). El puerto 8080 estaba ocupado por otra instancia (con código anterior) que **no se tocó**; la prueba corrió en el 8081 y se detuvo al terminar.
- La consola mostró `[CANAL SIMULADO] Código de acceso para •••• 0001: …`.
- Se ejecutaron las **44 solicitudes** de la colección de Postman con un ejecutor mío en Python (no hay Node ni Newman en esta máquina): comprueba el estado HTTP y el código de error de cada una y captura el token, el id de la cuenta y el id del negocio. Resultado: **44 de 44** con el entorno `identidad.ejemplo`. Con el entorno `identidad.local` vacío, las carpetas 1, 2 y 3 **no salen** y avisan qué datos faltan.

## No verificado / límites

- **Los scripts `pm.test` de la colección no se ejecutaron en Postman ni en Newman.** Lo que sí se comprobó es el estado y el código de error de cada solicitud con el ejecutor de arriba.
- Los repositorios siguen **en memoria**: cuentas, membresías, negocios y eventos se pierden al reiniciar. Las migraciones existen y están probadas, pero ningún módulo las usa todavía. El `PublicadorEventos` de desarrollo guarda en memoria; no hay adaptador del outbox de Spring Modulith.
- `EscuchaDeNegocios` usa `@ApplicationModuleListener`, que solo recibe eventos publicados dentro de una transacción con el bus real. Aquí se probó llamándolo directamente (y con el simulador de desarrollo); **no se probó con el bus** porque el módulo de negocios no existe.
- No hay límite de solicitudes para `POST /registro/negocio`: una persona autenticada puede publicar muchos eventos.
- El correo no se verifica (no hay canal): quien registre el correo de otra persona con su propio celular lo ocupa.
- Cobertura: las pruebas de mutación a mano no se repitieron.

## Resultado de las revisiones

Se pidieron `ecc:java-reviewer` y `ecc:security-reviewer` (solo lectura), como decía el plan, y se corrigió dentro del alcance con las pruebas primero. Cada hallazgo corregido tiene una prueba que falla si se deshace la corrección: se comprobó con **cinco mutaciones a mano** (se rompió el código, falló la prueba esperada, se restauró el archivo y se comprobó con `diff`).

| Hallazgo | Quién | Qué se hizo | Prueba |
|---|---|---|---|
| `POST /registro` sondea correos con un solo código (el 409 no gastaba intento) | ambas, alta | El rechazo por correo ocupado cuenta como intento fallido: 5 sondeos agotan el código | `RegistrarCuentaTest` (2 pruebas); mutación M1 detectada |
| `negocio.registrado` hacía dueña a la cuenta que dijera el evento | ambas, alta | Se anota el pedido (`SolicitudesDeNegocio`); solo se aplica si Identidad lo pidió, a nombre de quien lo pidió y con origen `negocios`; el pedido se consume al aplicarlo | `AltaDeNegocioTest` (6 pruebas), `NegocioRegistradoV1Test`; mutaciones M2 y M3 detectadas |
| Con correo o id, un código vencido o agotado delata que la cuenta existe | seguridad, alta | Con correo o id, `CODIGO_EXPIRADO` y `CODIGO_AGOTADO` salen como `CODIGO_INVALIDO`; con el celular se conservan | `AccesoPorIdentificadorTest` (2 pruebas); mutación M4 detectada |
| Con correo o id, el tope del celular agotado devolvía 429 solo si la cuenta existía | seguridad, media | Se omite el envío y se responde 202 igual | `AccesoPorIdentificadorTest`; mutación M5 detectada |
| `POST /registro/negocio` sin tope | ambas, media | Tope de 3 por cuenta cada 15 minutos | `AltaDeNegocioTest.solicitar_tieneTopePorCuenta` |
| El publicador de eventos en memoria crece sin límite | seguridad, media | Conserva los últimos 1000 | `PublicadorEventosEnMemoriaTest` |
| La llave única de `V022` impedía «invitar de nuevo» tras revocar | código, media | Índice único parcial (solo cuentan las no revocadas) y repositorio en memoria con la misma regla; un disparador impide que una revocada vuelva y que una membresía cambie de cuenta o de negocio | `RepositoriosEnMemoriaTest`, `EsquemaEquipoPostgresTest` (4 pruebas nuevas) |
| La política de seguridad por fila de `membresia` dejaba escribir a quien solo tenía la cuenta; `negocio_ref` era editable por completo | seguridad, media | Políticas separadas (leer: negocio o cuenta; escribir: solo el negocio de la transacción); de `negocio_ref` la aplicación solo cambia el estado | `EsquemaEquipoPostgresTest` |
| Un aviso que no se puede aplicar se descartaba sin rastro | código y seguridad | Se deja un `WARN` con el negocio y el evento, sin datos personales | `AltaDeNegocioTest` |
| Se perdía la correlación al reaccionar a `negocio.registrado` | código, baja | `membresia_agregada` conserva el hilo de la solicitud | `AltaDeNegocioTest`, `NegocioRegistradoV1Test` |
| El escucha solo existía en la configuración de desarrollo | código, baja-media | Se movió a la configuración común. **Corrección posterior (2026-10-15):** se documentó que no había `@EnableAsync` y eso era falso; Spring Modulith lo activa por sí solo, así que el escucha corre en otro hilo (ver `docs/pruebas/identidad-registro-unico.tdd.md`) | `PerfilProduccionTest` |

**Documentado, no corregido** (dependen de la persistencia o son decisiones del equipo):

- **Carrera del «último dueño» con Postgres** (ambas): ahora lo dice el Javadoc de `EquipoServicio` y el encabezado de `V022`; los repositorios JDBC deben bloquear la fila de `negocio_ref` (`FOR UPDATE`) y repetir el conteo.
- **Una sesión revocada podría «resucitar» al renovarse con Postgres** (ambas): documentado en `RepositorioSesiones`; la renovación debe ser un `UPDATE ... WHERE revocada_en IS NULL`.
- **El envío del código ocurre dentro del `synchronized` que también autentica** (ambas) y **la latencia delata la cuenta** al pedir el código con correo o id: documentado en `CanalDeCodigos`; se arregla al enviar fuera de la transacción o por outbox.
- **Bloqueo selectivo de una persona** (agotar sus 3 códigos, o los 5 intentos del que tiene), **la IP detrás de un proxy** y **el bombardeo de SMS**: son del diseño del acceso (el `desafioId` que sigue pendiente de decisión, un tope global del canal y un resolvedor de IP de confianza). No se tocaron.
- **El correo sin verificar no debe usarse para invitaciones ni acceso hasta que se verifique** (seguridad): queda como restricción para IDN-3.
- **Credenciales fijas `postgres`/`root` en `application-desarrollo.yml`** (seguridad, baja): lo pidió el usuario para el perfil de desarrollo; se anota para que no llegue a un servidor.
- **Código sin productor todavía** (`CERRADO`, `Origen.INVITACION`, los permisos de invitar y de auditoría, `EquipoApi` sin REST): es andamiaje de IDN-3, lo reconoce la documentación.
- **Sin atomicidad entre guardar el negocio y la membresía** y **sin `Idempotency-Key` en `POST /registro/negocio`**: dependen de la transacción de Postgres y de una decisión de contrato.

Las revisiones descartaron: bloqueo mutuo entre `IdentidadServicio` y `EquipoServicio`; incoherencias entre `RolesBase` y `V022` (coinciden los 8 roles, los 16 permisos y las 3 plantillas); datos personales en eventos; que `identidad` importe a otro módulo; escalada de privilegios en `EquipoServicio`; y piezas de desarrollo activas con el perfil `produccion`.

## Revisión de la migración tras corregir

`V022` cambió después de aplicarse a la base de desarrollo, así que se deshizo **solo** `V022` ahí (las tablas estaban vacías: 0 cuentas, 0 membresías, 0 negocios) y Flyway la volvió a aplicar. Se volvió a arrancar el backend (puerto 8081) y las 44 solicitudes de la colección respondieron lo esperado (**44 de 44**).
