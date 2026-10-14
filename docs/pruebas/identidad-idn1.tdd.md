# Evidencia de TDD: Identidad, épica IDN-1 (cuenta, código y sesión)

Fecha: 2026-10-14. Rama `julio`, sin commits. Autorización: entrada «2026-10-14 — Autorización para programar el módulo de Identidad» de `docs/BITACORA.md`.
Plan de origen: `Mapa de Identidad v0.1` (reglas AC, AS, AM, AI, AZ, AP y seguridad SI-01 a SI-20). No se usó un archivo `*.plan.md`.

**Alcance de esta evidencia:** código en memoria, sin base de datos. Los repositorios, el limitador y el canal de códigos son adaptadores para desarrollo y demo. Lo que sigue sin existir está en «Huecos conocidos».

## Resultado final

| Medida | Resultado | Comando |
|---|---|---|
| Suite completa | 705 pruebas, 0 fallas, 0 omitidas (410 eran la línea base de Clientes; 295 son de Identidad y de `compartido`) | `mvn -B verify` |
| Cobertura de Identidad | 99.6 % de líneas (446/448), 95.7 % de ramas (177/185) | JaCoCo, `target/site/jacoco/jacoco.csv` |
| Cobertura de `compartido.seguridad` | 93.9 % de líneas, 100 % de ramas | ídem |
| Cobertura de todo el backend | 98.7 % de líneas, 95.6 % de ramas | ídem |
| Regla de cobertura (80 %) | cumplida: «All coverage checks have been met» | `mvn -B verify` |
| Estructura de módulos | `identidad` solo depende de `compartido`; ningún módulo importa `identidad` | `EstructuraModulosTest` |

## Ciclos RED → GREEN

| Ciclo | RED observado | GREEN observado |
|---|---|---|
| 1. Utilidades y dominio (`TokenOpaco`, `HashConSal`, `Telefono`, `NombreVisible`, `Sesion`, `CodigoAcceso`) | `test-compile` falla por símbolos inexistentes | 85 pruebas pasan |
| 2. Aplicación (`IdentidadServicio`, política, plantillas, limitador, repositorios en memoria) | `test-compile` falla por clases e interfaces inexistentes | 191 pruebas acumuladas de identidad y seguridad |
| 3. Capa REST (`AuthController`, errores, canal simulado, configuración por perfil) | `test-compile` falla solo por `DevCanalController` y las clases de la etapa | 217 pruebas; luego 1 corrección propia (el controlador de desarrollo no se registraba sin escaneo) |
| 4. Endurecimiento tras las revisiones de código y de seguridad | `test-compile` falla solo por `DireccionIp`; las pruebas de comportamiento nuevas se escribieron antes del código | 704, luego 705 con la prueba que cerró un hueco hallado por mutación |

Nota honesta sobre el RED: en todos los ciclos fue **de compilación** (la prueba referencia una clase que aún no existe). No se ejecutó cada prueba nueva contra un código anterior para verlas fallar una por una. Para suplir eso se hicieron mutaciones puntuales (siguiente sección).

## Pruebas de mutación hechas a mano

Se rompió una regla a la vez, se corrió la prueba correspondiente y se restauró el archivo (se comprobó con `diff`).

| Mutación | Prueba que la detectó |
|---|---|
| El código usado vuelve a servir (se quita la comprobación de `usadoEn`) | `IniciarSesionTest`: 2 fallas |
| Un token revocado sigue autenticando (`vigente()` ignora la revocación) | `SesionesTest` y `SesionTest`: 4 fallas |
| `RespuestaSesion` imprime el token en `toString` | `RegistrosSinSecretosTest`: 1 falla |
| Se quita `Locale.ROOT` del código de 6 dígitos | `EndurecimientoTest`: 1 falla |
| Se guarda el código antes de enviarlo (orden viejo) | `EndurecimientoTest`: 3 fallas |
| El nombre ya no tiene tope de largo previo | `NombreVisibleInvisiblesTest`: 1 falla |
| El controlador de desarrollo vuelve a depender solo del perfil | **no fue detectada** con las pruebas existentes; se agregó `controlador_sinPropiedad_noExiste`, que ahora falla con la mutación y pasa con el código bueno |

## Revisiones

Se corrieron `ecc:java-reviewer` y `ecc:security-reviewer` (solo lectura) sobre el módulo. Ninguna encontró hallazgos críticos. Resumen: la de código, 2 altos, 5 medios, 9 bajos; la de seguridad, 2 altos, 7 medios, 9 bajos.

**Corregido en el ciclo 4** (con prueba primero):

| Hallazgo | Qué se hizo |
|---|---|
| El endpoint `/dev/canal/codigo` quedaba abierto si se olvidaba el perfil `produccion` | Ahora existe solo con `identidad.canal-simulado.habilitado=true` (el perfil `desarrollo` lo pone) y nunca con `produccion`. Sin eso la aplicación **no arranca**: se verificó arrancándola |
| Si el canal fallaba al reenviar, se inutilizaba el código anterior ya entregado | Se envía antes de guardar; si el canal falla no se guarda nada |
| Solo se capturaba `CanalNoDisponibleException` | Cualquier fallo del canal es `ERROR_TEMPORAL`, sin copiar su mensaje |
| `String.format("%06d")` dependía del idioma del servidor | `Locale.ROOT` |
| El dispositivo podía partir un emoji y dejaba caracteres de formato | Se corta por caracteres completos y se limpian los de formato |
| IP: octetos imposibles, IPv6 comprimida, IPv4 mapeada, /64 sin agrupar | Clase `DireccionIp`: solo IP literales (sin DNS), IPv4 mapeada = IPv4, IPv6 agrupada por /64 para el tope |
| La política no tenía cotas máximas | Cotas en `PoliticaDeAcceso` (intentos ≤ 10, vigencia del código ≤ 15 min, etc.) |
| `toString` sin proteger en `NegocioVista`, `PerfilVista`, `RespuestaNegocio` | Protegidos |
| Sin límite de largo del texto en JSON | Jackson limita cada cadena a 10 000 caracteres (`compartido.web.LimitesDeJson`) y `NombreVisible` descarta entradas de más de 400 antes de las expresiones regulares |
| Nombres invisibles, con separadores de línea o sin letra | Rechazados |
| Sin `Cache-Control: no-store` ni `X-Content-Type-Options` | Filtro `CabecerasDeSeguridad` en `/auth/*`, `/cuenta` y `/dev/*` |
| `GET /dev/canal/codigo` sin teléfono daba 500 | Da 400 `SOLICITUD_INVALIDA` |
| Tres pruebas con falso verde (`SesionesTest`, `SolicitarCodigoTest`, `PerfilProduccionTest`) | Corregidas; ahora la de perfil verifica la causa del fallo de arranque |
| Javadoc engañoso de `TokenOpaco` y torpe de `LimiteDeTasaEnMemoria` | Reescritos |

**No se corrigió, por ser decisión de diseño o deuda de otra historia** (el equipo debe decidir):

| Hallazgo | Por qué queda abierto |
|---|---|
| Cualquiera que conozca un teléfono puede gastar los intentos de su código o su cupo de solicitudes y bloquear el acceso de esa persona (A-02 y H3) | La solución propuesta (un `desafioId` devuelto por `/auth/codigo` y exigido en `/auth/sesion`) **cambia el contrato público**: la respuesta deja de ser un 202 vacío |
| Los errores `CODIGO_EXPIRADO` y `CODIGO_AGOTADO`, distintos de `CODIGO_INVALIDO`, revelan el estado del código de un teléfono ajeno (M-01 y H11) | Colapsarlos pierde información útil para quien sí es dueño del código; depende del `desafioId` anterior |
| Sin tope diario por teléfono ni presupuesto global de mensajes (H2, H4) | Hay que decidir los valores y el costo del canal real |
| Hash del código y claves del limitador con SHA-256 sin secreto del servidor (H12, M-04) | Pide un secreto de aplicación (HMAC) y definir dónde se guarda |
| El puerto de repositorios solo permite reemplazar la fila completa, lo que perdería actualizaciones al quitar `synchronized` (M-03, H8) | Conviene rediseñar los puertos al implementar Postgres (IDN-1.1), con pruebas de concurrencia reales |
| Llamada al canal dentro del bloqueo global y oráculo de existencia por el canal real (H7, H9) | Dependen del canal real (IDN-DOC.1) |
| Repositorios en memoria sin purga (H10) | Solo desarrollo y demo |
| `Sesion.conNegocio` conserva el hash del token (H14) | Se resuelve en IDN-2; el cambio de negocio debe emitir token nuevo |
| `IdentidadServicio` es público en la raíz del módulo (B-07) y hay un ciclo interno entre `web` e `infraestructura` | Mejora de estructura, sin riesgo hoy |

No se volvió a correr una revisión de agentes sobre los cambios del ciclo 4; los cubren las pruebas y las mutaciones de arriba.

## Especificación de pruebas (garantías por tema)

| # | Qué se garantiza | Dónde se prueba | Tipo |
|---|---|---|---|
| 1 | El token tiene 256 bits en base64url (43 caracteres), el hash es SHA-256 y la comparación es de tiempo constante | `TokenOpacoTest` | unidad |
| 2 | El hash con sal verifica solo el valor correcto y no se imprime | `HashConSalTest` | unidad |
| 3 | El teléfono se normaliza a E.164 +52 desde varios formatos y rechaza lo demás | `TelefonoTest` | unidad |
| 4 | El nombre mide de 2 a 80 caracteres, colapsa espacios, rechaza control, formato, separadores, rellenos invisibles y textos sin letra | `NombreVisibleTest`, `NombreVisibleInvisiblesTest` | unidad |
| 5 | La IP se guarda truncada; la IPv4 mapeada y los /64 comparten tope; un nombre de host no dispara el DNS | `DireccionIpTest` | unidad |
| 6 | La sesión es inmutable, se renueva sin pasar del tope absoluto y se revoca una sola vez | `SesionTest`, `SesionesTest` | unidad |
| 7 | El código dura 5 minutos, admite 5 intentos, es de un solo uso y se compara en tiempo constante | `CodigoAccesoTest`, `IniciarSesionTest` | unidad |
| 8 | Tres solicitudes por teléfono y 20 por IP cada 15 minutos; el 202 no depende de que exista la cuenta | `SolicitarCodigoTest`, `LimiteDeTasaEnMemoriaTest`, `AuthControllerTest` | unidad e integración |
| 9 | Una cuenta nueva sin nombre recibe 422 y su código no se gasta; el mismo teléfono en dos formatos es la misma cuenta | `IniciarSesionTest`, `AuthControllerTest` | unidad e integración |
| 10 | Un código usado nunca vuelve a servir, ni con 8 intentos simultáneos | `IniciarSesionTest` (concurrencia) | unidad |
| 11 | Máximo 5 sesiones por cuenta (la sexta revoca la más antigua) y también con 1 | `IniciarSesionTest`, `EndurecimientoTest` | unidad |
| 12 | Una cuenta suspendida o anonimizada no autentica en ninguna operación | `EndurecimientoTest` | unidad |
| 13 | Los 401 de token inexistente, ausente, con otro esquema o revocado son idénticos y llevan `WWW-Authenticate: Bearer` | `AuthControllerTest` | integración |
| 14 | Un `negocioId` en el cuerpo se ignora | `AuthControllerTest` | integración |
| 15 | Ningún error muestra trazas, clases ni lo recibido; ningún `toString` revela token, nombre, teléfono, código ni negocio | `AuthControllerTest`, `ErroresYRespuestasTest` | integración y unidad |
| 16 | Ni códigos, ni tokens, ni teléfonos, ni nombres aparecen en los registros, incluso con DEBUG en Spring MVC | `RegistrosSinSecretosTest` | integración |
| 17 | Sin el perfil ni la propiedad no existe el canal simulado ni el endpoint; con `produccion` tampoco; la aplicación no arranca sin canal real | `PerfilProduccionTest` | contexto |
| 18 | Un texto de 11 000 caracteres se rechaza con 400 sin repetirlo | `AuthControllerTest` | integración |
| 19 | Cabeceras `Cache-Control: no-store` y `X-Content-Type-Options: nosniff` en las rutas de acceso, incluso en errores | `AuthControllerTest` | integración |
| 20 | Si el canal falla, no se guarda código nuevo y el anterior sigue sirviendo | `EndurecimientoTest` | unidad |
| 21 | La política se lee de `identidad.politica.*`, tiene cotas máximas y una inválida impide el arranque | `PerfilProduccionTest`, `PlantillasYPoliticaTest`, `EndurecimientoTest` | unidad y contexto |

## Colección de Postman

Archivos: `backend/postman/identidad.postman_collection.json` (20 solicitudes en 3 carpetas, cada una con `pm.test`) y `identidad.local.postman_environment.json`.

**Lo que se verificó:** el JSON es válido (Node) y los mismos flujos se recorrieron con curl y con `fetch` de Node contra la aplicación en marcha (perfil `desarrollo`): 202, 422, 200 con token, perfil, nombre normalizado «Ana María Prueba», cierre 204, token cerrado 401, tope 429 en la cuarta, 415 con formulario, 400 con JSON roto, 400 con texto enorme, cierre de todas las sesiones y cabeceras `no-store`.
**Lo que NO se verificó:** los scripts de Postman (`pm.test`, `pm.sendRequest`) **no se ejecutaron** en Postman ni en Newman (no está instalado). Pueden tener errores de scripts que curl no detecta.

Para correrla: arrancar con `mvn spring-boot:run "-Dspring-boot.run.profiles=desarrollo"`; para repetirla varias veces seguidas, `"-Dspring-boot.run.arguments=--identidad.politica.solicitudes-por-ip=1000"`.

## Huecos conocidos

- **Sin base de datos:** las migraciones (IDN-1.1) y la seguridad por fila no existen. Los datos viven en memoria y se pierden al reiniciar. No hay Docker en esta máquina, así que Postgres con Testcontainers no se pudo probar.
- **Sin membresías, roles ni permisos:** `/auth/yo` devuelve `negocios: []` y `negocioActivo: null`. Llegan con IDN-2 en adelante.
- **Sin filtro de autenticación general:** nada llama aún a `autenticar` en `/graphql` ni en las demás rutas. Eso es IDN-4. Hasta entonces las consultas de Clientes siguen anónimas.
- **Canal real de códigos:** no está decidido (IDN-DOC.1). Solo existe el simulado.
- **Duplicación a resolver con HTC-89:** `Telefono` está copiada a propósito de Clientes y las utilidades de token y hash conviven con las privadas de `ServicioPreparacion`.
- **Sin prueba de carga** ni de rendimiento de las estructuras en memoria.
- **Cobertura de ramas:** 8 ramas de Identidad sin cubrir (caminos defensivos).
