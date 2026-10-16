# Evidencia de TDD: Identidad, alta única de persona y negocio y activación implícita (Etapa A)

Fecha: 2026-10-15. Rama `julio`, sin commits. Autorización: entrada «2026-10-15 — Autorización de la Etapa A: alta única de persona y negocio, y activación implícita» de `docs/BITACORA.md` (D-79 y D-80). D-81 a D-83 (turista, acceso compartido, invitaciones) son diseño y **no** se programaron.
Plan de origen: el plan «registro de turistas y de personal de negocios» presentado con `/ecc:plan` el 2026-10-15 (no hay archivo `*.plan.md`).

## Qué cambia para quien se registra

| Camino | Antes | Ahora |
|---|---|---|
| Dueño nuevo | 5 llamadas y una espera: código, `POST /registro`, `POST /registro/negocio`, `GET /auth/yo` repetido hasta que aparezca el negocio, `PUT /auth/negocio-activo` (otro token) | **2 llamadas** (código y `POST /registro` con el bloque `negocio`) y `GET /auth/yo`, que ya dice «pendiente» o «listo». Sin elegir negocio ni cambiar de token |
| Dueño que vuelve con un negocio | 1 llamada; el negocio se activaba al iniciar sesión | Igual, y además una sesión abierta antes de que exista el negocio lo ve activo sin volver a entrar |
| Dueño con dos o más negocios | Elegir con `PUT /auth/negocio-activo` | Igual |
| Persona sin negocio | `POST /registro` | Igual (el bloque `negocio` es opcional) |

## Qué se hizo

- `POST /registro` acepta `negocio {nombre, giro}`. Valida el formato del negocio **antes** de tocar el código (un negocio mal pedido no gasta el código ni crea nada), crea la persona, anota el pedido (`SolicitudDeNegocio` ahora guarda nombre y giro) y publica `negocio.registro_solicitado`. La respuesta trae `negociosPendientes`.
- `GET /auth/yo` y las respuestas de registro, inicio de sesión y elegir negocio traen `negociosPendientes` (`id`, `nombre`, `giro`, `estado: PENDIENTE`).
- **Activación implícita (D-80):** sin negocio elegido, el único negocio con membresía activa es el activo; el `negocio_id` sale de las membresías del servidor, no del cliente. Con dos o más no se activa nada y todo queda negado; si llega un segundo negocio, la sesión pierde el activo implícito.
- Sin migraciones nuevas, sin eventos nuevos, sin cambios al contrato de `negocio.registrado`.
- Colección de Postman rehecha: 51 solicitudes en 6 carpetas. La carpeta 1 es el registro de persona y negocio en una llamada; la 3, el segundo negocio y elegir el activo; la 6, la persona sin negocio (genera sus propios datos).

## Resultado final

| Medida | Resultado | Comando |
|---|---|---|
| Suite completa | **1150 pruebas, 0 fallas, 0 omitidas** (1125 al empezar la etapa; +25) | `mvn -B verify` |
| Cobertura de Identidad | 99.3 % de líneas (1021/1028), 96.0 % de ramas (381/397) | JaCoCo, `target/site/jacoco/jacoco.csv` |
| Cobertura de Clientes y de `compartido` | 99.3 % / 96.6 % y 96.5 % / 93.6 % | ídem |
| Regla de cobertura (80 %) | cumplida | `mvn -B verify` |

## Ciclos RED → GREEN

| Ciclo | RED observado | GREEN observado |
|---|---|---|
| Alta única, pendientes y activación implícita | `mvn test-compile` falla por los símbolos nuevos (`SolicitudDeRegistro.NegocioPedido`, `negociosPendientes`, `deCuenta`, `SolicitudDeNegocio` con nombre y giro, …) | 1125 pruebas en verde al primer intento: la activación implícita no rompió ninguna prueba existente, salvo las que yo cambié a propósito (`yo_negocioCreadoDespues_apareceSinActivarse` pasó a `..._seActivaSolo`, y dos expectativas de `NegociosControllerTest`) |
| Defecto hallado al correr la colección | Se escribió `DevNegociosControllerTest` **antes** de arreglarlo y falló por la razón correcta: `Status expected:<200> but was:<404>` y la comprobación de que el simulador no use el escucha asincrónico | 1150 pruebas en verde |

**Defecto hallado por la corrida real (no por las pruebas de unidad):** el primer `GET /roles` después del arranque daba 404 en el paso 1.8 de la colección. Causa: `EscuchaDeNegocios` usa `@ApplicationModuleListener`, y **Spring Modulith activa `@Async` por sí solo** (`EventPublicationAutoConfiguration$AsyncEnablingConfiguration`), así que el escucha corre en otro hilo; el simulador de desarrollo lo llamaba y respondía 204 antes de aplicar el aviso. Repetido 60 veces, falló solo la primera. **Corregido:** el simulador aplica el aviso con `EquipoServicio` en el mismo hilo. **También corregí dos afirmaciones mías erróneas** (el Javadoc de `IdentidadConfiguracion` y la fila del reporte `identidad-idn2.tdd.md` decían que el proyecto no activa `@EnableAsync`; el revisor de código lo había dado por bueno). Con el bus real la entrega sí es asincrónica, y por eso la app debe tratar el negocio como pendiente hasta que aparezca: es justo lo que ahora muestra `negociosPendientes`.

## Mutaciones a mano

Se rompió una cosa a la vez, se corrió la prueba correspondiente y se restauró el archivo (comprobado con `diff`).

| Mutación | Pruebas que la detectaron |
|---|---|
| Sin activación implícita | `NegocioActivoTest` (3 pruebas) y otras |
| El registro ya no pide el negocio | `RegistrarCuentaTest.registrar_conNegocio_creaLaPersonaYPideElNegocio`, `RegistroControllerTest` |
| El negocio se valida después de gastar el código | `RegistrarCuentaTest.registrar_negocioInvalido_fallaSinConsumirElCodigo` (4 casos) |
| `pendientesDe` devuelve vacío | `NegociosPendientesTest` (4 pruebas), `RegistrarCuentaTest` |

## Verificación en una instancia real

Se arrancó el backend en frío (puerto 8081; el 8080 sigue ocupado por una instancia del equipo con código anterior, que no se tocó) y se corrieron las **51 solicitudes** de la colección con mi ejecutor en Python (estado HTTP y código de error de cada una, captura de token, cuenta y negocios): **51 de 51**. Con el entorno `identidad.local` vacío, las carpetas 1 y 2 no salen y avisan qué datos faltan; la 3 depende de las anteriores.

## No verificado / límites

- Los `pm.test` de la colección siguen sin ejecutarse en Postman o Newman (no hay Node).
- Los pedidos pendientes **no vencen** y no hay evento `negocio.registro_rechazado`: un negocio que el módulo de negocios nunca confirme se queda `PENDIENTE` para siempre. Es propuesta del plan, a acordar con quien haga ese módulo.
- Los pedidos pendientes viven en memoria: un reinicio entre el pedido y la respuesta los pierde. Con Postgres hará falta una tabla.
- `POST /registro` con negocio no usa el tope de 3 pedidos por cuenta de `POST /registro/negocio` (la cuenta es nueva); el tope por IP y el del código siguen vigentes.
- Con el bus real, el escucha asincrónico no se probó (el módulo de negocios no existe).
- No hay revisión de agentes de esta etapa; el cambio es pequeño, pero toca la autenticación (`identificar`), y vale la pena revisarlo.
