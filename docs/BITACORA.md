# Bitácora del proyecto — Negocio 360 / Puebla Turística 360°

Este archivo existe para que **cualquier agente o persona que entre al proyecto sepa en qué punto estamos**, qué se decidió, por qué, y qué sigue abierto. Léelo completo antes de actuar.

## Cómo usar y mantener esta bitácora

1. **Lee primero** las secciones 1 a 4. Después revisa las entradas del registro (sección 7) de la más reciente hacia atrás.
2. **No reescribas entradas pasadas.** Agrega entradas nuevas al final del registro. Si una decisión cambia, agrega una entrada nueva y marca la anterior como `REEMPLAZADA por D-xx` en la tabla de decisiones.
3. **Formato de una entrada:** fecha `AAAA-MM-DD`, quién (persona o agente), qué se vio, qué se decidió, qué quedó pendiente, qué archivos se tocaron.
4. **Estado de una decisión:** `ACEPTADA` (el equipo la dijo expresamente), `PROPUESTA` (vigente salvo objeción), `ABIERTA` (sin respuesta), `REEMPLAZADA`.
5. **No presentes como decidido lo que no lo está.** Si el usuario no respondió una pregunta, la decisión queda `PROPUESTA` o `ABIERTA`.

## Documentos relacionados

| Documento | Para qué sirve |
|---|---|
| `.agent/rules/negocio360.md` | Regla permanente para Antigravity (`trigger: always_on`): se inyecta en cada turno. Resume lo no negociable (compuerta de código, principio rector, R1 a R6, seguridad, decisiones, datos, acciones hacia afuera) y manda a leer la Bitácora y la skill `negocio360-reglas`. Debe mantenerse corta y sincronizada con esa skill. |
| `.gitignore` | Lo que no se sube a GitHub: secretos, compilados de Java, archivos del IDE, volcados de bases de datos y temporales. No ignora `.agent/`, `docs/`, `CLAUDE.md` ni `README.md`. |
| `README.md` | Portada del repositorio: qué es el proyecto, estructura, por dónde empezar y **cómo usar las skills con Gemini en Antigravity** (preparación, ejemplos de mensajes, cómo comprobar que funcionan, qué hacer si no se activan y reglas del equipo). |
| `CLAUDE.md` | Especificación original del proyecto ("Negocio 360"). Actualizada el 2026-10-08 (ver sección 3): conserva las reglas de la convocatoria, de seguridad y el principio rector, y rotula como «original» el diseño previo al cambio de rumbo. |
| `docs/diseno/inventario.md` | Diseño del módulo de Inventario con reglas, datos, contratos de API y de eventos. Borrador v0.1. **No incorpora todavía D-24 a D-30**, que están en el documento de diagramas. |
| `docs/diseno/inventario-diagramas.md` | Ocho diagramas de secuencia, diagramas de eventos, lista de interacciones síncronas y asíncronas con su función, modelo de datos con todas las columnas e índices, y los hallazgos D-24 a D-30. Borrador v0.1. |
| `.agent/skills/negocio360-reglas/SKILL.md` | Skill para agentes de Google Gemini en Antigravity: qué leer primero, compuerta de código, principio rector, reglas R1 a R6, prohibiciones, manejo de dudas y protocolo de la Bitácora. Si cambia una decisión aceptada, hay que actualizar también esta skill. |
| `.agent/skills/generar-mockup-html/` | Skill para generar mockups como una página HTML autocontenida, con plantilla (`assets/esqueleto-mockup.html`), proceso, reglas de contenido y técnicas, y verificación. Los mockups van en `docs/mockups/`. |
| `.agent/skills/planificar-funcionalidad/references/` | Plantillas de plan, método de estimación, replanificación y el ejemplo real del plan de Inventario. |
| `.agent/skills/README.md` y las otras 16 carpetas de `.agent/skills/` | Paquete de skills para Antigravity: planificar, diseñar módulos, contratos de eventos y de API, modelar la base de datos, diagramas, revisar diseño, código y seguridad, pruebas, IA, fuentes de datos, bitácora, interfaz, demo del hackathon y diagnóstico de fallos. El README tiene el índice y los flujos típicos. |
| Mapa de Inventario (página publicada) | https://claude.ai/artifact/1M1DNRybgJoXheZmS7JJJt — vista visual de los dos documentos anteriores: mapa de comunicación, filtro de interacciones, secuencias, eventos y explorador de tablas. Privada: solo la ve quien tenga acceso. |
| Mockup de la plataforma (página publicada) | https://claude.ai/artifact/3o4no2gWTCwAkG5tcatffJ — flujos, módulos, pantallas, sección de APIs (GraphQL, WebSocket, REST). Marca como primera opción de stack a Python; **ya está desactualizada en eso** (ver D-03). |
| Mockup original (página publicada) | https://claude.ai/artifact/SgG3tYJ4SVua6qagM6QVM1 — bot, panel de gestión y tablero de gobierno del alcance anterior. |

---

## 1. Qué es el proyecto

Proyecto para el hackathon **"Puebla Turística 360°: Datos que transforman experiencias"** (Tech-Capital Puebla 2026), del 15 al 17 de octubre de 2026 en el Centro Expositor. Equipo de 3 estudiantes más un profesor tutor. Se evalúa el resultado final en el evento: prototipo demostrable, pitch y demo.

Rúbrica (resumen): datos 20%, comprensión del problema 15%, innovación 15%, solidez técnica del prototipo 15%, impacto 15%, viabilidad 10%, experiencia de usuario 5%, pitch 5%. La IA debe tener un valor real y justificado.

### Principio rector
**"El modelo interpreta, el código decide."** El LLM entiende intención y extrae datos a un esquema fijo; el código valida, calcula cada cifra y escribe en la base de datos. El LLM nunca toca la base de datos, nunca inventa números y nunca decide el `negocio_id` (sale de la sesión o del webhook). Nunca hay SQL libre generado por el modelo.

### Reglas de seguridad y convenciones que siguen vigentes (de `CLAUDE.md`)
- Nunca pedir ni almacenar credenciales del SAT.
- Toda escritura del usuario pasa por **preparar → confirmar** con token de un solo uso.
- Toda cifra mostrada viene de código determinista.
- Las reglas de negocio no se hardcodean: viven como datos versionados.
- Idioma de interfaz, plantillas y documentación: español de México.
- Datos simulados siempre etiquetados como simulados.
- No introducir dependencias ni funciones fuera del alcance del MVP sin preguntar.
- Sin secretos en el código.

---

## 2. Cambio de rumbo: de "Negocio 360" a plataforma modular

Hasta el 7 de octubre el alcance era un asistente conversacional (Telegram o WhatsApp) más un panel de gestión para prestadores turísticos. Entre el 7 y el 8 de octubre el equipo reorientó la plataforma en nueve puntos:

1. **Módulos horizontales** compartidos por todos los nichos: inventario, contabilidad, caja, ventas, clientes.
2. **Nichos como módulos verticales** (Restaurantes, Museos, Misceláneos) que consumen a los horizontales.
3. **Plataforma central** donde turistas y locales ven reseñas, menú, información general, horas pico y disponibilidad de mesas o atención.
4. **Afiliación a un pasaporte turístico** con cupones para incentivar el consumo y la estancia.
5. **Filtros por intereses y tipo de viaje** (cultura y arte, gastronomía local, festividades, noche o día, familiar, presupuesto).
6. **Búsqueda inteligente con IA** (Gemini o ChatGPT) que consulta nuestra API y genera rutas de visita.
7. **Categorías y subcategorías.** Restaurante: gluten free, verano, mixto, gastronomía típica, comida corrida, extranjera. Museos: historia, cultura, arte.
8. **Ubicación en tiempo real** y recomendaciones.
9. **Cuestionario posterior al registro:** tipo de visitante, 3 a 5 intereses, estilo de viaje.

Además: rutas del transporte público, qué autobuses tomar, medios de pago, y las líneas del sistema RUTA.

### Arquitectura de comunicación (de la sesión de mockups)
- **Monolito modular**. Cada módulo es dueño de sus datos y se comunica por API interna o por bus de eventos.
- **GraphQL** sobre HTTPS para la app del turista, el panel del locatario y el tablero de gobierno. Las suscripciones de GraphQL viajan por **WebSocket** en el mismo endpoint.
- **REST** para contratos fijos: herramientas de la IA (`/ia/v1`), autenticación, subida de archivos, webhooks del bot, administración y salud.
- **Bus de eventos** con outbox transaccional y consumidores idempotentes.
- La ubicación del turista no va por WebSocket: se manda en la consulta de inicio cuando se mueve más de 150 m o cada minuto, sin guardar el recorrido.

---

## 3. Estado del alcance (`CLAUDE.md` actualizado el 2026-10-08)

`CLAUDE.md` describía el alcance anterior. El 2026-10-08 se actualizó con una nota de estado, el alcance y la arquitectura vigentes, y se marcó como «original» lo que quedó del diseño previo (máquina de estados del bot, flujo en n8n, endpoints, objetivos y alcance originales). **Si algo de `CLAUDE.md` contradice esta Bitácora, manda la Bitácora.** La tabla muestra lo que cambió:

| Tema | `CLAUDE.md` decía (versión original) | Estado actual |
|---|---|---|
| Inventario | "Fuera de alcance (roadmap)" | **Dentro del MVP** (D-08) |
| Stack | Next.js, n8n, Supabase | Backend en **Java 21 + Spring Boot** (D-03). Frontend y orquestación del bot siguen sin decidirse |
| Producto | Asistente + panel para prestadores | Plataforma modular con turistas, pasaporte, rutas con IA y transporte |
| Endpoints del bot | `/ventas/preparar`, etc. | Se mantienen bajo `/bot` (REST), pero el diseño detallado está pendiente |

La actualización de `CLAUDE.md` se hizo el 2026-10-08 (ver el registro). Sigue siendo un documento mixto: lo vigente está al inicio de cada sección y lo original está rotulado.

---

## 4. Decisiones

| ID | Decisión | Estado | Fecha | Notas |
|---|---|---|---|---|
| D-01 | Monolito modular con horizontales y verticales | ACEPTADA | 2026-10-08 | Pivote de nueve puntos |
| D-02 | GraphQL para la app, WebSocket para tiempo real (suscripciones), REST para IA y contratos fijos | PROPUESTA | 2026-10-08 | Plasmada en el mockup sin objeción del usuario |
| D-03 | Backend en **Java 21 con Spring Boot** | ACEPTADA | 2026-10-08 | Dicho por el usuario. Reemplaza la recomendación del mockup (Python con FastAPI y Strawberry). GraphQL con Spring for GraphQL |
| D-04 | **R1:** cada módulo es dueño de sus datos; los demás entran por API pública o bus. "API" = interfaz Java en el mismo proceso. Esquema de Postgres propio por módulo | ACEPTADA | 2026-10-08 | Regla del usuario, con precisión añadida |
| D-05 | **R2 corregida:** síncrono = llamada a la API pública; asíncrono = evento por el bus | ACEPTADA | 2026-10-08 | Corrige la redacción original ("síncrona o asíncrona por el bus"): un evento no devuelve respuesta. Aceptada con el mensaje "se implementan las reglas extras que nombraste" |
| D-06 | **R3:** todo evento con ID único (UUIDv7) y consumidores idempotentes | ACEPTADA | 2026-10-08 | Regla del usuario, con idempotencia añadida |
| D-07 | **R4** (dependencias solo vertical → horizontal), **R5** (eventos versionados y aditivos), **R6** (entre horizontales rige R2) | ACEPTADA | 2026-10-08 | "Reglas extras" propuestas por el asistente |
| D-08 | Inventario del MVP incluye: conversión de unidades, CPP, lotes y caducidades, números de serie, recetas con descuento por platillo | ACEPTADA | 2026-10-08 | Eran roadmap en la versión 1 del plan |
| D-09 | Las recetas viven en **Restaurantes** | ACEPTADA | 2026-10-08 | |
| D-10 | Salida de lotes **FEFO por defecto**, configurable | ACEPTADA | 2026-10-08 | La granularidad "por negocio, con excepción por artículo" es PROPUESTA (pregunta 1 del doc de diseño). El usuario dijo "para cada usuario" |
| D-11 | CPP **sin recálculo retroactivo**; la política es configurable (`politica_costeo`) | ACEPTADA | 2026-10-08 | En el MVP solo existe `SIN_RECALCULO`. Otras políticas quedan reservadas, sin implementar |
| D-12 | Orden de recorte si falta tiempo: primero **series**, luego **lotes** | ACEPTADA | 2026-10-08 | |
| D-13 | Stock negativo permitido con alerta; el saldo se corrige por conteo, no a mano; una unidad base por artículo | ACEPTADA | 2026-10-08 | Aceptada de forma implícita ("las reglas extras que nombraste"). Verificar si hay objeción |
| D-14 | Build con **Maven** | PROPUESTA | 2026-10-08 | En la máquina solo están Java 21.0.10 y Maven 3.9.14; no hay Gradle |
| D-15 | Pruebas de integración con **Testcontainers** (Postgres real) | PROPUESTA | 2026-10-08 | Requiere Docker Desktop encendido |
| D-16 | Dónde vive la base de datos de la demo (Supabase o contenedor) | ABIERTA | 2026-10-08 | |
| D-17 | El **modo de inventario** de un producto (`NINGUNO`, `VINCULO_DIRECTO`, `RECETA`) lo lleva **Catálogo** | PROPUESTA | 2026-10-08 | Evita doble descuento sin romper R4. Reemplaza el plan v2, donde Restaurantes preguntaba a Inventario |
| D-18 | Llave de idempotencia del consumo: `(negocio, causaId, consumidor)` | PROPUESTA | 2026-10-08 | Evita que el consumo por vínculo y el de Restaurantes choquen con el mismo `venta_id` |
| D-19 | Marca de anulación previa cuando `venta.anulada` llega antes que `venta.registrada` | PROPUESTA | 2026-10-08 | No hay orden global de eventos |
| D-20 | Redondeo HALF_UP; escalas: cantidades 4, CPP 4, dinero 2, factores 8 | PROPUESTA | 2026-10-08 | |
| D-21 | Lotes caducados no se asignan automáticamente en FEFO | PROPUESTA | 2026-10-08 | |
| D-22 | `registrarConsumo` y `revertirConsumo` no usan preparar → confirmar | PROPUESTA | 2026-10-08 | Son consecuencia de una venta ya confirmada. Es una excepción a la convención de `CLAUDE.md` |
| D-23 | El token de preparar → confirmar expira a los 5 minutos | PROPUESTA | 2026-10-08 | |
| D-24 | `consumo.estado` gana el valor `FALLIDO` y se publica `inventario.consumo_fallido` v1 | PROPUESTA | 2026-10-08 | El listener de vínculo directo no tenía dónde registrar un error permanente. Con esto Inventario publica 9 eventos y no 8 |
| D-25 | `venta.anulada` v1 incluye `lineas[]` (`linea`, `producto_id`); se purgan las marcas `ANULADO_PREVIO` de más de 30 días | PROPUESTA | 2026-10-08 | Sin las líneas, cada consumidor dejaría marcas inútiles. Hay que congelarlo con el dueño de Ventas |
| D-26 | `lote` guarda `aviso_por_caducar_en` y `aviso_caducado_en` | PROPUESTA | 2026-10-08 | Para que los avisos de caducidad sean idempotentes (L-07) |
| D-27 | `aBase` devuelve `rastreo` y `activo`; Restaurantes rechaza series con `SERIE_EN_RECETA` | PROPUESTA | 2026-10-08 | Permite cumplir S-05 sin que Inventario dependa de Restaurantes |
| D-28 | `registrarConsumo` acepta `eventoCausaId` opcional | PROPUESTA | 2026-10-08 | Para llenar `causa_id` en los eventos |
| D-29 | `preparacion` guarda además el `comando` completo (jsonb) | PROPUESTA | 2026-10-08 | `confirmar` lo necesita para ejecutar lo que se preparó |
| D-30 | Una venta de artículo con serie y sin serie sí descuenta el saldo; `AsignarSerie` solo marca la serie como `VENDIDA` | PROPUESTA | 2026-10-08 | Precisa S-04 |
| D-31 | Estructura de ramas: `liz`, `uriel` y `julio`, cada una con su rama de testing (`liz-testing`, `uriel-testing`, `julio-testing`), y una rama `pre-main` que es la última antes de `main` | ACEPTADA | 2026-10-08 | Dicho por el usuario. Creadas solo en local |
| D-32 | Reglas del flujo de ramas: cada persona trabaja en su rama, prueba en su rama de testing, integra a `pre-main` por pull request revisado por otra persona y de ahí pasa a `main`; `main` y `pre-main` protegidas en GitHub; nombres con guion y no con barra | PROPUESTA | 2026-10-08 | El usuario definió las ramas, no el flujo de integración. Git no permite `liz` y `liz/testing` a la vez |

### Decisiones de producto abiertas (vienen de la sesión de mockups)
| Tema | Opciones | Estado |
|---|---|---|
| Modelo de IA para rutas | Gemini o ChatGPT | ABIERTA |
| Bot de chat (Telegram o WhatsApp) para locatarios | Se mantiene o se descarta | ABIERTA |
| Verticales reales en la demo | Solo Restaurantes real, Museos y Misceláneos como mockup (recomendado) | ABIERTA |
| Aplicación del turista y del locatario | Una PWA con dos modos, o dos aplicaciones | ABIERTA |
| Frontend y orquestación del bot | Next.js y n8n según `CLAUDE.md`, sin confirmar con el nuevo backend | ABIERTA |

---

## 5. Hechos del entorno (verificados el 2026-10-08)

| Elemento | Estado |
|---|---|
| Sistema | Windows 11 Home. Shell principal PowerShell; también hay Bash |
| Java | 21.0.10 LTS (Oracle) |
| Maven | 3.9.14 |
| Gradle | No instalado |
| Docker | Instalado pero **apagado** (no responde la API de Docker Desktop) |
| Postgres / `psql` | No instalados localmente |
| Repositorio | Carpeta `C:\Users\Julio\OneDrive\Documentos\BUAP\Hackathon_Turismo_puebla`. **Ya es un repositorio git** (actualizado el 2026-10-08): tiene `.git`, rama `main`, remoto `origin` en GitHub (`Julio-Cuellar/overtourism`) y un primer commit. No se verificó qué archivos están confirmados ni subidos, y no hay `.gitignore`. Está dentro de OneDrive (puede bloquear archivos de compilación y conviene excluir `target/`; tener `.git` dentro de OneDrive puede generar conflictos de sincronización) |
| Código existente | **Ninguno.** Solo `CLAUDE.md` y estos documentos |
| Fecha de hoy / demo | 2026-10-08 / 2026-10-15 al 17. Quedan 7 días |

## 6. Notas operativas para agentes

- **Estamos en fase de diseño.** El usuario pidió expresamente (2026-10-08): "no generes codigo aún, estamos totalmente en fase de diseño". No crees proyectos, ni scaffolding, ni código hasta que el equipo apruebe el diseño y lo diga explícitamente.
- El comando `/ecc:plan` exige esperar confirmación explícita del usuario antes de escribir código.
- Hay un hook de seguridad (GateGuard) que bloquea la primera escritura sobre cada archivo nuevo, y el primer comando de Bash o PowerShell de la sesión. Hay que enunciar los hechos que pide (quién llama al archivo, que no hay duplicado, qué datos maneja, la instrucción del usuario citada textualmente) y repetir la misma llamada.
- Evita ejecutar comandos de shell si no son necesarios.
- Los nombres de negocios en los mockups son ficticios (Fonda Doña Chela, Museo Talavera Viva, Sushi Volcán, Cocina Santa Clara, Dulces Doña Lupe). Los datos de transporte (RUTA tiene 4 líneas, se paga con tarjeta prepagada) son parciales; los medios de pago de los autobuses no están confirmados y no existe GTFS público: usar OpenStreetMap más captura propia, rotulado como estimado.

---

## 7. Registro de sesiones

### 2026-10-07 y 2026-10-08 — Sesión de mockups (fuente: la conversación origen de esta)
**Qué se vio:**
- Se generó el primer mockup del alcance original: arquitectura, demo interactiva de mensaje a panel, secuencias de eventos, seis pantallas, cuadrícula de gobierno y reglas de UX.
- Tras el pivote de nueve puntos se generó el segundo mockup (plataforma): capas, matriz horizontales × verticales, bus de eventos, flujos con Mermaid, categorías, ocho pantallas de la app del turista, rutas con IA, transporte, pasaporte con QR y panel del locatario.
- Se discutió el stack: se recomendó Python (FastAPI, Strawberry) por GraphQL y el trabajo con IA; la alternativa era Java con Spring (Spring Modulith, Spring for GraphQL).
- Se decidió documentar en el mockup qué va por GraphQL, WebSocket y REST. Se añadió la sección "APIs".

**Decisiones:** D-01, D-02 (propuesta). Las cinco decisiones abiertas de producto quedaron registradas (sección 4).

**Pendiente:** responder las decisiones de producto; actualizar `CLAUDE.md`.

**Archivos:** los dos mockups publicados (enlaces arriba). Ningún archivo en el repositorio.

### 2026-10-08 — Sesión de diseño del módulo de Inventario (esta bitácora nace aquí)
**Contexto:** esta sesión es una bifurcación de la de mockups. El usuario fijó el stack **Java 21 + Spring Boot** y pidió empezar por Inventario.

**Qué se vio:**
1. El usuario dio tres reglas de arquitectura (propiedad de datos, comunicación vertical → horizontal por bus, ID único por evento). El asistente las analizó: adoptó las tres, **corrigió** la regla 2 (un evento no puede devolver respuesta; síncrono = API pública) y propuso R4, R5 y R6.
2. **Plan v1** de Inventario (MVP mínimo: artículos, movimientos, saldo, kardex, preparar → confirmar, descuento por venta 1:1, alertas, GraphQL). Estimación de 4 a 4.5 días-persona.
3. **Plan v2:** el usuario pidió sumar lotes, caducidades, números de serie, conversión de unidades, CPP, recetas y descuento por platillo. El alcance subió a unos 11 días-persona. El asistente advirtió el riesgo de calendario (demo en 7 días) y propuso un orden de recorte.
4. El usuario respondió: recetas en Restaurantes; FEFO por defecto y configurable; CPP sin recálculo retroactivo y configurable; se acepta el orden de recorte; se implementan las reglas extras.
5. El asistente intentó verificar herramientas del entorno para empezar la fase 0. El usuario **interrumpió**: no generar código todavía, la fase es de diseño. Se leyeron versiones (Java 21.0.10, Maven 3.9.14, Docker apagado, sin `psql`) y no se creó nada.
6. El asistente propuso cuatro preguntas abiertas; el usuario respondió **sí** a la cuarta: generar el documento de diseño con los contratos y esta bitácora.
7. Al redactar los contratos se detectó un hueco: la regla "un producto tiene receta o vínculo, nunca ambos" no se podía verificar desde Inventario sin depender de Restaurantes (rompe R4). Solución propuesta: el modo de inventario del producto lo lleva Catálogo (D-17).
8. También se detectó que el consumo por vínculo y el de Restaurantes usarían el mismo `venta_id` como llave, lo que causaría choque de idempotencia. Solución propuesta: añadir `consumidor` a la llave (D-18).

**Decisiones:** D-03 a D-13 aceptadas; D-14 a D-23 propuestas (ver sección 4).

**Pendiente (preguntas del documento de diseño, sección 13):**
- Confirmar que FEFO y costeo se configuran por negocio y no por usuario.
- Confirmar Maven y Docker Desktop encendido.
- Decidir dónde vive la base de datos de la demo (D-16).
- Aceptar D-17 (modo de inventario en Catálogo) y D-22 (consumo sin confirmación humana).
- Aceptar los detalles D-18 a D-21 y D-23.
- Identificar quién es dueño de Catálogo y de Ventas para congelar con ellos los contratos de `venta.registrada` y `venta.anulada`.
- Actualizar `CLAUDE.md` al nuevo alcance.

**Archivos creados:**
- `docs/diseno/inventario.md` (diseño v0.1: reglas, datos, API, eventos, pruebas, fases)
- `docs/BITACORA.md` (este archivo)

**Siguiente paso esperado:** el equipo revisa el documento de diseño y responde la sección 13. Solo después de una aprobación explícita se abre la fase 0 (esqueleto Maven, módulo `compartido`, Testcontainers, Flyway y `verify()`).

### 2026-10-08 — Diagramas, interacciones y modelo de datos de Inventario
**Pedido del usuario:** generar los diagramas de secuencia y de eventos del módulo, la lista de eventos síncronos y asíncronos con su función, y el modelo de la base de datos, en un documento y como página visual.

**Qué se vio:**
1. Se dibujaron ocho secuencias (entrada de mercancía, venta con receta, venta con vínculo y con serie, anulación en orden y fuera de orden, entrega repetida de un evento, revisión diaria de caducidades, alerta hasta el panel, guardar receta), dos diagramas de eventos (ciclo de vida y cadena causal) y cuatro diagramas entidad-relación.
2. La lista de interacciones quedó en 18 llamadas síncronas (13 hacia Inventario, 3 hacia Catálogo y 2 hacia Restaurantes) y 11 eventos asíncronos (2 que Inventario consume y 9 que publica).
3. **Al dibujar aparecieron siete huecos en `inventario.md` v0.1.** Se registraron como D-24 a D-30, todos en estado PROPUESTA:
   - No había dónde registrar un fallo permanente del consumo por vínculo directo (D-24).
   - `venta.anulada` no traía las líneas (D-25).
   - No había dónde guardar que un aviso de caducidad ya se mandó (D-26).
   - La regla de que una serie no puede ser insumo no se podía verificar (D-27).
   - `registrarConsumo` no recibía el evento causante (D-28).
   - `preparacion` no guardaba el comando completo, que `confirmar` necesita (D-29).
   - No estaba dicho si una venta sin serie descuenta el saldo (D-30).
4. Se completó el modelo con tipos, restricciones e índices: kardex, localizar movimientos de una venta, asignación FEFO, revisión diaria y purgas. Se recomienda reforzar con llaves foráneas compuestas que incluyan `negocio_id`.

**Observaciones para quien continúe:**
- `docs/diseno/inventario.md` **sigue en v0.1 y no incorpora D-24 a D-30**. Los cambios solo viven en `inventario-diagramas.md`, sección 9. Si el equipo los acepta, hay que reflejarlos en `inventario.md` (eventos publicados: de 8 a 9; estado de `consumo`; columnas de `lote` y `preparacion`; contrato de `venta.anulada`, `aBase` y `registrarConsumo`).
- La primera línea de `docs/diseno/inventario.md` tiene texto basura antepuesto al encabezado (una secuencia de corchetes y paréntesis, `[[[[[[[[[]()...`). El archivo cambió en disco después de crearse; no se sabe quién lo modificó. No se corrigió.
- Los diagramas de Mermaid y la página no se verificaron visualmente: no se renderizaron antes de publicarlos. Si algún diagrama no se dibuja, revisar primero su sintaxis.
- La hora exacta de la revisión diaria de caducidades sigue por definir.

**Decisiones:** D-24 a D-30 (propuestas).

**Pendiente:**
- Que el equipo acepte o rechace D-24 a D-30.
- Congelar con el dueño de Ventas `venta.registrada` y `venta.anulada` (incluye D-25).
- Definir con el dueño de Catálogo `existeProducto`, `modoInventario` y `establecerModoInventario`.
- Las preguntas de la sección 13 de `inventario.md` siguen abiertas.

**Archivos creados:**
- `docs/diseno/inventario-diagramas.md`
- Página publicada "Mapa de Inventario" (enlace en la tabla de documentos). El archivo fuente está solo en la carpeta temporal de la sesión, no en el repositorio.

**Archivos modificados:** `docs/BITACORA.md` (tabla de documentos, decisiones D-24 a D-30 y esta entrada).

### 2026-10-08 — Skill de reglas para agentes de Gemini en Antigravity
**Pedido del usuario:** generar un documento de reglas del proyecto, en forma de skill para Google Gemini en Antigravity, para que los agentes del equipo no pierdan contexto ni hagan lo que no deben.

**Qué se vio:**
1. Antigravity carga las skills desde `.agent/skills/<nombre>/SKILL.md` en el proyecto (existe también una ruta global, `~/.gemini/antigravity/skills`). El encabezado lleva `name` y `description`, y el agente decide si carga la skill comparando la tarea con la `description`. Fuente: el codelab de Google "Authoring Google Antigravity Skills" y blogs de terceros; no se consultó la documentación oficial de referencia, y las fuentes no coinciden en si `name` es obligatorio (se incluyó de todos modos).
2. La skill reúne lo que ya estaba decidido y no inventa reglas nuevas: orden de lectura, fuentes de verdad, compuerta de código (no programar sin autorización explícita anotada en la Bitácora), principio rector, reglas R1 a R6 y lo que prohíben en la práctica, lista de prohibiciones, manejo de dudas, convenciones y protocolo de cierre en la Bitácora.
3. La skill incluye una instantánea del estado al 2026-10-08. Puede quedar vieja: la propia skill indica que gana la Bitácora.

**Decisiones:** ninguna nueva.

**Pendiente:**
- Probar la skill en Antigravity: no se probó que el IDE la detecte ni que el agente la cargue con la `description` actual.
- Decidir si se agrega una regla que se cargue siempre (por ejemplo `.agent/rules/`), porque una skill solo se carga cuando la `description` coincide con la tarea.
- Mantener la skill sincronizada: cuando una decisión pase de propuesta a aceptada, actualizar la sección 10 de la skill.

**Archivos creados:** `.agent/skills/negocio360-reglas/SKILL.md`.
**Archivos modificados:** `docs/BITACORA.md` (tabla de documentos y esta entrada).

### 2026-10-08 — Paquete de skills para agentes de Antigravity
**Pedido del usuario:** generar otras skills para planificación, diseño, revisión, etc., un paquete completo de skills útiles.

**Qué se vio:**
1. Se crearon 16 skills nuevas en `.agent/skills/`, más un `README.md` con el índice y los flujos típicos. Con `negocio360-reglas` son 17 skills.
2. Cobertura: planificar (`planificar-funcionalidad`), diseñar (`disenar-modulo`, `disenar-interfaz`), contratos (`contratos-eventos`, `contratos-api`), datos (`modelar-base-de-datos`, `documentar-fuentes-de-datos`), diagramas (`diagramas-mermaid`), revisión (`revisar-diseno`, `revisar-codigo`, `revisar-seguridad`), pruebas (`escribir-pruebas`), IA (`integrar-ia`), cierre de sesión (`registrar-en-bitacora`), hackathon (`preparar-demo-hackathon`) y diagnóstico (`diagnosticar-fallo`).
3. Las skills no inventan reglas nuevas: condensan lo ya decidido (R1 a R6, principio rector, compuerta de código) y lo aprendido al diseñar Inventario, como los «huecos típicos» que se repiten entre módulos y los errores de sintaxis de Mermaid que ya ocurrieron.
4. Las skills `revisar-codigo` y `escribir-pruebas` se escribieron para cuando haya código, y dicen que solo aplican si la Bitácora lo autoriza. Hoy no está autorizado.
5. En `preparar-demo-hackathon`, los dos caminos de demo candidatos están marcados como propuestas que dependen de las decisiones de producto abiertas.

**Decisiones:** ninguna nueva.

**Observaciones para quien continúe:**
- Ninguna skill se probó dentro de Antigravity. No se sabe si el IDE las detecta todas ni si el agente carga la correcta con cada `description`.
- Los nombres de herramientas de pruebas de Spring Modulith (`@ApplicationModuleTest`, `Scenario`, `ApplicationModules.verify()`) y de GraphQL (`GraphQlTester`) salen de conocimiento general y no se verificaron contra las versiones que el equipo elija. Las skills piden verificar versiones compatibles.
- Las skills duplican algunas reglas de `negocio360-reglas` a propósito, para que funcionen si solo se carga una. Si una regla cambia, hay que cambiarla en todas (el README lo indica).

**Pendiente:**
- Probar el paquete en Antigravity con tareas reales y ajustar las `description` que no se activen.
- Decidir si se agrega una regla que se cargue siempre (`.agent/rules/`) apuntando a `negocio360-reglas`.
- Mantener sincronizada la instantánea de `negocio360-reglas` (sección 10) cuando cambien las decisiones.

**Archivos creados:** `.agent/skills/README.md` y `.agent/skills/<nombre>/SKILL.md` para: `planificar-funcionalidad`, `disenar-modulo`, `contratos-eventos`, `contratos-api`, `modelar-base-de-datos`, `diagramas-mermaid`, `revisar-diseno`, `revisar-codigo`, `escribir-pruebas`, `revisar-seguridad`, `integrar-ia`, `documentar-fuentes-de-datos`, `registrar-en-bitacora`, `disenar-interfaz`, `preparar-demo-hackathon`, `diagnosticar-fallo`.
**Archivos modificados:** `.agent/skills/negocio360-reglas/SKILL.md` (sección 11 con la lista de skills), `docs/BITACORA.md` (tabla de documentos y esta entrada).

### 2026-10-08 — Skill de mockups y ampliación de la skill de planificación
**Pedido del usuario:** generar una skill para que los agentes de Gemini produzcan mockups como los que se hicieron en la sesión de diseño, y dejar «totalmente cubierta» al menos la skill de planificación.

**Qué se vio:**
1. Se creó `generar-mockup-html`: tipos de mockup, proceso de ocho pasos, reglas de contenido (datos inventados, cifras que cuadran, operación de API en cada pantalla, cinco estados), reglas técnicas (un archivo, tokens claro y oscuro, responsivo, accesible), cómo hacer diagramas Mermaid y SVG, verificación y registro. Incluye la plantilla `assets/esqueleto-mockup.html` con un teléfono con selector de estados, un mapa de arquitectura con la convención de flechas y un diagrama Mermaid.
2. La skill aclara que **un mockup es un documento de diseño y se permite en fase de diseño** aunque el código no esté autorizado, siempre que no cree proyecto ni toque el backend. Es una interpretación mía de la compuerta de código de `negocio360-reglas`; si el equipo piensa distinto, hay que ajustar la skill.
3. `planificar-funcionalidad` pasó de unos 60 renglones a una skill con tres niveles de plan (rápido, estándar y mayor), un procedimiento de 14 pasos, tabla para interpretar la respuesta del usuario, qué hacer tras la confirmación, replanificación, autoevaluación y errores comunes. Se agregaron cuatro archivos en `references/`: plantillas, estimación, replanificación y el ejemplo real del plan de Inventario.
4. En el ejemplo se anotaron seis lecciones del plan de Inventario: faltó una fase de revisión del diseño, no se fijaron las horas efectivas detrás del «día-persona», no hubo tabla de capacidad contra demanda, los contratos con Ventas y Catálogo quedaron sin dueño ni fecha, no se dibujó el camino dorado de la demo y no se presentó el delta entre versiones.
5. Se reconoce en `estimacion.md` que los planes anteriores usaron «día-persona» sin fijar las horas, así que con estudiantes de jornada parcial pueden ser optimistas.

**Decisiones:** ninguna nueva.

**Observaciones para quien continúe:**
- Las cifras de `estimacion.md` (de 4 a 5 horas efectivas por día para estudiantes, colchón de 20% a 30%) son **heurísticas mías**, no datos del equipo. La skill las marca como supuestos que hay que corregir con la disponibilidad real.
- La plantilla carga Mermaid desde un CDN con la versión `10.9.1`. **No se verificó que esa versión exista ni que la plantilla se dibuje bien**; no se abrió en un navegador. La skill pide verificar la versión.
- Las skills no se probaron en Antigravity.

**Pendiente:**
- Probar la plantilla en un navegador y corregir lo que falle.
- Que el equipo diga su disponibilidad real (horas efectivas por persona y día) para fijar la capacidad en los planes.
- Decidir si se agrega la regla que se cargue siempre (`.agent/rules/`).

**Archivos creados:** `.agent/skills/generar-mockup-html/SKILL.md`, `.agent/skills/generar-mockup-html/assets/esqueleto-mockup.html`, `.agent/skills/planificar-funcionalidad/references/plantilla-plan.md`, `estimacion.md`, `replanificacion.md` y `ejemplo-plan-inventario.md`.
**Archivos modificados:** `.agent/skills/planificar-funcionalidad/SKILL.md` (reescrita), `.agent/skills/README.md`, `.agent/skills/disenar-interfaz/SKILL.md`, `.agent/skills/negocio360-reglas/SKILL.md`, `docs/BITACORA.md`.

### 2026-10-08 — README del repositorio con las instrucciones de las skills
**Pedido del usuario:** agregar al README del repositorio las instrucciones de cómo utilizar las skills.

**Qué se vio:**
1. Al revisar la carpeta apareció algo nuevo que no se hizo en esta sesión: un `README.md` en la raíz con solo el título `# overtourism`, y un `.git` con remoto en GitHub (`Julio-Cuellar/overtourism`), rama `main` y un primer commit. Antes la Bitácora decía que no era un repositorio git; se corrigió ese dato en la sección 5.
2. El README conserva el título `# overtourism` (nombre del repositorio) y agrega: descripción del proyecto, estado (fase de diseño, sin código), estructura del repositorio, por dónde empezar y la guía de uso de las skills.
3. La guía de skills explica cómo se cargan (por la `description`), la preparación (abrir la carpeta raíz), las 17 skills con cuándo usar cada una, ejemplos de mensajes por tarea, flujos típicos, cómo comprobar que funcionan (con señales de buen y mal comportamiento), qué hacer si una no se activa, skills globales, reglas del equipo, mantenimiento y límites.
4. Se incluyó una nota para quien use Claude Code: `CLAUDE.md` se lee solo, las skills de `.agent/` son para Antigravity.

**Decisiones:** ninguna nueva.

**Observaciones para quien continúe:**
- **Nada se confirmó ni se subió a GitHub.** No se ejecutó ningún comando de git. Mientras los archivos nuevos (`docs/`, `.agent/`, `README.md`, `CLAUDE.md`) no se confirmen y se suban, los compañeros no los reciben. Es una acción que decide el equipo.
- No hay `.gitignore`. Antes de empezar a programar conviene agregar uno (por ejemplo para `target/`, archivos de entorno y claves).
- El README afirma que el paquete **no se ha probado en Antigravity**. Esa afirmación debe actualizarse cuando alguien lo pruebe.
- Los pasos de «Si una skill no se activa» y la ruta global de skills salen de fuentes de terceros y de mi conocimiento general; no se verificaron en la documentación oficial de la versión instalada.
- Se incluyó en el README la URL del remoto leída de `.git/config`. Si el repositorio es privado, la instrucción de clonar solo funciona para quien tenga acceso.

**Pendiente:**
- Probar el paquete de skills en Antigravity y actualizar el README según el resultado.
- Decidir cuándo confirmar y subir los documentos y las skills a GitHub, y agregar un `.gitignore`.

**Archivos creados:** ninguno.
**Archivos modificados:** `README.md`, `docs/BITACORA.md` (tabla de documentos, sección 5 y esta entrada).

### 2026-10-08 — Actualización de `CLAUDE.md`
**Pedido del usuario:** actualizar `CLAUDE.md` con lo que se viene trabajando, con la opción de edición puntual (no reescribirlo).

**Qué se vio:**
1. `CLAUDE.md` describía el producto anterior (asistente más panel; Supabase, Next.js y n8n; endpoints REST; inventario fuera de alcance). Cada sesión de Claude Code lo lee, así que contradecía el diseño.
2. **Se agregó:** una nota de estado al inicio (la Bitácora manda; fase de diseño, sin código), el alcance vigente en nueve puntos, la arquitectura vigente (Java 21 con Spring Boot, monolito modular, reglas R1 a R6, GraphQL, WebSocket y REST, eventos), el alcance vigente del MVP (Inventario con prioridades y orden de recorte), las rutas con IA como componente de IA, convenciones nuevas (no programar sin autorización, registrar en la Bitácora, reglas R1 a R6, diseño por módulo, skills) y notas de avance en los pendientes.
3. **Se rotuló como «original»:** objetivos específicos, stack inicial, máquina de estados del bot, flujo en n8n, endpoints y alcance del MVP original. Varias viñetas del stack ahora dicen qué sigue abierto (base de datos de la demo, tecnología del frontend, bot).
4. **No se tocó:** reglas de la convocatoria, rúbrica, entregables, principio rector, garantías de veracidad, seguridad, reglas de negocio modificables, fuentes de datos y riesgos.
5. Se corrigieron las referencias que decían que `CLAUDE.md` estaba desactualizado: sección 3 de la Bitácora, fila de documentos, `README.md` y `negocio360-reglas`.

**Decisiones:** ninguna nueva.

**Observaciones para quien continúe:**
- `CLAUDE.md` es ahora un documento **mixto**: lo vigente está al inicio de cada sección y lo original está rotulado. Es la opción de edición puntual, la más barata; el costo es que sigue siendo largo y puede volver a desactualizarse. La alternativa, reescribirlo corto y dejar lo cambiante en la Bitácora, quedó descartada por ahora.
- El estado que `CLAUDE.md` menciona (D-16, D-22, etc.) puede cambiar. Cuando una decisión se acepte, hay que revisar que no se contradiga en `CLAUDE.md`.
- No se verificó cómo se lee el archivo completo después de las ediciones.

**Pendiente:**
- Revisar `CLAUDE.md` completo y confirmar que se lee bien.
- Decidir si más adelante conviene reducirlo a lo estable.

**Archivos creados:** ninguno.
**Archivos modificados:** `CLAUDE.md`, `README.md`, `.agent/skills/negocio360-reglas/SKILL.md`, `docs/BITACORA.md`.

### 2026-10-08 — `.gitignore` y regla permanente para los agentes
**Pedido del usuario:** agregar el `.gitignore` y la regla que se cargue siempre.

**Qué se vio:**
1. Se consultó la documentación oficial de Antigravity sobre reglas y skills. Hallazgos:
   - La carpeta por defecto es `.agents/` (plural); `.agent/` sigue aceptada por compatibilidad. Este repositorio usa `.agent/`.
   - Todo archivo de `rules/` necesita un encabezado YAML con `trigger`. `always_on` inyecta la regla en cada turno. Un `trigger` inválido hace que la regla se **descarte en silencio**.
   - Límite por archivo de reglas: 24,000 bytes. Las reglas globales y las `always_on` comparten un presupuesto de 20,000 tokens.
   - En las skills solo `description` es obligatoria; `name` es opcional y toma el nombre de la carpeta (queda resuelta la duda anotada antes).
   - La ruta global de skills ya no es solo `~/.gemini/antigravity/skills/`: en Antigravity 2.0 y el IDE es `~/.gemini/config/skills/`. Se corrigió en el README y en el índice de skills.
2. Se creó `.agent/rules/negocio360.md` con `trigger: always_on`. Es corta a propósito: resume lo no negociable y remite a la skill `negocio360-reglas` para el detalle.
3. Se creó `.gitignore` con secretos y claves, compilados de Java y Maven, registros, cobertura, Node, IDE, sistema operativo, temporales de Office y OneDrive, volcados de bases de datos y archivos locales de Claude Code. No ignora `.agent/`, `docs/`, `CLAUDE.md` ni `README.md`.

**Decisiones:** ninguna nueva.

**Observaciones para quien continúe:**
- **No se probó** que la regla se cargue: no se abrió el proyecto en Antigravity. La comprobación está descrita en el README.
- Si la versión del equipo solo lee `.agents/`, hay que renombrar `.agent` a `.agents` y corregir las rutas en README, Bitácora, `CLAUDE.md` y las skills.
- El `.gitignore` es genérico. Ignora `*.db`, `*.sqlite` y `*.bak`: si más adelante hay que versionar un archivo de datos limpios en uno de esos formatos, hay que agregar una excepción (`!ruta/archivo`). Las carpetas de datos del piloto (`datos/crudos/`, `datos/piloto/`) quedaron como líneas comentadas porque la estructura aún no está decidida.
- Un `.gitignore` no afecta archivos que ya se hayan confirmado. No se verificó qué hay confirmado.
- Si cambia una regla de arquitectura o una prohibición, hay que actualizar también `.agent/rules/negocio360.md`.
- Nada se confirmó ni se subió a GitHub.

**Pendiente:**
- Probar en Antigravity que la regla y las skills se cargan, y ajustar rutas si hace falta.
- Revisar el `.gitignore` cuando se defina la estructura de código y de datos.
- Decidir cuándo confirmar y subir los documentos, las skills y la regla.

**Archivos creados:** `.gitignore`, `.agent/rules/negocio360.md`.
**Archivos modificados:** `README.md`, `.agent/skills/README.md`, `CLAUDE.md`, `docs/BITACORA.md`.

### 2026-10-08 — Ramas de trabajo
**Pedido del usuario:** generar las ramas `liz`, `uriel` y `julio`, cada una con una rama de testing, y una rama `pre-main` que sea la última antes de `main`.

**Qué se vio:**
1. Estado al empezar: rama `main` en el único commit `5e20223` («first commit»), con remoto `origin/main`. `README.md` modificado; `.agent/`, `.gitignore`, `CLAUDE.md` y `docs/` sin confirmar.
2. Se crearon **siete ramas locales** desde `5e20223`: `liz`, `liz-testing`, `uriel`, `uriel-testing`, `julio`, `julio-testing` y `pre-main`. No se cambió de rama (se sigue en `main`) y no se tocó ningún archivo.
3. El pedido decía «dos ramas» y luego nombraba tres; se tomaron las tres. «Cada una con una rama de testing» se interpretó como **una rama de testing por persona**, no una compartida. Si se quería una sola rama `testing` compartida, hay que borrar las tres `-testing` y crear `testing`.
4. Las ramas de testing se nombraron con guion (`liz-testing`) porque git no permite a la vez una rama `liz` y otra `liz/testing`.
5. Se documentó el flujo en la sección «Ramas de trabajo» del README.

**Decisiones:** D-31 (aceptada) y D-32 (propuesta).

**Observaciones para quien continúe:**
- **Las ramas son solo locales.** No se ejecutó `git push`. Mientras no se suban, los demás integrantes no las ven.
- **Los archivos nuevos no están en ninguna rama.** `docs/`, `.agent/`, `CLAUDE.md` y `.gitignore` siguen sin confirmar y las siete ramas nacieron antes de ellos. Quien los confirme decide en qué rama quedan; lo razonable es confirmarlos primero y luego actualizar las demás ramas con ese commit.
- El flujo de integración (pull request, quién revisa, protección de ramas) es una propuesta mía; el usuario definió la estructura de ramas pero no esas reglas.
- Ninguna protección de ramas se configuró en GitHub: es una acción en el servicio hacia afuera que decide el equipo.
- La compuerta de seguridad marcó `git branch` como comando destructivo; no lo era, solo crea referencias.

**Pendiente:**
- Decidir en qué rama se confirman primero los archivos nuevos, y hacerlo.
- Decidir si se suben las ramas a GitHub y si se protegen `main` y `pre-main`.
- Confirmar el flujo de integración (D-32).

**Archivos creados:** ninguno. Se crearon siete ramas locales en `.git`.
**Archivos modificados:** `README.md`, `docs/BITACORA.md`.
