# Registro en dos pasos con pre-registro y código al correo (D-84, D-85, D-86): evidencia TDD

**Fecha:** 2026-10-15 · **Rama:** `julio` (sin commits) · **Autorización:** entrada «Autorización: registro en dos pasos con pre-registro y código al correo» de `docs/BITACORA.md`.

**Plan de origen:** el que presenté en el chat tras leer `clinerya-users` del repositorio Julio-Cuellar/Clinerya (no hay archivo `*.plan.md`). Las historias salen de lo que pidió el usuario: «hace el formulario de registro, se envía un código de confirmación a su correo, este código por el momento pasa como un log en la consola y lo colocamos en otro endpoint que sea el de verificación, los códigos de verificación se guardan en base de datos durante 15 min y si no se ingresa este se borra».

## Historias de usuario
1. Como dueño de un negocio, quiero llenar el formulario de registro (con mi negocio) sin tener que pedir ni escribir un código antes, para registrarme más rápido.
2. Como dueño, quiero recibir un código en mi correo (hoy, en la consola) y verificarlo en otro endpoint, para que mi cuenta nazca con el correo comprobado y con mi sesión abierta.
3. Como dueño que se equivocó o no recibió el código, quiero pedir otro, para no tener que llenar todo de nuevo.
4. Como plataforma, quiero que el formulario y su código vivan 15 minutos en la base y se borren si no se verifican, para no guardar datos personales de más.
5. Como persona con cuenta, quiero pedir mi código con el celular, el correo o el id y recibirlo siempre en mi correo, sin que nadie pueda saber por ahí si existe una cuenta.

## Plan → pruebas → RED → GREEN
| # | Tarea del plan | Pruebas que la guardan | RED | GREEN |
|---|---|---|---|---|
| 1 | `Preregistro` (dominio): vigencia, intentos, hash, código nuevo | `PreregistroTest` (9) | no compila: falta la clase | 9 de 9 |
| 2 | Puerto y almacén en memoria | `RepositorioPreregistrosEnMemoriaTest` (4) | no compila | 4 de 4 |
| 3 | Formulario, verificación y reenvío en el servicio | `RegistroEnDosPasosTest` (71) | no compila: faltan `verificarRegistro`, `reenviarCodigoDeRegistro` y el nuevo `SolicitudDeRegistro` | 71 de 71 |
| 4 | Todos los códigos al correo (D-85) | `SolicitarCodigoTest` (18), `AccesoPorIdentificadorTest` (16), `IniciarSesionTest` (30), `EndurecimientoTest` (15), `CanalSimuladoTest` (4), `CorreoTest` (24) | `Envio.destino()` y `CodigoAcceso.destino()` no existían | todas en verde |
| 5 | Tabla `identidad.preregistro` y `codigo_acceso.destino` (V023) | `EsquemaPreregistroPostgresTest` (8), `EsquemaIdentidadPostgresTest` (11) | | verde con Postgres real |
| 6 | Repositorio JDBC | `RepositorioPreregistrosJdbcPostgresTest` (6) | ver «Honestidad sobre el orden» | verde |
| 7 | Limpieza de vencidos cada minuto | `LimpiezaDePreregistrosTest` (4), `LimpiezaProgramadaTest` (2) | ver «Honestidad sobre el orden» | verde |
| 8 | REST: `/registro` 202, `/registro/verificacion` 201, `/registro/reenvio` 202, `/dev/canal/codigo?correo=` | `RegistroControllerTest` (19), `AuthControllerTest` (22), `NegociosControllerTest` (10), `DevNegociosControllerTest` (2), `RegistrosSinSecretosTest` (2), `ErroresYRespuestasTest` (28) | | verde |
| 9 | Colección de Postman | corrida real, abajo | | 62 de 62 |

**RED (compilación de pruebas, antes de tocar producción):** `mvn -B -q test-compile` dio 200 líneas de error, todas por lo que falta: `verificarRegistro` (30), `reenviarCodigoDeRegistro` (4), `RepositorioPreregistrosEnMemoria` (4), `Preregistro` (4), el constructor de 5 argumentos de `SolicitudDeRegistro` y `telefono()` en el canal grabador. Ninguna por una causa ajena.

**GREEN, primer corte:** al terminar el código, las 505 pruebas sin Spring dieron verde salvo 3 de `PerfilProduccionTest`, que armaban un contexto sin base y ahora necesitan un `JdbcClient` de mentira (el repositorio de pre-registros vive en Postgres). Después, 289 pruebas de Spring, Postgres y web en verde a la primera.

## Honestidad sobre el orden
El repositorio JDBC, la migración V023 y la limpieza programada **se escribieron antes que sus pruebas**, contra el orden que pide TDD. Para compensarlo, las pruebas se validaron con mutaciones (abajo): M6, M7, M10, M12 y M17 las apuntan y las cinco fallaron. Aun así, la prueba no nació en rojo para esas tres piezas.

## Mutaciones a mano (cada una se aplicó, se corrió la suite y se revirtió)
| # | Mutación | Resultado |
|---|---|---|
| M1 | el pre-registro vence solo después de la hora (`isAfter`) | detectada |
| M2 | verificar sin mirar el tope de intentos | detectada |
| M3 | verificar sin revisar que el correo siga libre | detectada |
| M4 | reenvío sin tope por correo | detectada |
| M5 | registrar guarda antes de enviar | detectada |
| M6 | JDBC: vencidos con `<` en vez de `<=` | detectada |
| M7 | JDBC: el reemplazo no actualiza el hash del código | detectada |
| M8 | la cuenta nace con el correo sin verificar | detectada |
| M9 | pedir el código con celular no manda nada | detectada (78 pruebas) |
| M10 | la limpieza deja pasar la excepción | detectada |
| M12 | la migración sin `UNIQUE` en el correo | detectada |
| M13 | formulario sin tope por IP | detectada |
| M14 | verificación sin tope por IP | detectada |
| M15 | verificar no borra el pre-registro vencido | detectada |
| M16 | el aviso se acepta al verificar y no al llenar el formulario | detectada |
| M17 | JDBC: `eliminar` siempre responde que sí | detectada |
| M18 | verificar ignora quién consumió el pre-registro | detectada |
| M19 | iniciar sesión sin haber pedido código deja pasar | detectada |

M11 (ocho verificaciones simultáneas) no se mutó: con el servicio `synchronized` la prueba pasaría igual; la protección real con Postgres es el `DELETE` condicional, que prueba M17.

## Corrida real (no solo pruebas)
- **Colección de Postman:** 62 solicitudes en 6 carpetas; con mi ejecutor en Python, **62 de 62** con los topes por omisión (una corrida usa unos 13 de los 20 pedidos por IP) y **62 de 62 dos veces seguidas** sobre el mismo backend con el tope de IP subido. No ejecuta los `pm.test`: comprueba el estado HTTP, el código de error y captura las variables.
- **Consola:** cada código sale como `[CANAL SIMULADO] Código de verificación para a***@example.com: 196483`, con el correo enmascarado.
- **Base de datos real (dev):** un `POST /registro` dejó una fila en `identidad.preregistro` con vigencia de 15 minutos y un hash de 64 caracteres. Se la envejeció a mano a 5 minutos vencida y **la limpieza la borró sola en unos 45 segundos**.
- **Migración:** V023 se aplicó a la base de desarrollo `negocio360` al arrancar mi instancia de prueba.

## Cobertura y huecos
- `mvn -B verify`: **1222 pruebas, 0 fallas**, «All coverage checks have been met». Identidad: **99.4 % de líneas y 96.2 % de ramas**.
- Las clases nuevas (`Preregistro`, `RepositorioPreregistrosEnMemoria`, `RepositorioPreregistrosJdbc`, `LimpiezaDePreregistros`, `PreregistroCreado`) tienen 100 % de líneas y ramas.
- **Sin cubrir a propósito:** la rama de `cuentaParaIniciar` por cuenta que desapareció entre dos pasos (inalcanzable con el bloqueo del servicio) y el dispositivo vacío (de antes).
- **No verificado:** los `pm.test` de la colección (no hay Node ni Newman); el comportamiento con dos instancias de la aplicación (cada una corre su limpieza; es inofensivo, pero no se probó); esta etapa **no tuvo revisión de agentes**.
- **Reemplaza** a `identidad-registro-unico.tdd.md` en lo que toca al alta de la persona (aquel flujo de una llamada con el código por adelantado ya no existe); lo demás de ese documento (negocio pendiente, activación implícita) sigue vigente.
