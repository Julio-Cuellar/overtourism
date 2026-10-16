# Evidencia TDD — IDN-4: una sola puerta de `/graphql` (fases M0 a M7)

**Fecha:** 2026-10-16. **Rama:** `julio`, sin commit ni push. **JDK:** 21.
**Autorización:** [BITACORA.md](../BITACORA.md), entrada «Autorización: unir las dos puertas de `/graphql` (opción 1)».
**Fuente del plan:** el plan presentado en la sesión (no hay archivo `*.plan.md`); las pruebas de cada fase salen de él.

## Por qué existió este trabajo

Al traer `origin/julio` (commit `230766e`, «modulo de negocios») llegó una segunda implementación de IDN-4 hecha por el equipo
(`FiltroDeEntrada`, `EntradaGraphQl`, `InterceptorDeEntrada`, `LimitesDeConsultaGraphQl`, `ResolvedorDeErroresComunes` y los
resolvedores de Negocios). Chocaba con las Fases 0 a 7 de este trabajo: sesión obligatoria en todo `/graphql` frente a una
superficie pública sin sesión, dos medidores de consulta, dos portadores de sesión, dos resolvedores de errores. Se resolvió con
la opción 1: se queda el filtro de entrada del remoto y encima se portan las capas de la puerta de la plataforma.

## Resultado de cada fase

| Fase | RED (evidencia) | GREEN (evidencia) |
|---|---|---|
| M0 línea base | Tras la mezcla: 2000 pruebas, 1 falla y 525 errores de contexto (`mvn clean verify`). Dos causas: bean `limitesDeConsulta` repetido y `autenticadorDeIdentidad` pidiendo `IdentidadServicio`, que ya no es un bean. | Con dos arreglos mínimos de nombre/tipo y el fixture `extend type Subscription`: la causa restante era solo «falta declarar Query.miNegocio…» (SG-21), es decir, trabajo de M2. |
| M1 un portador de sesión | De compilación: firmas nuevas de `ConfirmacionReforzada` y `EventosPorNegocio`. | 57 pruebas unitarias en verde. La puerta, el WebSocket, el tope de suscripciones, el aislamiento de eventos y el registro leen `EntradaGraphQl`. |
| M2 operaciones públicas | De compilación (`Acceso`, `publica`, `deCuenta`, `ComandoDeclarado.publico`). | 78 pruebas en verde, más 7 de `OperacionesDeNegociosTest` (Negocios declara sus 19 operaciones y su variante pública). |
| M3 filtro unificado | `SeguridadHttpTest`: 2 fallas por la razón esperada; `FiltroDeEntradaControlesTest`: de compilación. | 62 pruebas del filtro y la cadena en verde; suite completa: de 523 errores a 39 fallas + 15 errores. |
| M4 errores / M5 límites | 4 fallas en `ResolvedorDeErroresGraphqlTest` (el resolvedor del remoto tapaba al mío). | `TraductorDeErroresDeNegocios` (4 pruebas), 7 casos de límites portados (`LimitesDeConsultaDelRemotoTest`), 3 casos nuevos del clasificador. `GraphQlEntradaTest` (20 del remoto) en verde. |
| M6 WebSocket + `altaDeNegocio` | 2 errores en `AutenticacionWebSocketInterceptorTest` (la sesión sin negocio se rechazaba). | `AltaDeNegocioPorWebSocketTest` con servidor real: 5 pruebas. |
| M7 cierre | Dos revisiones independientes (`java-reviewer` y `security-reviewer`); 7 hallazgos corregidos con prueba primero (ver abajo). | `mvn -B clean verify` (JDK 21): **2069 pruebas, 0 fallas, cobertura cumplida**. 18 mutaciones, todas detectadas. |

La progresión de la suite completa fue: 2000 (1 + 525 rojas) → 2022 (54 rojas) → 2037 (26) → 2044 (17) → 2059 (0) → **2069 (0)** tras la revisión.

## Qué se retiró y dónde quedaron sus garantías

| Se retiró | Sus garantías pasaron a |
|---|---|
| `LimitesDeConsultaGraphQl` y `LimitesDeConsultaGraphQlTest` (7 casos) | `LimitesDeConsultaInstrumentation`; los mismos 7 casos corren en `LimitesDeConsultaDelRemotoTest` |
| `ResolvedorDeErroresComunes` | `ClasificadorDeErrores` (sin sesión, sin negocio, sin permiso, argumentos ilegibles) con pruebas en `ResolvedorDeErroresGraphqlTest` |
| `ResolvedorDeErroresDeNegocios` | `TraductorDeErroresDeNegocios` (SPI `TraductorDeErrores`), con `TraductorDeErroresDeNegociosTest` |
| `SesionEnContextoInterceptor` y su prueba (la sesión del hilo pasa al contexto) | `InterceptorDeEntrada` del remoto (`InterceptorDeEntradaTest`) y las pruebas de la puerta con `EntradaGraphQl` |
| `ClavesDeContexto.SESION` | `EntradaGraphQl.CLAVE` |
| Dos pruebas de `AutorizacionDeOperacionesInstrumentationTest` («manda el contexto sobre el hilo») | No aplican: ya no hay una segunda fuente en el hilo. Las reemplaza «sin ninguna entrada en el contexto se niega». |

## Pruebas existentes que cambiaron, y por qué

Ninguna se borró ni se debilitó para que pasara. Cambiaron porque el contrato cambió por una decisión aprobada:

| Prueba | Antes | Ahora | Motivo |
|---|---|---|---|
| `SeguridadHttpTest` (sin token) | 401 con `{"codigo":"NO_AUTENTICADO"}` | La cadena sola sigue dando 401 si la petición no pasó por el filtro; por el camino completo una consulta sin token llega a GraphQL | Hay superficie pública (turista) |
| `GraphqlConSesionDeIdentidadTest` | sin negocio = 403 HTTP; ausente/falso/revocado = el mismo 401 | sin negocio = 200 + `SIN_NEGOCIO_ACTIVO`; **falso y revocado siguen siendo el mismo 401** (AS-08); ausente = 200 + `NO_AUTENTICADO` | Igual |
| `TasaGraphqlConSesionTest` | sin token contaba como fallo de la IP | solo cuenta un token que no sirve; lo anónimo es válido y tiene su tope (120 por IP, 60 por dispositivo) | Decisión 2 |
| `HttpEstrictoGraphqlTest` | cuerpo `{"codigo":…}`; origen permitido = 401 | cuerpo `{"errors":[…]}` del filtro de entrada; origen permitido = 200 | El filtro del remoto responde primero |
| `EsquemaSinNegocioIdTest` | vacío | lista exacta de 4 apariciones documentadas (2 entradas públicas y 2 salidas del alta) | La superficie pública nombra el negocio consultado (D-98, D-99) |
| `AutorizacionDeOperacionesTest` (sin negocio) | `PERMISO_DENEGADO` | `SIN_NEGOCIO_ACTIVO` | Convención de Negocios |
| `TablaRealDeOperacionesTest` | solo Clientes y plataforma | cada módulo contra lo suyo, más listas exactas de operaciones públicas y de cuenta | Hay más módulos |
| `GraphQlEntradaTest` (del remoto) | tope de cuerpo con 40 000 bytes → 413; 12 alias → `CONSULTA_DEMASIADO_GRANDE` | 70 000 bytes → 413 (el tope aprobado es 64 KB); 12 campos con alias → `DEMASIADOS_CAMPOS_RAIZ` y se añadió el caso de 12 alias anidados → `DEMASIADOS_ALIAS` | Decisiones 1 y 3 |
| `WebSocketGraphqlTest.initInvalido` y `AutenticacionWebSocketInterceptorTest` | una sesión sin negocio se rechazaba con 4401 | se acepta | `altaDeNegocio` existe para quien aún no tiene negocio |
| Código `DEMASIADAS_PETICIONES` | en la cadena de Spring Security y en el WebSocket | `DEMASIADAS_SOLICITUDES` en todas partes | Un solo código para lo mismo |

## Mutaciones (18, todas detectadas; archivos restaurados idénticos)

| # | Mutación | Pruebas que la detectaron |
|---|---|---|
| 1 | Un token malo se degrada a anónimo | 6 fallas en `FiltroDeEntradaTest`, `FiltroDeEntradaControlesTest`, `GraphqlConSesionDeIdentidadTest` |
| 2 | Un campo que la tabla no conoce queda abierto | 2 en `AutorizacionDeOperacionesInstrumentationTest` |
| 3 | Toda variante de comando es pública | 2 en la misma |
| 4 | Una operación de cuenta pasa sin sesión | 1 |
| 5 | La cadena de Spring Security deja pasar `/graphql` sin el filtro de entrada | 1 en `SeguridadHttpTest` |
| 6 | Un token falso no gasta fichas de la IP | 2 en `FiltroDeEntradaControlesTest` |
| 7 | El registro de auditoría pierde la cuenta | 2 en `RegistroDeOperacionesTest` |
| 8 | Los eventos de otro negocio pasan | 5 en `EventosPorNegocioTest` |
| 9 | El permiso sensible no pide confirmación | 9 |
| 10 | Una consulta del panel se vuelve pública | 3 en `OperacionesDeNegociosTest` y `TablaRealDeOperacionesTest` |
| 11 | La tasa por cuenta no se aplica | 1 |
| 12 | El apretón de manos del WebSocket se trata como consulta | 1 |
| 13 | El barrido del limitador usa la ventana de quien lo dispara | 2 en `LimiteDeTasaEnMemoriaTest` |
| 14 | Una sesión revocada por WebSocket no gasta fichas de la IP | 1 |
| 15 | El interceptor de HTTP vuelve a tocar el WebSocket | 2 en `InterceptorDeEntradaWebSocketTest` |
| 16 | Un token de 257 caracteres se consulta | 1 |
| 17 | Una cadena vacía no libera el lugar de la suscripción | 1 |
| 18 | La consulta inválida no deja rastro | 1 |

## Revisión independiente (M7)

Los dos revisores solo leyeron. No encontraron ningún camino para ejecutar una operación del panel sin sesión, sin negocio o sin
permiso, ni para falsificar la `EntradaGraphQl`. Lo que encontraron, y qué se hizo:

| Hallazgo | Gravedad | Qué se hizo |
|---|---|---|
| `LimiteDeTasaEnMemoria`: el barrido borraba los intentos de TODAS las claves con la ventana de quien lo disparaba, así que el tráfico de `/graphql` (1 minuto) liberaba los topes de solicitud de código de acceso (hasta 24 h). | ALTA | **Corregido de raíz**: cada clave guarda su ventana. 2 pruebas nuevas; la primera falló antes del arreglo con «la clave bloqueada quedó libre». |
| WebSocket: cada mensaje con sesión revocada consultaba a Identidad sin tope. | MEDIA | **Corregido**: gasta fichas de fallos de la IP y, agotadas, ni se consulta. 2 pruebas. |
| `InterceptorDeEntrada` podía pisar con una anónima la entrada autenticada del WebSocket (hoy funcionaba por un orden implícito de Spring). | MEDIA | **Corregido**: ignora las peticiones de WebSocket. 3 pruebas; 2 fallaron antes. |
| `ConversionException` se respondía como culpa del cliente sin rastro. | MEDIA | **Corregido**: deja una línea WARN con campo y clase, nunca el valor. |
| Token sin tope de longitud en HTTP. | BAJA | **Corregido** (256, igual que WebSocket). |
| El lugar de una suscripción no se liberaba si la cadena terminaba vacía. | BAJA | **Corregido**. |
| Javadocs y comentarios que ya no decían la verdad (orden de interceptores, filtros de tasa, «sin validar»). | BAJA | **Corregidos**. |

**Registrados y no corregidos** (necesitan decisión o son de despliegue):
1. Detrás de un proxy todos los clientes compartirían IP (se usa la de la conexión): hay que configurar `forward-headers-strategy` y los proxies de confianza al desplegar.
2. Las suscripciones por SSE en `/graphql` no pasan por el tope de suscripciones (hoy solo existe `altaDeNegocio`, una por cuenta). Decidir si se permite SSE o se aplica el tope.
3. No hay tope de conexiones WebSocket simultáneas por cuenta (sí de apretones de manos por IP).
4. `X-Dispositivo-Id` se puede rotar y evita el tope de reseñas: solo importa si las reseñas salen del modo demostración.
5. `/demo/**` es público y se protege solo por perfil (`demo & !produccion`); `/admin/**` queda abierto a nivel de cadena y su token no tiene longitud mínima.
6. `confirmarComando` es pública a nivel de puerta: cuando Clientes o Inventario tengan resolvedores hace falta una prueba de arquitectura que obligue a exigir sesión en cada manejador con permiso.
7. Los 401 y 429 del filtro de entrada no dejan registro, el límite no agrupa IPv6 por /64, el `Origin` se verifica después de autenticar y `Retry-After` vale siempre 60.

## Lo que se encontró de verdad (no estaba en el plan)

- **Regresión de auditoría (SG-20):** al quitar la sesión del hilo, el registro por operación quedó con `cuenta=- negocio=-`.
  La detectaron `RegistroDeOperacionesTest`, `ErroresGraphqlConSesionTest` y `WebSocketGraphqlTest`. Corregido: el registro lee
  `EntradaGraphQl`, con pruebas de anónimo y de sesión sin negocio.
- **El WebSocket rechazaba justo a quien lo necesita:** `altaDeNegocio` es para quien acaba de registrarse y no tiene negocio, pero
  `connection_init` exigía negocio activo. Corregido y probado con servidor real.
- **Códigos de error duplicados** para la misma condición (`DEMASIADAS_PETICIONES` y `DEMASIADAS_SOLICITUDES`). Unificados.

## Lo que no se hizo igual que en el plan

- **Decisión 5 (un solo mecanismo de tasa):** no se unificó. El puerto `LimiteDeTasa` de Identidad no tiene «consultar sin
  consumir», y los fallos por IP necesitan consultar antes de autenticar. El filtro usa el puerto para lo general (IP y
  dispositivo) y `LimitadorDeTasa` para fallos y cuentas.
- **Cinco filtros míos:** no se retiraron todos. `BearerTokenFilter` ya no actúa sobre `/graphql`, pero `PeticionGraphqlEstrictaFilter`
  (origen en POST y en la subida) y `LimiteDeConexionesWebSocketFilter` siguen en la cadena porque hacen algo que el filtro del
  remoto no hace; sus propias pruebas siguen en verde.
- **`EventosPorCuenta`:** no se creó. `AvisosDeAlta` ya aísla por cuenta de forma estructural y está probado con servidor real.

## Límites conocidos

Los de las Fases 0 a 7 siguen (suscripciones abiertas sobreviven a una revocación; `EventosPorNegocio` no se aplica solo; tasa en
memoria de una sola instancia; CORS no configurado) y SG-11 y SG-18 siguen pendientes.

## Estado de las reglas SG-01 a SG-21 al cierre

| Regla | Estado | Dónde |
|---|---|---|
| SG-01 Autenticación | Cumplida | `FiltroDeEntrada` (token malo = 401, nunca anónimo) y puerta por operación; `/graphql` falla cerrado sin la entrada del filtro. Las únicas operaciones sin sesión están listadas y fijadas por prueba. |
| SG-02 `negocio_id` solo del contexto | Cumplida | `EntradaGraphQl`; `negocioId` solo aparece donde `EsquemaSinNegocioIdTest` lo documenta |
| SG-03 Autorización en cada resolvedor | Cumplida | `AutorizacionDeOperacionesInstrumentation` |
| SG-04 Autorización por función | Cumplida | Permiso por operación y por variante de comando |
| SG-05 Esquema mínimo | Parcial | Falta revisar tipo por tipo cuando los módulos tengan resolvedores |
| SG-06 Profundidad y costo | Cumplida | `LimitesDeConsultaInstrumentation` (profundidad 7, costo 100) |
| SG-07 Paginación y DataLoader | Parcial | Depende de los módulos; Negocios ya acota páginas |
| SG-08 Alias y lotes | Cumplida | Tope de alias y de campos de la raíz; sin lotes |
| SG-09 Tiempos y tamaños | Cumplida | 10 s, 10 000 caracteres de consulta, 64 KB de cuerpo y de mensaje de WebSocket |
| SG-10 Tasa | Cumplida (una instancia) | IP, dispositivo, fallos por IP y cuenta; en memoria |
| SG-11 Consultas permitidas por hash | **Pendiente** | No necesaria para la demo; solo producción |
| SG-12 Esquema cerrado | Cumplida | Introspección, GraphiQL y esquema apagados; documentación solo en desarrollo |
| SG-13 Errores enmascarados | Cumplida | Lista blanca; traductores por módulo (Clientes, Inventario, Negocios) |
| SG-14 HTTP estricto | Cumplida | Solo POST JSON de tamaño conocido; origen verificado |
| SG-15 WebSocket seguro | Cumplida con límites | Origen, `connection_init`, token revalidado por operación; sin tope de conexiones simultáneas |
| SG-16 Aislamiento de eventos | Cumplida con límite | `EventosPorNegocio`; `altaDeNegocio` aislada por cuenta; falta la prueba arquitectónica |
| SG-17 Validación por lista permitida | Parcial | La de Negocios y Clientes está en sus módulos |
| SG-18 Sin dependencias flojas | **Pendiente** | Enforcer, perfil de escaneo y Dependabot |
| SG-19 Operaciones sensibles | Cumplida | Negadas por omisión hasta IDN-3.2 |
| SG-20 Registro y auditoría | Cumplida | Una línea por operación con cuenta y negocio; nunca el texto de la consulta |
| SG-21 Inventario de operaciones | Cumplida | Tabla verificada contra el esquema al arrancar, con las de Clientes, Negocios y la plataforma |
