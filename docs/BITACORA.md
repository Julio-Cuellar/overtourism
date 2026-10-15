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
| Mapa de Inventario (página publicada) | https://claude.ai/artifact/1M1DNRybgJoXheZmS7JJJt — vista visual de los dos documentos anteriores: mapa de comunicación, filtro de interacciones, secuencias, eventos y explorador de tablas. Desde la versión 2 (2026-10-09) incluye la propuesta de API del panel (GraphQL, WebSocket y REST), las 21 reglas de seguridad y el plan de trabajo. **Versión 0.3 (2026-10-13):** aplica D-55 y D-63 (sin REST de bot), agrega el estado del código tras la etapa I0 de Clientes, un kardex de ejemplo calculado y la sección Scrum con 7 épicas, 36 historias y 6 sprints. **Está compartida con cualquiera que tenga el enlace** (verificado al leerla; antes de esa fecha se había anotado como privada por error). |
| Mapa de Clientes (página publicada, privada) | https://claude.ai/artifact/W6ubG316Laxipgbc4EqAV7 — versión 0.2 (2026-10-13): entidades con diagrama entidad-relación, 38 reglas de dominio, funciones, eventos, tabla de comunicación con la API (GraphQL, WebSocket, REST, Java y bus), plan de consultas, seguridad, tabla de cumplimiento de la LFPDPPP y plan. La versión escrita vive en `docs/diseno/clientes.md` (2026-10-13). Privada: solo la ve quien tenga acceso. |
| Mapa de Identidad (página publicada, privada) | https://claude.ai/artifact/AyFzKR2Rp4cJszZYagsFpa — versión 0.1 (2026-10-13): módulo de autenticación y equipo del personal de los negocios, por REST y API Java: 13 tablas, 45 reglas, 26 endpoints, 25 permisos con matriz por rol, 20 reglas de seguridad, 15 diagramas y el plan en 8 épicas y 36 historias con sprints propuestos. Privada. Aún no hay documento en `docs/diseno/` ni nada en Jira. |
| Mockup de la plataforma (página publicada) | https://claude.ai/artifact/3o4no2gWTCwAkG5tcatffJ — flujos, módulos, pantallas, sección de APIs (GraphQL, WebSocket, REST). Marca como primera opción de stack a Python; **ya está desactualizada en eso** (ver D-03). |
| `docs/mockups/app-turista.html` | Mockup detallado de la app del turista (solo local, abrir en el navegador): teléfono interactivo con 16 pantallas, 75 acciones con su operación, entrada, salida y eventos, mapa de la interfaz (qué pasa al tocar cada botón), catálogo de 14 eventos publicados y 3 consumidos, mapa de comunicación con el bus y hallazgos H-1 a H-15. Operaciones y eventos son PROPUESTA. |
| `docs/diseno/rutas.md` | Diseño del módulo de Rutas v2 (borrador v0.1): reglas de dominio RT-01 a RT-17 con prueba de aceptación, datos, operaciones, eventos E-11 y E-15 a E-17, cómo decide la IA, alcance P1/P2/P3 y preguntas abiertas. |
| `docs/mockups/rutas.html` | Mockup de Rutas v2 (solo local): 9 pantallas, 45 acciones, mapa de la interfaz, «Cómo decide», acciones y eventos, 15 problemas con su solución y alcance y riesgos. Reemplaza S-09 de la app. |
| App del turista (página publicada, privada) | https://claude.ai/artifact/2cTvdAS5Y5pTaSbiS29f67 — copia de `docs/mockups/app-turista.html` al 2026-10-08. Privada: solo la ve quien tenga acceso. Si el archivo del repositorio cambia, hay que volver a publicarla. |
| Rutas del turista (página publicada, privada) | https://claude.ai/artifact/9iQfz3q8Zu77AkKcrRDTgE — copia de `docs/mockups/rutas.html` al 2026-10-08. Privada: solo la ve quien tenga acceso. |
| Mockup original (página publicada) | https://claude.ai/artifact/SgG3tYJ4SVua6qagM6QVM1 — bot, panel de gestión y tablero de gobierno del alcance anterior. |
| `docs/diseno/ventas.md` | Diseño del módulo de Ventas (borrador v0.1, 2026-10-14): reglas VT-01 a VT-13, modelo de datos, `VentasApi`, contrato de `venta.registrada` y `venta.anulada` (D-77), comunicación con la API, flujos y criterios de aceptación. |
| `docs/diseno/caja.md` | Diseño del módulo de Caja (borrador v0.1, 2026-10-14): reglas CJ-01 a CJ-12, turnos, movimientos, corte, `CajaApi`, eventos `caja.*`, flujos y criterios de aceptación. |
| `docs/planes/ventas-caja-v1.md` | Plan mayor de Ventas y Caja (2026-10-14): requisitos, fases F0 a F6, estimación (≈ 9.9 días-persona), calendario (no cabe antes del evento) y preguntas. Esperando confirmación. |
| Ventas y Caja (página publicada, compartida con cualquiera que tenga el enlace) | https://claude.ai/artifact/FVtCcUoJHC8A48rUcJ17xE — **versión 2** (2026-10-14), alineada con `ventas.md`, `caja.md` y el plan: qué se corrigió de la versión 1, decisiones, mapa de módulos y eventos, entidad-relación, entidades, reglas VT y CJ, funciones, códigos de error, eventos con JSON, tres secuencias, estados, tabla de comunicación con la API con filtros, contratos con otros módulos, criterios de aceptación, JavaDoc (D-83), plan y preguntas. La versión 1 (hecha sin leer el repositorio) quedó reemplazada. Copia local: `docs/mockups/ventas-caja.html`. Si cambian los documentos, hay que volver a publicarla. |

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
| D-31 | Estructura de ramas: `liz`, `uriel` y `julio`, cada una con su rama de testing (`liz-testing`, `uriel-testing`, `julio-testing`), y una rama `pre-main` que es la última antes de `main` | ACEPTADA | 2026-10-08 | Dicho por el usuario. Creadas y subidas a GitHub el 2026-10-08, un commit atrás de `main` |
| D-32 | Reglas del flujo de ramas: cada persona trabaja en su rama, prueba en su rama de testing, integra a `pre-main` por pull request revisado por otra persona y de ahí pasa a `main`; `main` y `pre-main` protegidas en GitHub; nombres con guion y no con barra | PROPUESTA | 2026-10-08 | El usuario definió las ramas, no el flujo de integración. Git no permite `liz` y `liz/testing` a la vez |
| D-33 | Los verticales publican eventos genéricos de disponibilidad (`negocio.disponibilidad_cambiada`) definidos en `compartido`; los horizontales (Directorio) los consumen sin conocer giros | PROPUESTA | 2026-10-08 | Resuelve el choque de R4 con «mesas disponibles» |
| D-34 | Canje de cupón: Ventas llama a `Pasaporte.canjearCupon` de forma síncrona (R6); Pasaporte no consume eventos de Ventas | PROPUESTA | 2026-10-08 | Evita un ciclo entre módulos. Requiere acuerdo con el dueño de Ventas y de Caja |
| D-35 | La app del turista no publica ni consume eventos del bus: manda comandos por GraphQL y REST y recibe 5 suscripciones WebSocket. Preparar → confirmar en reseña, sello, cupón y borrado; excepción propuesta para perfil y consentimientos | PROPUESTA | 2026-10-08 | Extiende D-22 y D-23. La excepción choca con la convención de `CLAUDE.md` |
| D-36 | El QR de sello es firmado y rotativo, con vencimiento corto | PROPUESTA | 2026-10-08 | Anti-fraude. Falta diseñar la pantalla del panel que lo muestra |
| D-37 | `negocio_id` opcional en el sobre de eventos de plataforma que no pertenecen a un negocio (`visitante.*`, `ruta.generada`); `visitante_id` es un seudónimo | PROPUESTA | 2026-10-08 | Cambia el §7.1 de `inventario.md` («siempre presente») |
| D-38 | Reseñas consume `sello.otorgado` y guarda sus propias habilitaciones, en vez de llamar a Pasaporte | PROPUESTA | 2026-10-08 | Cambia lo dicho en el plan de funcionalidades de la misma sesión. Consistencia eventual de segundos |
| D-42 | La ficha cambia según el giro: restaurantes ven «Menú», misceláneas ven «Catálogo de productos» (los dos los atiende Catálogo con `catalogoPublico`, antes `menuPublico`) y museos ven «Atracciones» y «Recorridos» (los atiende Museos). El canal compone la ficha; Directorio no conoce a los verticales | PROPUESTA | 2026-10-08 | Pedido del usuario. El canal puede llamar a un vertical porque no es un módulo (R4 solo limita a los módulos) |
| D-43 | Recorrido en solitario: guía por salas en el idioma del perfil, descargable para usar sin conexión y sin ubicación (la persona avanza de sala en sala). El museo entrega español; otros idiomas son opcionales y una traducción automática, si se usa, va rotulada | PROPUESTA | 2026-10-08 | Quién traduce está sin decidir. Detección por QR de sala: por definir |
| D-44 | Recorridos con guía: agenda informativa con personas inscritas y estado (vacío, disponible, casi lleno con 80% o más, lleno). El museo registra asistentes en su panel y Museos publica `museos.cupo_recorrido_actualizado` (E-14). Reservar desde la app queda fuera de alcance | PROPUESTA | 2026-10-08 | Pantalla del panel del museo por diseñar |
| D-39 | Rutas arma el recorrido con un **solver determinista**; el modelo solo interpreta el texto libre y redacta el «por qué», con verificador de cifras y plantilla si falla o no responde | PROPUESTA | 2026-10-08 | Aplica el principio rector. Sin IA la ruta sigue funcionando. Sustituye «el modelo propone el orden» de `CLAUDE.md` |
| D-40 | Ruta viva: modo viaje con reajustes, eventos `ruta.iniciada` (E-15), `ruta.reajustada` (E-16) y `ruta.finalizada` (E-17), suscripción `rutaActiva`, y Rutas consume E-12, E-13 y E-05 | PROPUESTA | 2026-10-08 | Los avisos solo con la app abierta; las notificaciones fuera de la app quedan fuera del MVP |
| D-41 | Equidad: puntaje con la demanda planeada que Rutas ya tiene y la afluencia actual. `ruta.generada` (E-11) se publica al guardar o empezar, no al proponer. Afluencia no consume eventos de Rutas | PROPUESTA | 2026-10-08 | Evita un ciclo entre Rutas y Afluencia. Cambia A-39 y S-09 de la app |
| D-45 | Reglas de seguridad de la API de Inventario (SG-01 a SG-21), alineadas con la guía de OWASP para GraphQL y la lista OWASP API Security Top 10 2023: autenticación en HTTP y WebSocket, `negocio_id` solo del contexto, autorización en cada resolvedor, límites de profundidad, costo, alias, tiempo y tasa, esquema y errores cerrados, HTTP estricto, WebSocket con verificación de origen y aislamiento de eventos por negocio. Nueve son imprescindibles (SG-01 a SG-05, SG-12, SG-13, SG-14 y SG-17) | PROPUESTA | 2026-10-09 | Es alineación, no certificación. Los valores numéricos son iniciales. SG-14, SG-15, SG-16 y SG-20 son un complemento mío porque la guía no trata CSRF, WebSocket ni registros. Detalle en el mapa publicado y, cuando se apruebe, en `docs/diseno/inventario-graphql.md` |
| D-46 | Plan de API del panel de Inventario: GraphQL con dos mutaciones (`prepararComando`, `confirmarComando`), 10 consultas y 2 suscripciones (`alertasNegocio` con las 8 clases y `saldoActualizado`); REST solo para el bot y solo si continúa; sin herramientas de Inventario para la IA en el MVP. Agrega a `InventarioApi` las operaciones `unidades` y `resumen` y mueve las operaciones de recetas al esquema de Restaurantes | PROPUESTA | 2026-10-09 | Ajustes API-1 a API-4 del mapa. La autenticación de la plataforma sigue sin definir (dependencia). Preguntas abiertas: forma de la entrada de comandos, extensiones, si continúa el bot y cómo viaja la identidad |
| D-47 | Clientes es un módulo horizontal con esquema `clientes`. Es dueño de los datos del cliente, sus consentimientos, el aviso de privacidad, las solicitudes ARCO y el resumen de compras. No guarda ventas, cupones, reseñas ni el perfil del turista | PROPUESTA | 2026-10-13 | Mapa de Clientes v0.2 |
| D-48 | Ventas guarda `cliente_id` como referencia opaca y lo agrega como campo opcional a `venta.registrada` v1 (R5). Ventas no llama a Clientes (evita un ciclo). El canal valida con `sugerirClientes` antes de vender | ACEPTADA (delegada) | 2026-10-13 | El usuario respondió «como veas más conveniente». Hay que congelarlo con el dueño de Ventas |
| D-49 | El resumen de compras de un cliente se calcula solo con eventos de Ventas, con `compra_aplicada` para idempotencia y `ANULADO_PREVIO` | ACEPTADA (delegada) | 2026-10-13 | Mismo patrón que D-18 y D-19 |
| D-50 | Solo se anonimiza, no se borra: irreversible, con preparar y confirmar, y publica `cliente.anonimizado` | ACEPTADA | 2026-10-13 | Dicho por el usuario. Ver C-9 del mapa: la ley prevé un periodo de bloqueo (arts. 2 III y 24) |
| D-51 | Consentimiento solo de inserción, por finalidad, con forma, medio y versión del aviso | PROPUESTA | 2026-10-13 | |
| D-52 | Teléfono, correo y fecha de nacimiento enmascarados u ocultos en listas. La ficha completa se audita en `acceso_cliente` | PROPUESTA | 2026-10-13 | |
| D-53 | Un turista solo es cliente de un negocio si lo autoriza de forma explícita (P2). El evento `visitante.perfil_compartido` está por definir con Visitantes | ACEPTADA (delegada) | 2026-10-13 | El usuario respondió «como veas más conveniente» |
| D-54 | La IA nunca recibe la lista de clientes: el modelo extrae el texto del nombre, el código busca y la persona elige | PROPUESTA | 2026-10-13 | |
| D-55 | `prepararComando` y `confirmarComando` son un par de plataforma; cada módulo agrega variantes a `ComandoInput` | PROPUESTA | 2026-10-13 | Corrige D-46: dos módulos no pueden definir la misma mutación. Falta actualizar el plan de Inventario |
| D-56 | Sin suscripciones WebSocket de Clientes en P1; la candidata P2 es `alertasClientes` | PROPUESTA | 2026-10-13 | |
| D-57 | Clientes entra a la demo. Su corte P1: alta, edición, búsqueda, sugerencias, resumen, consentimiento, anonimización, aviso de privacidad y ARCO. P2: etiquetas, vínculo con visitante, alerta de inactividad. P3: fusionar | ACEPTADA | 2026-10-13 | Dicho por el usuario. El detalle de P1 es propuesta mía. No hay autorización escrita para programar |
| D-58 | Clientes se diseña para cumplir la LFPDPPP desde el principio. El negocio es el responsable y la plataforma es persona encargada | ACEPTADA | 2026-10-13 | Dicho por el usuario. Texto de la ley leído en diputados.gob.mx (última reforma DOF 14-11-2025); no se revisó el Reglamento y no es asesoría jurídica |
| D-59 | Datos del cliente: nombre, apellidos, teléfono, correo, fecha de nacimiento y notas. Domicilio y RFC fuera hasta que tengan una finalidad en el aviso. Sin datos sensibles | PROPUESTA | 2026-10-13 | El usuario pidió «datos completos»; la lista concreta es mi interpretación (pregunta abierta 1) |
| D-60 | Contactar por mensajería y enviar promociones exige consentimiento expreso, aunque el art. 7 admite el tácito por regla general. El consentimiento verbal cuenta como expreso, con constancia | PROPUESTA | 2026-10-13 | Es más estricto que el mínimo legal |
| D-61 | Los derechos ARCO se atienden dentro del producto: tabla `solicitud_arco`, plazos de 20 y 15 días hábiles (art. 31), ampliables una vez. Acceso = exportación; rectificación = editar; cancelación = anonimizar; oposición = revocar consentimiento o anonimizar | PROPUESTA | 2026-10-13 | Falta un calendario de días inhábiles (C-10) |
| D-62 | Cada negocio tiene un aviso de privacidad versionado y no puede registrar clientes sin uno vigente | PROPUESTA | 2026-10-13 | Falta quién redacta el aviso y el contrato de encargo |
| D-63 | El bot de chat no continúa por ahora. Se eliminan las vías REST del bot y el origen `BOT` | ACEPTADA | 2026-10-13 | Dicho por el usuario. Cierra la decisión abierta sobre el bot. Afecta también el REST del bot de Inventario (D-46) |
| D-64 | Umbrales iniciales de Clientes: inactivo a los 60 días sin comprar; frecuente con 3 compras en 90 días. Son reglas versionadas | ACEPTADA (delegada) | 2026-10-13 | El usuario respondió «cómo veas conveniente» |
| D-65 | El `total` de `venta.registrada` es neto del cupón cuando aplica, y el total completo cuando no hay cupón | ACEPTADA | 2026-10-13 | Dicho por el usuario («sí, solo cuando aplica»). Falta congelarlo con el dueño de Ventas |
| D-76 | Ventas y Caja son dos módulos horizontales separados (`ventas`, `caja`). Ventas no conoce a Caja: el canal obtiene el turno con `CajaApi.turnoActivo` y pasa el `turno_id`. Caja escucha `venta.*` y llama a `VentasApi.resumenTurno` en el corte | PROPUESTA | 2026-10-14 | Evita un ciclo. `docs/diseno/ventas.md` y `caja.md`. La separación la pidió el usuario; el reparto de dependencias es mío |
| D-77 | Contrato de `venta.registrada` v1 (`venta_id`, `folio`, `serie`, `turno_id`, `cliente_id?`, `cupon_id?`, `subtotal`, `descuento`, `total`, `lineas[]`, `pagos[]`) y `venta.anulada` v1 (`venta_id`, `turno_id`, `motivo?`, `lineas[]`, `pagos[]`) | PROPUESTA | 2026-10-14 | Une lo que piden Inventario (§7.2, D-25), Clientes (D-48, D-65) y Caja. Compatible con `VentaRegistradaV1` y `VentaAnuladaV1` de Clientes (lector tolerante). Falta acordarlo con quien programa Inventario y Clientes |
| D-78 | Cupón: Ventas lo cotiza con `PasaporteApi.cotizarCupon` (nuevo, solo lectura) al preparar y lo canjea con `canjearCupon` al confirmar, en la misma transacción | PROPUESTA | 2026-10-14 | Precisa D-34. Revertir el canje al anular está abierto (VT-10) |
| D-79 | El folio de venta es consecutivo por negocio y serie y se asigna al confirmar | PROPUESTA | 2026-10-14 | Sin huecos por preparaciones que expiran |
| D-80 | Ventas no conoce boletos ni venta a granel: la unidad y si un producto admite fracción vienen de Catálogo; lo propio del museo vive en Museos | PROPUESTA | 2026-10-14 | Cumple R4 |
| D-81 | Preparar el corte de caja pone el turno `EN_CORTE` (única preparación que cambia un estado); descartar o expirar lo regresa a `ABIERTO` | PROPUESTA | 2026-10-14 | Excepción a «preparar no escribe» de Inventario §6.2, para que no entren ventas entre el conteo y la confirmación |
| D-82 | Anular una venta usa preparar → confirmar, con motivo y permiso de Identidad; una venta registrada no se edita | PROPUESTA | 2026-10-14 | Quién puede anular y hasta cuándo está abierto |
| D-83 | Todo el código se documenta con **JavaDoc**: cada tipo y método público, con `@param`, `@return`, fallos (`@throws` o código de error), el ID de función y las reglas que aplica (`VT-04`, `CJ-07`), y escala y redondeo en todo monto. `mvn javadoc:javadoc` sin advertencias entra en la verificación | ACEPTADA | 2026-10-14 | Dicho por el usuario: «el codigo debe de ser documentado con JavaDoc para no batallar a la hora de encontrar errores». Falta reflejarlo en la skill `negocio360-reglas` (§7) si el equipo lo pide |

### Decisiones de producto abiertas (vienen de la sesión de mockups)
| Tema | Opciones | Estado |
|---|---|---|
| Modelo de IA para rutas | Gemini o ChatGPT | ABIERTA |
| Bot de chat (Telegram o WhatsApp) para locatarios | Se mantiene o se descarta | DESCARTADO por ahora (D-63, 2026-10-13) |
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
| Repositorio | Carpeta `C:\Users\Julio\OneDrive\Documentos\BUAP\Hackathon_Turismo_puebla`. **Ya es un repositorio git** (actualizado el 2026-10-08): tiene `.git`, rama `main`, remoto `origin` en GitHub (`Julio-Cuellar/overtourism`) y, al 2026-10-08, dos commits: `5e20223` («first commit») y `7de219d` («skills e inicio del proyecto»), que subió alguien del equipo e incluye documentos, skills, regla permanente y `.gitignore`. Hay además siete ramas de trabajo (ver D-31). Está dentro de OneDrive (puede bloquear archivos de compilación y conviene excluir `target/`; tener `.git` dentro de OneDrive puede generar conflictos de sincronización) |
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

### 2026-10-08 — Ramas subidas a GitHub
**Pedido del usuario:** subir las ramas al repositorio remoto.

**Qué se vio:**
1. Se ejecutó `git push -u origin` con las siete ramas (`liz`, `liz-testing`, `uriel`, `uriel-testing`, `julio`, `julio-testing`, `pre-main`). Todas se crearon en `origin` y quedaron con seguimiento hacia su copia remota. No se subió `main`, no se usó `--force` y no se subió ningún commit nuevo. Se verificó con `git ls-remote --heads origin`.
2. **Hallazgo:** `main` en GitHub estaba en el commit `7de219d` («skills e inicio del proyecto»), posterior a `5e20223`. Alguien del equipo confirmó y subió a `main` los documentos, las skills, la regla permanente, `CLAUDE.md`, `README.md` y `.gitignore` mientras se creaban las ramas. Se hizo `git fetch` para verlo; el `main` local ya coincidía con `origin/main`.
3. Se comparó el contenido: los 31 archivos de ese commit son **idénticos** a los de la carpeta de trabajo y no hay cambios pendientes.
4. **Consecuencia:** las siete ramas apuntan a `5e20223` y están **un commit atrás de `main`**. No contienen `docs/`, `.agent/`, `CLAUDE.md` ni `.gitignore`. Quien cree su trabajo en una de ellas empezaría sin el diseño ni las skills.

**Decisiones:** ninguna nueva.

**Observaciones para quien continúe:**
- Se corrigió la nota anterior de «archivos sin confirmar»: ya están confirmados en `main`.
- No se actualizaron las ramas para que incluyan `7de219d`. Hacerlo implica mover las siete ramas en el remoto (avance sin `--force`), y se dejó para que el equipo lo decida.
- La compuerta de seguridad marcó como destructivos comandos que solo leen, y rechazó un reintento en el que se había cambiado el texto de una etiqueta; hay que repetir el comando idéntico.
- Siguen sin configurarse protecciones de rama en GitHub.

**Pendiente:**
- Decidir si se llevan las siete ramas a `7de219d` (recomendado) o se dejan en `5e20223` y cada quien hace `git merge main`.
- Proteger `main` y `pre-main` en GitHub.
- Confirmar el flujo de integración (D-32).

**Archivos creados:** ninguno.
**Archivos modificados:** `README.md`, `docs/BITACORA.md`.

### 2026-10-08 — Mockup detallado de la app del turista
**Pedido del usuario:** un mockup detallado de la UI/UX de la app, con la comunicación entre la app y los demás módulos, qué eventos publica y consume, qué información transporta, un mapa de la interfaz («si picamos un botón, pasa tal acción») y lo mismo para cada acción y evento.

**Qué se vio:**
1. Se creó `docs/mockups/app-turista.html` (un solo archivo, se abre en el navegador). Tiene seis pestañas: **Prototipo** (teléfono con 14 pantallas y selector de estados; al tocar un botón, un inspector muestra recorrido, entrada, salida, errores y eventos, más un registro de la sesión), **Mapa de la interfaz** (tarjeta por pantalla con «botón → acción → destino» y el camino dorado propuesto), **Acciones** (66, filtrables), **Eventos** (13 publicados, 3 consumidos, 4 suscripciones), **Comunicación** (diagrama con el bus, tabla de 14 llamadas síncronas y qué ve el locatario) y **Hallazgos**.
2. **Respuesta a «qué publica y consume la app»:** nada del bus. La app manda comandos por GraphQL y REST, y recibe 4 suscripciones WebSocket (mesas, afluencia, cupón, respuestas a reseñas). Los eventos los publican los módulos de plataforma: visitantes (3), pasaporte (5), reseñas (2), rutas (1), directorio (1) y afluencia (1).
3. Al dibujarlo aparecieron diez hallazgos (H-1 a H-10). Los más importantes: el sobre exige `negocio_id` pero cinco eventos no pertenecen a un negocio (D-37); «mesas disponibles» chocaba con R4 (D-33); Ventas y Pasaporte formarían un ciclo (D-34); el canje lo hace quien cobra, no el turista, y falta esa pantalla en Caja (H-7); un QR de sello fijo se puede reutilizar (D-36).
4. **Cambio respecto al plan de funcionalidades de la misma sesión:** allí Reseñas llamaba a Pasaporte; ahora consume `sello.otorgado` y guarda sus habilitaciones (D-38). Cuesta consistencia eventual de segundos.

**Decisiones:** D-33 a D-38, todas PROPUESTA (sección 4). El plan de funcionalidades F-01 a F-16 y los módulos nuevos (directorio, visitantes, reseñas, pasaporte, rutas, transporte, afluencia, gobierno) tampoco están aceptados: el usuario no los confirmó antes de pedir el mockup.

**Verificación hecha:** se abrió en Chrome (la consola quedó sin errores), se recorrió con scripts el camino dorado completo (cuestionario, ficha, sello, pasaporte, cupón, canje), se revisaron las seis pestañas, y a 400 px no hay desplazamiento horizontal de la página. Las cuentas de ejemplo cuadran: cuenta $230.00 − cupón 10% $23.00 = $207.00; ruta 60.00 + 180.00 + 40.00 = $280.00 y 90 + 15 + 75 + 9 + 30 = 219 min.

**No verificado:** navegación con teclado de todo el mockup, el tema claro en cada pestaña, impresión, y que los textos coincidan con el esquema GraphQL (no existe). Un error de consola en la primera versión (`top` chocaba con `window.top`) se corrigió.

**Observaciones para quien continúe:**
- Los nombres de operaciones, campos y códigos de error son propuestas. Cuando haya esquema GraphQL, hay que reconciliarlos.
- Hay que cambiar el §7.1 de `docs/diseno/inventario.md` si se acepta D-37.
- Los datos de transporte son ilustrativos y están rotulados como estimados; el contrato de `comoLlegar` depende de la sesión de transporte.
- No se publicó como página externa; está solo en el repositorio. No se creó ni modificó código del producto.
- El archivo se confirma con git solo si alguien lo decide (aparece como archivo nuevo sin seguimiento).

**Pendiente:**
- Que el equipo acepte o rechace F-01 a F-16, los módulos nuevos y D-33 a D-38.
- Congelar con los dueños de Catálogo (`menuPublico`), Ventas y Caja (`canjearCupon`, `venta.registrada`).
- Diseñar las pantallas del panel del locatario que faltan (QR de sello rotativo, escanear cupón en caja, responder reseñas).
- Escribir `docs/diseno/app-turista.md` con reglas de dominio y una prueba de aceptación por regla.

**Archivos creados:** `docs/mockups/app-turista.html`.
**Archivos modificados:** `docs/BITACORA.md` (tabla de documentos, decisiones D-33 a D-38 y esta entrada).

### 2026-10-08 — Ficha por giro: catálogo de productos, atracciones y recorridos de museo
**Pedido del usuario:** solo los negocios de comida llevan «Menú»; las misceláneas llevan un «Catálogo de productos» consultable desde la app; los museos llevan «Historia» o «Atracciones» (qué hay: pinturas, objetos antiguos, escritos) y una sección «Recorridos» con «Recorrido en solitario» (guía en el idioma del turista para explorar dentro del museo) y «Con guía» (agenda de recorridos guiados, personas que lo recorren y si está lleno o vacío).

**Qué se hizo en `docs/mockups/app-turista.html`:**
1. La ficha cambia por giro: restaurante (Información, Menú, Reseñas), miscelánea (Información, Catálogo, Reseñas) y museo (Información, Atracciones, Recorridos, Reseñas). El museo muestra «Aforo» en vivo y la miscelánea «Atención» en vivo (tipos AFORO y ATENCION del evento C-01).
2. Dos pantallas nuevas: S-15 Recorrido en solitario (4 salas, español e inglés de ejemplo, descarga sin conexión) y S-16 Recorridos con guía (agenda de hoy y mañana, con 12 de 15, 0 de 15, 15 de 15 y 5 de 15 personas, y su estado).
3. Acciones nuevas A-67 a A-75 (de 66 a 75); A-56 pasó a ser `establecerIdioma`. Evento nuevo E-14 `museos.cupo_recorrido_actualizado` (de 13 a 14 publicados), suscripción nueva SUB-5 (de 4 a 5) y llamada síncrona L-15 del canal a Museos. Hallazgos H-11 a H-15.
4. `menuPublico` se renombró a `catalogoPublico`, porque ahora atiende menú y catálogo.
5. Se corrigió un texto del mockup que decía «7 módulos» publican eventos; eran 6 (ahora 7 con Museos y se calcula solo).

**Decisiones:** D-42, D-43 y D-44 (PROPUESTA). D-35 pasa de 4 a 5 suscripciones.

**Observaciones para quien continúe:**
- «Historia» y «Atracciones» son una sola pestaña llamada «Atracciones» que abre con la historia del museo. Si el equipo prefiere otro nombre, es un cambio de texto.
- La función «Recorridos de museo» recibe el ID F-17 en el catálogo de funcionalidades que solo existe en la conversación (F-01 a F-16 no están registrados en ningún documento).
- Los D-39 a D-41 que se propusieron en la conversación para Rutas v2 **no se registraron** aquí: el usuario no los ha respondido.
- La cuenta de personas la debe capturar el museo; sin esa captura la agenda no sirve. Hace falta una pantalla del panel del museo, sin diseñar.
- Las traducciones y el contenido de salas del ejemplo son inventados. El museo y sus datos son ficticios.

**Verificación hecha:** se abrió en Chrome, la consola quedó sin errores; con scripts se recorrieron los tres giros (pestañas correctas en cada uno, catálogo de Dulces Doña Lupe sin platillos, atracciones, solitario en español e inglés, descarga, agenda con los cuatro estados y cambio de estado al simular que una persona se une). Se corrigió un recorte de la fila de pestañas. Las cuentas cuadran: 12 + 30 + 8 + 15 = 65 piezas; 12 de 15 = 80% (casi lleno).

**No verificado:** el teclado y el tema claro de S-15 y S-16. A 400 px se revisó que S-07 (museo), S-15, S-16 y las seis pestañas no desborden.

**Pendiente:**
- Aceptar o ajustar D-42 a D-44 y decidir quién traduce la guía.
- Definir con el dueño de Catálogo el contrato `catalogoPublico` y con el de Museos `atracciones`, `recorridoSolitario`, `paqueteGuia` y `recorridosGuiados`.
- Diseñar las pantallas del panel del museo (registrar asistentes y publicar la guía).
- Decidir si reservar un lugar en un recorrido entra algún día.

**Archivos modificados:** `docs/mockups/app-turista.html`, `docs/BITACORA.md` (cifras del mockup, D-35, D-42 a D-44 y esta entrada).

### 2026-10-08 — Rutas v2: mockup, diseño y páginas publicadas
**Pedido del usuario:** «sí, hazlo, y utiliza la herramienta de artifacts de claude para generar los mockups y poder compartirlos», sobre el plan de Rutas v2 (cómo se vería completa la parte de rutas, mejorada para dar mejor experiencia y resolver problemas habituales del rubro).

**Qué se hizo:**
1. `docs/mockups/rutas.html`: teléfono interactivo con 9 pantallas (plantear, elegir estilo, editar, alternativas, revisar y guardar, en ruta, aviso de lugar lleno, cierre y mis rutas), selector de estados (incluye «Sin IA» y «Sin conexión»), inspector con la operación, la entrada y salida y los eventos de cada botón, y cinco pestañas más: mapa de la interfaz con el ciclo de vida de una ruta, «Cómo decide», acciones y eventos, 15 problemas con su solución, y alcance y riesgos. El servidor del ejemplo calcula en vivo horas, costo, caminata y avisos; al editar se ve qué cabe.
2. `docs/diseno/rutas.md`: reglas de dominio RT-01 a RT-17 con una prueba de aceptación por regla, datos (esquema `rutas`), 16 operaciones, eventos E-11 y E-15 a E-17, pipeline de IA, alcance P1/P2/P3 y preguntas abiertas.
3. Se publicaron **dos páginas privadas** con la herramienta de artifacts: Rutas del turista y App del turista (enlaces en la tabla de documentos). Quien tenga el enlace no puede abrirlas hasta que el dueño las comparta desde el menú de la página.
4. Se corrigió una incongruencia entre mockups: Dulces Doña Lupe aparecía «cerrado» todo el día en la app, pero la ruta la visita a las 14:09. Ahora abre a las 10:00 y el teléfono marca las 9:41.

**Decisiones:** D-39, D-40 y D-41 (PROPUESTA). Resumen: el orden de las paradas lo calcula un solver determinista y la IA solo interpreta y redacta; la ruta es «viva» con modo viaje; la equidad usa la demanda planeada que Rutas ya tiene, y Afluencia no consume eventos de Rutas.

**Hallazgos nuevos:** un ciclo posible entre Rutas y Afluencia (resuelto con D-41); en la app, A-39 publicaba E-11 al proponer y contaría rutas que nadie va a hacer; no todos los lugares tienen sello, por eso existe «marcar como hecha»; «sin gluten» y «caminar poco» necesitan etiquetas que Directorio no tiene.

**Verificación hecha:** se abrió en Chrome; con scripts se recorrió el flujo completo (brief, variantes, editar, alternativas, agregar, guardar, compartir, empezar, sellar, simular que la fonda se llena, cambiar parada, terminar, mis rutas); se revisaron las seis pestañas; a 400 px ninguna pestaña ni pantalla desborda. Las cuentas del ejemplo cuadran: ruta A = 60.00 + 180.00 + 40.00 = $280.00 y 90 + 15 + 75 + 9 + 30 = 219 min; B = $190.00 y 202 min; C = $215.00 y 169 min. Las pruebas RT-02, RT-03, RT-07 y RT-13 del documento se comprobaron contra el mockup. La consola solo mostró un aviso del navegador de pruebas sobre URL `file:`, no de la página.

**No verificado:** las páginas publicadas no se abrieron en el visor de claude.ai (se derivaron quitando el envoltorio HTML; las fuentes vienen de Google Fonts, que el visor permite). Tampoco se probó el teclado ni el tema claro de las pantallas nuevas, ni el aviso de lugar lleno con otras variantes que no sean la A.

**Observaciones para quien continúe:**
- `docs/mockups/app-turista.html` todavía tiene la pantalla S-09 y las acciones A-39 a A-41 de la v1. Hay que enlazarlas con Rutas v2 o reemplazarlas (HR-2).
- Los textos de «por qué», las duraciones, costos, horarios y traslados son inventados o estimados. Los lugares «(ejemplo)» son ficticios.
- Los pesos de las variantes son cualitativos y están por calibrar; la lista de problemas sale de lo observado y de problemas conocidos del rubro, sin investigación de mercado.
- El corte P1 es una propuesta mía; el equipo no ha dicho su disponibilidad real.
- Las copias publicadas se desactualizan si cambian los archivos del repositorio.

**Pendiente:**
- Aceptar o ajustar D-39 a D-41 y el corte P1.
- Definir con el dueño de Directorio los campos de duración, costo, dieta y accesibilidad, y con el de Transporte el contrato de `estimarTraslado`.
- Ficha del componente de IA con evaluación y casos fijos del solver (skill `integrar-ia`).
- Actualizar S-09 de la app y registrar D-39 a D-41 en `CLAUDE.md` (donde dice que el modelo elige entre los negocios).

**Archivos creados:** `docs/mockups/rutas.html`, `docs/diseno/rutas.md`.
**Archivos modificados:** `docs/mockups/app-turista.html` (horario de Dulces Doña Lupe), `docs/BITACORA.md` (tabla de documentos, D-39 a D-41 y esta entrada).

### 2026-10-09 — Plan de API de Inventario (GraphQL, WebSocket, REST) y reglas de seguridad
**Pedido del usuario:** planificar con `/ecc:plan` las consultas de GraphQL del módulo de Inventario con su flujo, los WebSockets y el REST si se requiere; luego, agregar las reglas de seguridad de la información y contra las vulnerabilidades propias de GraphQL; y por último, «continúa agregando todo esto en el artefacto».

**Qué se vio:**
1. **Plan v1 y v2** presentados en conversación (no hay archivo de plan). Contenido: 10 consultas, 2 mutaciones (`prepararComando` y `confirmarComando`, que cubren los 10 comandos), 2 suscripciones, REST solo para el bot si continúa, cuatro flujos, fases de diseño G1 a G6 (≈ 13 h) y de implementación I0 a I5 (≈ 3.25 d-p sin REST, ≈ 3.75 con REST), riesgos y cuatro preguntas.
2. **Cuatro hallazgos del diseño existente** (API-1 a API-4 en el mapa): la sección 9 de `inventario.md` pone operaciones de Restaurantes dentro del GraphQL de Inventario (rompe R1); faltan `unidades()` y `resumen()` en `InventarioApi`; `alertasNegocio` listaba 7 eventos y deben ser 8, y no basta para ver el saldo moverse; la autenticación de la plataforma no está definida.
3. **Seguridad:** 21 reglas (SG-01 a SG-21) basadas en la guía de OWASP para GraphQL y en OWASP API Security Top 10 2023, con su control, cómo se comprueba y cobertura de las diez categorías. Se consultaron las dos páginas de OWASP. Esa guía no trata CSRF, WebSocket ni registros: esas reglas son complemento mío.
4. **Se actualizó el artefacto «Mapa de Inventario»** a la versión 2 con tres secciones nuevas (API del panel con 4 flujos F1 a F4 y un filtro de operaciones, Seguridad con las 21 reglas filtrables y la cobertura OWASP, y Plan de trabajo con fases, ajustes y riesgos). Se partió de la versión publicada, que coincidía con mi archivo fuente.
5. **Corrección:** el artefacto está compartido con cualquiera que tenga el enlace, y la Bitácora decía que era privado. Se corrigió la fila de documentos.

**Decisiones:** D-45 y D-46 (PROPUESTA).

**Verificación hecha:** se comprobaron por búsqueda en el archivo los identificadores de las secciones nuevas, de los contenedores y de las llamadas de render, y que los diagramas no contuvieran punto y coma en mensajes ni saltos de línea de HTML.

**No verificado:** la página no se abrió en un navegador ni en el visor de claude.ai: no sé si los cuatro diagramas nuevos se dibujan ni cómo se ve a 400 px o en tema oscuro. Los valores numéricos de los límites, las horas y los días-persona son heurísticas mías. No se comprobó qué soporta la versión de Spring for GraphQL que se elija (entrada de una sola opción, consultas persistidas, cierre de sugerencias de campos, verificación del origen del WebSocket).

**Observaciones para quien continúe:**
- **El plan no está aprobado.** El usuario pidió reflejarlo en el artefacto, lo que se interpretó como autorización para documentarlo y no para implementar. Todo está marcado como propuesta.
- **Aún no existe** `docs/diseno/inventario-graphql.md` ni los ajustes a `inventario.md` §6 y §9. Quedan para cuando se confirme el plan.
- Las otras dos páginas publicadas (App del turista y Rutas del turista) figuran como privadas en esta Bitácora; no se verificó su estado de uso compartido, y la de Inventario resultó estar abierta por enlace.
- Dos sesiones trabajan en este repositorio. Esta sesión solo agregó entradas al final de la Bitácora y no tocó los archivos de la otra.
- El marco legal mexicano de protección de datos personales no se verificó; queda como pendiente de la plataforma.

**Pendiente:**
- Confirmar el plan y contestar las cuatro preguntas: forma de la entrada de comandos, extensiones (`saldoActualizado`, `resumenInventario`, `unidades`), si el bot continúa y cómo viaja la identidad.
- Definir la autenticación de la plataforma, de la que dependen SG-01 a SG-03.
- Abrir el artefacto en un navegador y corregir lo que falle.

**Archivos creados:** ninguno en el repositorio.
**Archivos modificados:** `docs/BITACORA.md` (fila del Mapa de Inventario, D-45, D-46 y esta entrada). Se republicó el artefacto «Mapa de Inventario» (versión 2); su archivo fuente está en la carpeta temporal de la sesión original, fuera del repositorio.

### 2026-10-13 — Módulo de Clientes: arquitectura, respuestas del equipo y LFPDPPP
**Pedido del usuario:** «generaremos la arquitectura del módulo de clientes» con `/ecc:plan`; luego, entregar entidades, funciones, eventos y el plan de consultas a la API en un artefacto, con diagramas entidad-relación y de eventos, tablas por sección y una tabla de comunicación con la API que diga si es WebSocket, GraphQL o REST. Después contestó las 9 preguntas del plan y pidió abordar todo con la LFPDPPP desde el principio. También pidió ignorar el paquete de skills de `.agent/skills/` y usar solo las de `ecc:` (guardado en la memoria del asistente, no en el repositorio).

**Qué se hizo:**
1. **Mapa de Clientes** publicado como artefacto privado (ver la tabla de documentos). Versión 0.2: 12 tablas (9 en P1), 38 reglas de dominio con prueba de aceptación, 23 funciones de `ClientesApi`, 4 eventos publicados y 4 consumidos, 23 vías de comunicación (10 GraphQL, 1 WebSocket, 1 REST, 3 llamadas Java y 8 eventos del bus), 12 diagramas, reglas de seguridad (21 heredadas de Inventario y 12 propias) y una tabla de cumplimiento con los artículos de la ley.
2. **Respuestas aplicadas:** Clientes entra a la demo (D-57); Ventas guarda `cliente_id` opaco (D-48, D-49); el turista solo con autorización explícita (D-53); solo se anonimiza (D-50); datos completos (D-59); total neto de cupón cuando aplica (D-65); umbrales 60 días y 3 compras en 90 (D-64); el bot se descarta por ahora (D-63); LFPDPPP desde el inicio (D-58).
3. **Lectura de la ley.** Se descargó el texto consolidado de la Cámara de Diputados (última reforma DOF 14-11-2025) y se leyeron los artículos 2, 5 a 20 y 22 a 36 y las sanciones. Lo que cambió el diseño: el negocio es el responsable y la plataforma es persona encargada; los plazos ARCO son 20 y 15 días **hábiles**, ampliables una vez (arts. 2 VIII y 31); el acceso exige acreditar la identidad y es gratuito; la cancelación prevé un periodo de bloqueo antes de suprimir (arts. 2 III y 24); las alergias en notas serían datos sensibles (art. 8). De ahí salieron las tablas `aviso_privacidad` y `solicitud_arco`, las reglas L-01 a L-09 y los comandos de aviso y ARCO.
4. **Hallazgos C-1 a C-11**, entre ellos: `prepararComando` y `confirmarComando` no pueden ser de un solo módulo (corrige D-46), y `preparacion.comando` guarda datos personales mientras vive el token.

**Decisiones:** D-47 a D-65 (sección 4). Aceptadas: D-50, D-57, D-58, D-63, D-65. Aceptadas por delegación («como veas más conveniente»): D-48, D-49, D-53, D-64. El resto, propuesta.

**Verificación hecha:** los 12 diagramas pasan el analizador de Mermaid (versión 11.4.1, en Node); el script de la página no tiene errores de sintaxis; las cifras de la página se calculan de los mismos datos. Las cuentas de ejemplo cuadran: 100.00 + 50.50 + 0.01 = 150.51, ticket promedio 50.17; veinte días hábiles desde el lunes 2026-10-05 caen el 2026-11-02. Se leyeron los artículos de la ley en el texto oficial, no en resúmenes.

**No verificado:** la página no se abrió renderizada ni en el visor de claude.ai, ni a 400 px, ni en tema oscuro. No se revisó el Reglamento de la ley ni lineamientos de la Secretaría Anticorrupción y Buen Gobierno. Una fuente secundaria afirma que hay obligaciones reforzadas para menores de edad y el texto leído no las menciona (C-11). Esto no es asesoría jurídica. Los límites, plazos internos y días-persona son heurísticas mías.

**Observaciones para quien continúe:**
- **No hay autorización escrita para programar.** El usuario dijo que Clientes entra a la demo, pero el `CLAUDE.md` exige la autorización escrita en esta Bitácora y no se ha dado. Sigue la fase de diseño.
- P1 suma 4.25 días-persona (heurística) y la demo es el 15 de octubre. Hay que decidir qué corre en vivo y qué se muestra como mockup. La demo debe usar datos sintéticos (SC-11).
- D-63 (sin bot) afecta al plan de Inventario: el REST del bot de D-46 deja de aplicar.
- Ya existe `docs/diseno/clientes.md` (2026-10-13), con el diseño completo y los diagramas en Mermaid dentro del mismo archivo. No se creó un `clientes-diagramas.md` aparte. El archivo fuente del artefacto sigue en la carpeta temporal de la sesión, fuera del repositorio. En ese documento las preguntas abiertas se renumeraron (la de la autorización ya está contestada), así que la pregunta 8 del artefacto es la 7 del documento.
- Contradicción con el plan anterior: el Mapa de Clientes v0.1 (mismo día) decía que el bot seguía abierto y que 6 preguntas esperaban respuesta. La v0.2 lo reemplaza.

**Pendiente:**
- Contestar las 9 preguntas abiertas del mapa: si «datos completos» incluye domicilio o RFC y con qué finalidad, quién redacta el aviso de privacidad y el contrato de encargo, quién es la persona de datos personales, si se aceptan menores, si basta anonimizar sin periodo de bloqueo, quién mantiene el calendario de días inhábiles, la autorización para programar, qué corre en vivo en la demo y dónde vive la base de datos (D-16).
- Congelar con el dueño de Ventas `cliente_id` y el total neto de cupón.
- Actualizar el plan de Inventario por D-55 y D-63.
- Verificar la ley con una persona con formación jurídica antes del piloto con datos reales.

**Archivos creados:** ninguno en el repositorio.
**Archivos modificados:** `docs/BITACORA.md` (fila del Mapa de Clientes, D-47 a D-65, el estado del bot de chat y esta entrada).

### 2026-10-13 — Autorización para programar el módulo de Clientes
**Autorización escrita (cita textual del usuario en la sesión):** «autorizo programar Clientes».

**Alcance de la autorización:** solo el módulo de Clientes, siguiendo el Mapa de Clientes v0.2 y sus decisiones. No cubre Inventario ni los demás módulos, y no cambia el resto de las reglas de `CLAUDE.md` (principio rector, R1 a R6, preparar y confirmar, pruebas primero). Sigue vigente la regla de no introducir dependencias ni funciones fuera del alcance del MVP sin preguntar.

**Cómo se trabaja:** en la rama personal `julio` (D-31), con TDD (primero la prueba, luego el código) y sin commits ni pushes hasta que el usuario los pida. `julio` estaba dos commits atrás de `main` y sin commits propios, así que se adelantó a `main` sin perder nada.

**Primera etapa (I0 del Mapa de Clientes):** esqueleto Maven mínimo (Java 21, Spring Boot, Spring Modulith, Spring for GraphQL), esquema GraphQL de Clientes, sobre común de eventos y contratos de los eventos de Clientes y de `venta.registrada` v1. Sin base de datos todavía (eso es I1).

**Observación:** `CLAUDE.md` todavía dice «Fase actual: diseño». Por su propia regla, manda la Bitácora. La primera etapa crea `backend/`, que también cubre la fase 0 del plan de Inventario (esqueleto Maven y `compartido`): conviene avisar a quien tome Inventario para no duplicarlo.

**Resultado de la primera etapa (I0), con TDD:**
1. **Esqueleto** en `backend/` (Maven; Spring Boot 3.5.0, Spring Modulith 1.4.0, Spring for GraphQL; paquete `mx.negocio360`) con los módulos `compartido` (abierto) y `clientes` (solo puede depender de `compartido`).
2. **RED:** 7 clases de prueba sin código de producción: `BUILD FAILURE` con 16 errores de compilación, todos por clases que faltaban.
3. **GREEN:** sobre común de eventos (`SobreEvento`, UUIDv7 obligatorio), mapeador JSON con dinero como cadena (`MapeadorEventos`), escalares `Decimal`, `Date` y `DateTime`, los 3 eventos de Clientes sin datos personales, los contratos que Clientes lee de Ventas (`venta.registrada` y `venta.anulada` v1) y los esquemas GraphQL `compartido.graphqls` y `clientes.graphqls` (8 consultas, el par `prepararComando` y `confirmarComando`, `ComandoInput` con `@oneOf` y 8 variantes). `application.yml` cierra introspección, GraphiQL e impresión del esquema (SG-12).
4. **Revisión de código** (agente `ecc:java-reviewer`, solo lectura): sin hallazgos críticos, 1 alto, 5 medios y 8 bajos. Se corrigieron con un segundo ciclo de TDD (21 pruebas fallaron primero, cada una por un hallazgo): topes de consulta y `primeros` no nulo; una sola gramática de decimales (`DecimalPlano`) para GraphQL y JSON; `datos` de los eventos sin flotantes y con copia profunda inmutable; totales de venta no negativos; pruebas de seguridad más fuertes; y se quitaron dos dependencias sin uso.
5. **Resultado final:** 143 pruebas, 143 en verde con `mvn -B verify`; cobertura 96.1 % de líneas y 91.9 % de ramas, y el `pom.xml` ahora exige el 80 %. La prueba de introspección se comprobó con una mutación: con la introspección abierta, falla.
6. **Reporte de evidencia:** `docs/pruebas/clientes-i0.tdd.md`, con el RED y el GREEN de cada ciclo.

**Verificación hecha:** `mvn -B verify` en `backend/` con Java 21.0.10 y Maven 3.9.14.

**No verificado / límites:** solo hay contratos; no hay base de datos, seguridad por fila, resolvedores, listeners ni autenticación. Las pruebas de esquema usan wiring simulado, así que no ejecutan consultas. Ventas no existe: el contrato de `venta.registrada` es lo que Clientes espera y hay que congelarlo con su dueño. No se hicieron commits ni pushes.

**Decisión técnica que conviene revisar:** `ComandoInput` y el `Mutation` de la plataforma se crearon aquí porque Clientes es el primer módulo (D-55). Cuando Inventario implemente sus comandos tendrá que ampliar `ComandoInput` en lugar de crear otro.

**Limitación del segundo ciclo:** las pruebas de `DecimalPlano` y `LimitesDeConsulta` se escribieron antes que su código pero no se ejecutaron en RED. Las otras 21 sí.

**Pendiente tras la etapa I0:**
- **Antes del primer resolvedor** (revisor): `/graphql` hoy responde sin autenticación ni límites de profundidad, alias, costo, tamaño de cuerpo ni tasa. Es inocuo sin resolvedores, pero hay que cerrarlo primero (SG-01 a SG-10). Los textos del esquema (`notas`, `motivo`, `textoIntegral`) aún no tienen largo máximo.
- **Para quien haga Inventario:** `ComandoInput` está definido en `clientes.graphqls` y el `Mutation` en `compartido.graphqls`. Inventario debe ampliarlo con `extend input ComandoInput`, no crear otro. Conviene una prueba que lo exija cuando entre el segundo módulo.
- **Contrato con Ventas** (D-48, D-65): `cliente_id` opcional y `total` neto de cupón en `venta.registrada` v1. Clientes ya lo lee así y falla si el total no es una cadena decimal plana y no negativa.
- **Siguiente etapa:** I1 (migraciones Flyway, seguridad por fila y entidades). Necesita Docker Desktop encendido para Testcontainers (D-15) y decidir dónde vive la base (D-16).
- Los archivos nuevos (`backend/`, `docs/pruebas/`) y los cambios de la Bitácora **no están en ningún commit**. Hacer commit y pull request cuando el usuario lo pida.

### 2026-10-13 — Backlog Scrum de Clientes e Inventario en Jira
**Pedido del usuario:** leer los diseños de Clientes e Inventario y generar épicas y sprints en Jira para trabajar con Scrum. Respuestas: sprints de una semana tras la demo, alcance P1 de Clientes más MVP de Inventario, y aprobó el plan antes de crearlo.

**Qué se hizo:**
1. Plan escrito en `docs/plan/backlog-jira.md` (épicas, historias, puntos, sprints, supuestos y riesgos) y aprobado por el usuario.
2. **Jira:** proyecto HTC «Hackathon Tech Capital» en `juliocuellar.atlassian.net` (team-managed, tablero 2). Hubo que reautorizar el conector de Atlassian porque solo tenía concedido `anyaquimera1.atlassian.net`.
3. Creados **88 elementos**: 16 épicas (HTC-1 a HTC-16: PLT, CLI-I1 a CLI-I7, CLI-DOC, INV-F1 a INV-F5, INV-F7 e INV-F6) y 72 historias y tareas (HTC-17 a HTC-88), con puntos, etiquetas y criterios de aceptación.
4. **Sprints:** «HTC Sprint 1» (id 3) se renombró a «Sprint 0 · Demo» (13 al 17 oct); se crearon Sprint 1 al 5 semanales (19 oct al 20 nov). Ninguno está iniciado. 62 elementos están asignados a un sprint y 10 quedan en backlog (CLI-I7, CLI-DOC e INV-F6).

**Supuestos que hay que corregir:** 1 punto = 1 hora efectiva; capacidad de unos 30 puntos en el Sprint 0 y 40 por sprint semanal; ambos son míos, el equipo no ha dicho su disponibilidad. Los puntos por historia suman 2 a 4 veces los días-persona de los diseños (Clientes I1 a I6: 88 puntos contra 4 d-p; Inventario MVP: 108 contra 9 d-p).

**Observaciones para quien continúe:**
- **Inventario no está autorizado para programarse.** La tarea HTC-19 pide aprobar su diseño y anotar la autorización aquí; debe cerrarse antes de empezar INV-*.
- La demo del 15 no alcanza para todo el camino de Clientes: el Sprint 0 llega a alta y búsqueda; el resumen de compras y la anonimización están en el Sprint 1.
- Nadie está asignado a ninguna incidencia, y las dependencias entre elementos están solo escritas en las descripciones, no como vínculos de Jira.
- Los puntos de cada sprint son los del plan; no se verificó la suma dentro de Jira, solo la asignación de cada elemento al sprint (Sprint 0 comprobado en el tablero).

**Pendiente:** que el equipo asigne personas, recalibre los puntos tras el Sprint 0, decida qué parte corre en vivo en la demo (pregunta 7 de Clientes) e inicie el Sprint 0 en Jira.

**Archivos creados:** `docs/plan/backlog-jira.md`.
**Archivos modificados:** `docs/BITACORA.md` (esta entrada).

### 2026-10-14 — Clientes I2 sin base de datos: aviso, alta, edición, búsqueda y preparar → confirmar (HTC-25 a HTC-29)
**Pedido del usuario:** «Continúa con HTC-25 a HTC-29 sin Docker». Antes, las 88 incidencias del proyecto HTC en Jira (HTC-1 a HTC-88) se asignaron al usuario, a petición suya; no se tocaron las de ejemplo SCRUM-1 a SCRUM-4.

**Alcance y autorización:** solo el módulo de Clientes, bajo la autorización del 2026-10-13. No se tocó Inventario. Rama `julio`, TDD, sin commits ni pushes.

**Qué se hizo** (detalle y evidencia en `docs/pruebas/clientes-i2.tdd.md`):
1. **Dominio** (`clientes/dominio`): teléfono E.164, correo, texto de búsqueda, `DatosCliente`, `Cliente`, `AvisoPrivacidad`, `Consentimiento` y `CriterioBusqueda`, con los 24 códigos de error del diseño. Lista de caracteres permitidos (SG-17).
2. **Puertos** (`clientes/aplicacion/puertos`): `RepositorioClientes`, `RepositorioAvisos`, `RepositorioConsentimientos`, `RepositorioPreparaciones` y `PublicadorEventos`. Dobles en memoria en `src/test` que imitan lo que hará la base.
3. **Casos de uso** (`clientes/aplicacion`): `PublicarAvisoPrivacidad` (HTC-25), `RegistrarCliente` (HTC-26), `EditarCliente` (HTC-27), `buscar` y `sugerir` (HTC-28), y `ServicioPreparacion` con token de un solo uso (HTC-29). `ClientesApi` es la interfaz pública.
4. **Compartido:** `UuidV7`, `ContextoNegocio` y `ZonaMexico`.

**Resultado:** `mvn -B verify` con Java 21.0.10: **410 pruebas en verde**, Spring Modulith en verde, **98.4 % de líneas y 95.3 % de ramas**. Tres mutaciones a propósito rompieron las pruebas esperadas. Revisión con `ecc:java-reviewer` y `ecc:security-reviewer`: ningún crítico; se corrigieron en un segundo ciclo (RED con 52 fallas y 2 errores) los hallazgos que cabían en esta etapa.

**Decisiones mías que el equipo debe confirmar o corregir** (propuestas, no aceptadas):
- **P-1.** `avisoVersion` del alta debe ser exactamente la del aviso vigente; si no, `AVISO_NO_VIGENTE`.
- **P-2.** El teléfono acepta lada con primer dígito de 2 a 9; el prefijo móvil antiguo `+52 1` no se admite.
- **P-3.** Límites iniciales del aviso: responsable 120, domicilio 200, contacto ARCO 200, texto simplificado 2 000, texto integral 20 000 y hasta 20 finalidades. Un texto fuera de límite o con caracteres prohibidos usa `AVISO_INCOMPLETO` porque no hay otro código.
- **P-4.** En la edición, un campo nulo no cambia y un texto vacío borra los opcionales; la fecha de nacimiento no se puede borrar. Las notas descartan en silencio los caracteres prohibidos (no existe un código para rechazarlos).
- **P-5.** Los cursores de las listas son el id del último cliente (UUIDv7), ordenado por id y no por nombre, para que el cursor no lleve datos personales.
- **P-6.** El token falla cerrado: se consume antes de ejecutar y se vacía el comando aunque la ejecución falle. **Contrato transaccional propuesto** en el Javadoc de `ServicioPreparacion` (`marcarUsada` y `vaciar` en su propia transacción; cada `confirmar` de un manejador, una sola con su outbox). Si se unen en una, cambia el comportamiento.

**Decisiones abiertas que salieron de la revisión:** consentimiento contra las finalidades del aviso y rechazo de `APP_TURISTA` desde el panel (HTC-34); `recurso_id` en `preparacion` para reintentar una confirmación perdida; ranking u orden de `sugerir`; si `paginaEfectiva` recorta o falla (SC-03).

**No verificado / límites:** no hay base de datos, transacciones, outbox, cableado de Spring, autenticación ni resolvedores. La unicidad del teléfono, el bloqueo optimista y la atomicidad de `marcarUsada` solo están en dobles en memoria. Las preparaciones que nunca se confirman conservan su comando hasta la purga de 24 horas (HTC-38), que no existe.

**Notas para HTC-24:** índice único parcial `(negocio_id, telefono)` entre activos; único `(negocio_id, version)` en `aviso_privacidad` (hoy `ultimaVersion()+1` tiene una carrera); escapar `% _ \` en la búsqueda con `ESCAPE`; que `TELEFONO_DUPLICADO` traduzca el id existente; considerar HMAC del comando; una prueba de contrato que corra la misma batería contra el doble y contra Postgres.

**Jira:** HTC-25 a HTC-29 quedan comentadas con este resultado y en «En curso». No pasan a «Listo» hasta que el código se confirme en un commit y se revise.

**Archivos creados:** `backend/src/main/java/mx/negocio360/{compartido/{identidad,contexto,tiempo},clientes/{dominio,aplicacion,aplicacion/puertos}}`, `ClientesApi.java`, pruebas en `backend/src/test/java/...` y `docs/pruebas/clientes-i2.tdd.md`.
**Archivos modificados:** `docs/BITACORA.md` (esta entrada).
**Pendiente:** commit y pull request cuando el usuario los pida; HTC-21 a HTC-24 cuando haya Docker (HTC-17).

### 2026-10-13 — Mapa de Inventario v0.3 con Scrum (épicas y sprints)
**Pedido del usuario:** con `/ecc:plan`, «genera el artefacto para el modulo de inventario, con todos los elementos que te he solicitado con anterioridad, ademas de colocar las tareas que estaran en el scrum divididas en epics y sprints».

**Qué se hizo:**
1. Se republicó el artefacto «Mapa de Inventario» (misma URL, versión 3 en el visor) con el formato del Mapa de Clientes: 18 tablas (13 de Inventario y 5 de Restaurantes) con ER y ciclos de vida, 38 reglas con prueba y épica, 28 funciones de `InventarioApi`, 9 eventos publicados y 2 consumidos, 33 vías de comunicación filtrables por protocolo (GraphQL, WebSocket, REST, Java y bus), plan de 12 operaciones GraphQL y 2 suscripciones con esquema propuesto, 8 secuencias, 3 flujos de API, 21 reglas de seguridad con su estado en el código, decisiones D-08 a D-63 que tocan Inventario, hallazgos, riesgos y 14 preguntas abiertas.
2. **Sección Scrum:** 7 épicas de Inventario (INV-F1 a INV-F7) con meta, dependencias, criterios de aceptación y 36 historias (33 del plan de Jira y 3 nuevas), gráfica de carga por sprint contra la capacidad supuesta, calendario, dependencias entre épicas, tablero filtrable por sprint, meta y revisión de cada sprint, comparación con los días-persona del diseño, definición de listo y de terminado, y ceremonias. Los puntos por sprint salen del mismo código y se comprobaron contra `docs/plan/backlog-jira.md` (31, 38, 38, 33, 26, 34 y 35 en backlog; MVP de Inventario 108).
3. **Cambios al plan anterior por decisiones nuevas:** D-63 quita el REST del bot y el flujo F4; D-55 hace que Inventario extienda `ComandoInput` con 10 variantes en lugar de definir sus mutaciones.
4. **Hallazgos H-1 a H-8** (todos propuesta): tope de página 100 contra 50 de Clientes; el CPP con 4 decimales deja 26 centavos de diferencia en el ejemplo calculado; `ServicioPreparacion` vive dentro de Clientes y Inventario lo necesita (historia nueva INV-F3.5, 3 pts); Catálogo y Ventas no existen (INV-F4.8, 2 pts); `compartido.graphqls` no declara `Subscription`; D-24 no tenía historia (INV-F4.9, 2 pts). Las 3 historias nuevas suman 7 puntos y **no están en Jira**.

**Verificación hecha:** los 27 diagramas pasan el analizador de Mermaid 11 en Node; el generador falla si los puntos por sprint o el kardex no coinciden con el plan; la página local se abrió en Chrome a ancho de celular (sin desbordamiento horizontal de la página) y se vio la sección Scrum en tema oscuro.

**No verificado:** la página publicada no se abrió en el visor de claude.ai, así que no sé si los diagramas se dibujan allí, ni se vio el tema claro ni la gráfica de carga. No pude leer los tickets de Jira desde esta sesión: las historias se identifican por épica y orden del plan, y las claves de épica HTC-10 a HTC-16 son inferidas del orden de creación. La capacidad (30 y 40 puntos) y 1 punto = 1 hora son supuestos del plan. Los avisos de estado de SG se basan en lo que existía el 2026-10-13.

**Observaciones para quien continúe:**
- **Inventario sigue sin autorización escrita para programarse.** Es la tarea PLT-3 (HTC-19).
- Con este plan Inventario empieza el 26 de octubre, después de la demo del 15 al 17: en la demo solo puede aparecer como mockup (pregunta abierta 12).
- Dos sesiones escriben en este repositorio; esta solo tocó una fila de la tabla de documentos y esta entrada.

**Pendiente:** contestar las 14 preguntas del mapa; dar de alta en Jira las 3 historias nuevas si se aceptan; congelar con Ventas `venta.anulada` con líneas (D-25); actualizar `docs/diseno/inventario.md` por D-55 y D-63 (PLT-4).

**Archivos creados:** ninguno en el repositorio (la fuente del artefacto está en la carpeta temporal de la sesión).
**Archivos modificados:** `docs/BITACORA.md` (fila del Mapa de Inventario y esta entrada).

### 2026-10-13 — Jira verificado y 3 historias de Inventario creadas (corrige la entrada anterior del Mapa de Inventario v0.3)
**Pedido del usuario:** conectar Jira a la sesión («ya quedó») y, tras ver lo encontrado, «hazlo» sobre las correcciones propuestas.

**Qué se verificó en Jira (sitio `juliocuellar.atlassian.net`, proyecto HTC):** las épicas de Inventario son HTC-10 a HTC-16 (INV-F1 a INV-F7), tal como se había inferido. Las 33 historias del plan (HTC-56 a HTC-88) y las tareas HTC-19 y HTC-20 existen con los puntos y el sprint del mapa: Sprint 0 = 1 pt, Sprint 2 = 15, Sprint 3 = 33, Sprint 4 = 26, Sprint 5 = 34 y 8 en backlog (INV-F6). Todo está en «Por hacer» y los sprints en estado futuro.

**Qué se cambió en Jira (con autorización del usuario):**
1. Etiqueta de HTC-77 a HTC-81 (lotes y caducidades): de `P1` a `P2`, para coincidir con el diseño.
2. Se crearon tres historias, sin asignar y con puntos: **HTC-89** extraer el servicio de preparar → confirmar a `compartido` (3 pts, hijo de HTC-12, Sprint 3); **HTC-90** puerto `CatalogoApi` con doble de prueba (2 pts, hijo de HTC-13, Sprint 4); **HTC-91** estado `FALLIDO` y evento `inventario.consumo_fallido` (2 pts, hijo de HTC-13, Sprint 4). Con ellas, el Sprint 3 queda en 36 puntos y el Sprint 4 en 30, dentro de la capacidad supuesta de 40.

**Qué cambia respecto a la entrada anterior de hoy:** ya no es cierto que las claves HTC-10 a HTC-16 fueran inferidas, que las tres historias nuevas no estuvieran en Jira ni que nadie estuviera asignado: las 35 incidencias de Inventario del plan original están asignadas a una sola persona (julio cuellar). No se revisó la asignación de las incidencias de Clientes. El Mapa de Inventario se republicó (versión 4 en el visor) con la clave HTC de cada historia y estos textos corregidos; el hallazgo H-9 registra la etiqueta y que HTC-67 ya pedía reutilizar el patrón de Clientes.

**No verificado:** la página publicada no se abrió en el visor de claude.ai. No se revisó el tablero de Jira en el navegador, solo por la API. Las 3 historias nuevas no tienen vínculos de dependencia en Jira.

**Pendiente:** asignar las 3 historias nuevas; sigue abierta la autorización escrita para programar Inventario (HTC-19).

**Archivos modificados:** `docs/BITACORA.md` (esta entrada).

### 2026-10-13 — Diseño del módulo de Identidad (autenticación, equipo y permisos), propuesta
**Pedido del usuario:** con `/ecc:plan`, definir el módulo de autenticación para agregarlo a los demás módulos, con varios usuarios por negocio (cajeros, gerentes, administradores, meseros). Luego respondió: una persona no puede ser personal y turista con la misma cuenta (los turistas tienen cuenta aparte), tomar las recomendaciones del plan, usar **REST** en lugar de GraphQL para este módulo, y dividir en épicas y sprints de Jira.

**Qué se hizo:**
1. Se publicó el artefacto privado «Mapa de Identidad» (ver tabla de documentos): alcance, 13 tablas, 45 reglas de dominio, 26 endpoints REST, API Java (`IdentidadApi`, `Autorizacion`, `CatalogoDePermisos`), 25 permisos con matriz de 8 roles y plantillas por giro, 4 eventos, 7 funciones clave explicadas paso a paso, 20 reglas de seguridad con cobertura OWASP, 5 secuencias y 10 decisiones (D-66 a D-75, **números provisionales**).
2. **Plan de Scrum propuesto, no creado en Jira:** 8 épicas nuevas (IDN-0 a IDN-6 e IDN-DOC), 36 historias, 90 puntos en el MVP y 26 en backlog. Ordenado por dependencias y empacado con capacidad supuesta de 40 por sprint, **el plan completo termina en el Sprint 7 y no en el 5**: hay que crear los sprints 6 y 7 y mover 33 historias que ya están en Jira (todo CLI-I6 y los INV-F2 a INV-F7, salvo INV-F1). HTC-44 (autenticación en `/graphql`) se reemplazaría por la épica IDN-4.
3. Hallazgos I-1 a I-8, entre ellos: la utilidad de tokens y hashes de `ServicioPreparacion` debe compartirse con HTC-89; `cuenta`, `sesion` y `codigo_acceso` no tienen negocio y necesitan un rol de base de datos propio; nadie es dueño del alta de negocios.

**Verificación hecha:** los 15 diagramas pasan el analizador de Mermaid; el generador falla si un permiso de rol no existe en el catálogo, si una regla cita una épica inexistente o si los puntos de lo ya existente no coinciden con Jira (38, 36, 30 y 34 en los sprints 2 a 5, leídos de Jira ese día). La página local se abrió a ancho de celular sin desbordamiento.

**No verificado:** la página publicada no se abrió en el visor de claude.ai. Las reglas de seguridad salen de conocimiento general de OWASP, no se contrastaron con las guías. La matriz de permisos, las vigencias, los topes y los puntos son propuestas mías sin validar con el equipo ni con negocios. No se revisó el tema claro.

**Observaciones para quien continúe:**
- **No hay autorización escrita para programar Identidad.** Tampoco se aceptó la excepción D-73 al patrón preparar → confirmar de `CLAUDE.md` (aquí se usa una confirmación reforzada).
- Nada de esto se creó en Jira: se necesita aprobar antes el reordenamiento de sprints.
- Falta decidir el canal real de códigos y quién es dueño del alta de negocios.

**Pendiente:** contestar las 10 preguntas del mapa; aprobar o ajustar el plan de sprints; escribir `docs/diseno/identidad.md`; crear en Jira las épicas y los sprints 6 y 7 si se aprueba.

**Archivos creados:** ninguno en el repositorio (el generador está en la carpeta temporal de la sesión).
**Archivos modificados:** `docs/BITACORA.md` (fila del Mapa de Identidad y esta entrada).

### 2026-10-14 — Autorización para programar el módulo de Identidad
**Autorización escrita (cita textual del usuario en la sesión):** «comencemos con el desarrollo del modulo de identidad utiliza la skill /ecc:tdd-workflow para ejecutar el plan que establecimos, tambien genera clases test para lo que realizaremos y una colección en postman para pruebas».

**Alcance que le doy a esa autorización (interpretación mía):** el módulo `identidad` según el Mapa de Identidad v0.1, empezando por la épica **IDN-1** (cuenta, código y sesión) y sus endpoints REST de acceso. Sin base de datos todavía (adaptadores en memoria solo para desarrollo y demo, porque falta Docker y las migraciones son IDN-1.1), sin tocar Jira y sin cambiar el orden de sprints. **No** acepta por sí sola D-73 (confirmación reforzada en lugar de preparar → confirmar), que se necesita hasta IDN-3, ni el reordenamiento a los sprints 6 y 7. Cada etapa siguiente necesita que el equipo la autorice y se anote aquí.

**Cómo se trabaja:** en la rama `julio`, con TDD (RED, GREEN, refactor, evidencia en `docs/pruebas/`), sin commits ni pushes hasta que el usuario los pida, sin dependencias nuevas.

### 2026-10-14 — Módulo de Identidad: IDN-1 programada sin base de datos (cuenta, código y sesión)
**Pedido del usuario:** el mismo de la entrada anterior (desarrollo con `/ecc:tdd-workflow`, clases de prueba y colección de Postman).

**Qué se hizo:**
1. Módulo `identidad` con TDD en cuatro ciclos: utilidades y dominio (`TokenOpaco`, `HashConSal`, `Telefono`, `NombreVisible`, `Sesion`, `CodigoAcceso`, `DireccionIp`), casos de uso (`IdentidadServicio` con `IdentidadApi`), adaptadores en memoria solo para desarrollo y demo, y API REST de acceso: `POST /auth/codigo`, `POST /auth/sesion`, `GET /auth/yo`, `DELETE /auth/sesion`, `DELETE /auth/sesiones` y `PATCH /cuenta`.
2. Dos revisiones de solo lectura (`ecc:java-reviewer` y `ecc:security-reviewer`): ninguna crítica. Se corrigió en un cuarto ciclo lo que era defecto del código y no cambiaba el contrato (detalle en `docs/pruebas/identidad-idn1.tdd.md`).
3. Colección de Postman en `backend/postman/` (20 solicitudes) con su entorno.
4. Reporte de evidencia: [`docs/pruebas/identidad-idn1.tdd.md`](pruebas/identidad-idn1.tdd.md).

**Cambio que afecta a quien arranque la aplicación:** el canal de códigos simulado y el endpoint `GET /dev/canal/codigo` ya no dependen de «no ser producción»: solo existen con `identidad.canal-simulado.habilitado=true`. Para desarrollar o hacer la demo hay que arrancar con `mvn spring-boot:run "-Dspring-boot.run.profiles=desarrollo"`. **Sin eso la aplicación no arranca, a propósito.** Las pruebas lo habilitan en `src/test/resources/config/application.yml`.

**Verificación hecha:** `mvn -B verify`: 705 pruebas, 0 fallas, cobertura de Identidad 99.6 % de líneas y 95.7 % de ramas. Siete mutaciones puntuales a mano (una no se detectó al principio y se cubrió con una prueba nueva). La aplicación se arrancó y se recorrió el flujo con curl y Node; sin el perfil se comprobó que no inicia.

**No verificado:**
- Los scripts de Postman (`pm.test`, `pm.sendRequest`) **no se ejecutaron** en Postman ni en Newman (no está instalado).
- No hay base de datos ni Docker: las migraciones, la seguridad por fila y las pruebas con Postgres siguen pendientes (IDN-1.1). Los datos viven en memoria.
- El RED de cada ciclo fue de compilación, no se vio fallar cada prueba nueva contra un código anterior (para compensarlo se hicieron las mutaciones).
- No se volvió a pasar una revisión de agentes sobre los cambios del cuarto ciclo.
- Nada llama aún a `autenticar` en `/graphql` ni en otras rutas (IDN-4): las consultas de Clientes siguen anónimas.

**Decisiones que necesitan al equipo** (ninguna se tomó por cuenta propia):
- **Bloqueo por terceros:** quien conozca un teléfono puede gastar los intentos de su código o su cupo de solicitudes. La corrección propuesta es que `/auth/codigo` devuelva un `desafioId` que `/auth/sesion` exija, lo que **cambia el contrato público**.
- Si los errores de código vencido y agotado se unifican en uno solo (depende de lo anterior).
- Topes diarios por teléfono y presupuesto global de mensajes, y su valor.
- HMAC con un secreto de aplicación para el hash del código y las claves del limitador, y dónde guardar ese secreto.
- Rediseñar los puertos de repositorios (actualizaciones condicionales) antes de IDN-1.1.
- Canal real de códigos (IDN-DOC.1), alta de negocios, excepción D-73 y los sprints 6 y 7 siguen como en la entrada del diseño.

**Pendiente:** autorización escrita para el siguiente tramo (IDN-2 en adelante); asignar HTC-89, HTC-90 y HTC-91; HTC-89 debe absorber la duplicación de utilidades de token y hash y de `Telefono`; antes del primer resolvedor GraphQL cerrar `/graphql` (SG-01 a SG-10).

**Archivos creados:** `backend/src/main/java/mx/negocio360/identidad/**`, `backend/src/main/java/mx/negocio360/compartido/seguridad/{TokenOpaco,HashConSal}.java`, `backend/src/main/java/mx/negocio360/compartido/web/LimitesDeJson.java`, `backend/src/main/resources/application-desarrollo.yml`, las pruebas correspondientes y `backend/src/test/resources/config/application.yml`, `backend/postman/*`, `docs/pruebas/identidad-idn1.tdd.md`.
**Archivos modificados:** `docs/BITACORA.md` (esta entrada) y `backend/src/test/java/mx/negocio360/EstructuraModulosTest.java` (exige también el módulo `identidad`).

### 2026-10-14 — Identidad en Jira: épicas, historias y Sprint 6 y 7 creados; 33 historias reordenadas
**Pedido del usuario:** «hazlo y los sprints que resolviste los tachas como Listo en el tablero», tras preguntarle si creaba en Jira el plan de Identidad. Con eso queda **aprobado el plan de sprints del Mapa de Identidad** (sprints 6 y 7 y reordenamiento de 33 historias).

**Qué se hizo en Jira (proyecto HTC):**
1. **Sprint 6** (id 9) y **Sprint 7** (id 10) en el tablero 2, **sin fechas** (se quitaron a petición del usuario; el plan los proponía del 23 al 27 de noviembre y del 30 de noviembre al 4 de diciembre). Las épicas tampoco tienen fechas. Los sprints 0 a 5 conservan las suyas.
2. **8 épicas:** IDN-0 = HTC-92, IDN-1 = HTC-93, IDN-2 = HTC-94, IDN-3 = HTC-95, IDN-4 = HTC-96, IDN-5 = HTC-97, IDN-6 = HTC-98 (P2) e IDN-DOC = HTC-99.
3. **36 historias, HTC-100 a HTC-135**, con puntos, etiquetas `P1` o `P2` más `identidad`, asignadas a julio cuellar y en el sprint del plan. IDN-6 e IDN-DOC quedan en el backlog sin sprint. Clave por historia: IDN-0.1 = HTC-100, IDN-0.2 = HTC-101, IDN-1.1 a 1.6 = HTC-102 a HTC-107, IDN-2.1 a 2.6 = HTC-108 a HTC-113, IDN-3.1 a 3.6 = HTC-114 a HTC-119, IDN-4.1 a 4.4 = HTC-120 a HTC-123, IDN-5.1 a 5.5 = HTC-124 a HTC-128, IDN-6.1 a 6.5 = HTC-129 a HTC-133, IDN-DOC.1 y DOC.2 = HTC-134 y HTC-135.
4. **33 historias existentes** pasaron al sprint que les toca (Clientes GraphQL e Inventario); quedaron donde estaban HTC-56 a HTC-60. Carga resultante en Jira, verificada con consultas: Sprint 1 = 39, Sprint 2 = 39, Sprint 3 = 40, Sprint 4 = 39, Sprint 5 = 38, Sprint 6 = 38, Sprint 7 = 28 puntos. Termina en el Sprint 7, dos sprints después de lo previsto.
5. **HTC-44** salió del Sprint 2 al backlog, con la etiqueta `reemplazada-por-IDN-4` y una nota en la descripción que apunta a HTC-120 y HTC-121. No se borró.
6. **Marcadas como «Listo»:** HTC-100 (IDN-0.1) y HTC-103 a HTC-107 (IDN-1.2 a IDN-1.6), que son lo que se programó hoy. HTC-102 (IDN-1.1, migraciones) **no** se marcó porque no existe base de datos; ni HTC-101 (IDN-0.2, acordar permisos con Clientes e Inventario).

**Cosas a tener en cuenta:**
- Los sprints siguen en estado «futuro»: no se inició ni se cerró ninguno. «Listo» está puesto en las historias, no en los sprints.
- La definición de terminado del plan pide cero hallazgos altos de seguridad. IDN-1 tiene abiertos A-02/H3, H2/H4 y H12/M-04 (ver `docs/pruebas/identidad-idn1.tdd.md`); quedaron anotados en las descripciones de HTC-105 y HTC-128. HTC-100 se marcó Listo aunque D-73 no está aceptada (anotado en su descripción).
- Tres movimientos (HTC-77, HTC-83 y HTC-84) los bloqueó el control de permisos de la sesión y se hicieron con la herramienta de sprints.
- No se crearon vínculos de dependencia entre historias.

**No verificado:** el tablero no se abrió en el navegador, solo por la API.

**Pendiente:** asignar HTC-89, HTC-90 y HTC-91 (siguen sin responsable); autorización escrita para programar Inventario (HTC-19) y para IDN-2 en adelante.

**Archivos modificados:** `docs/BITACORA.md` (esta entrada).

### 2026-10-14 — Autorización: PostgreSQL con outbox y migraciones con Flyway, sin RabbitMQ
**Autorización escrita (cita textual del usuario):** «Postgres con el outbox, sin RabbitMQ, genera migraciones con Flyway, al igual que crea la base de datos de postgreSQL en mi imagen de docker».

**Alcance que le doy (interpretación mía):**
- Base PostgreSQL de este proyecto en un contenedor propio, con la imagen `postgres:17-alpine` que ya tiene en su Docker. **No se toca** `backend-postgres-1` (puerto 15432), que parece ser de otro proyecto.
- Migraciones con Flyway para los esquemas de **Identidad** (IDN-1.1), **Clientes** (HTC-21, HTC-22 y HTC-23) y la tabla de publicación de eventos de Spring Modulith, que el diseño define como el outbox de la plataforma. **Inventario queda fuera**: sigue sin autorización escrita para programarse (HTC-19).
- Dependencias nuevas autorizadas por esta instrucción: driver de PostgreSQL, JDBC, Flyway, `spring-modulith-starter-jdbc` y Testcontainers para pruebas.
- **No** incluye reemplazar los repositorios en memoria de Identidad y Clientes por repositorios JDBC, salvo que el equipo lo pida. **RabbitMQ queda descartado**: el bus de eventos es interno al monolito.
- Contraseñas: solo en un archivo `.env` local (ignorado por git) y variables de entorno; nunca en el repositorio ni en el chat.

### 2026-10-14 — PostgreSQL en Docker, migraciones con Flyway y outbox: hecho y revisado
**Pedido del usuario:** el de la entrada anterior (autorización).

**Qué se hizo:**
1. **Base propia en Docker:** `backend/docker-compose.yml` (proyecto `negocio360`, contenedor `negocio360-postgres`, imagen `postgres:17-alpine`, puerto `127.0.0.1:5433`, volumen propio). Dos roles: el dueño (superusuario del contenedor, corre Flyway) y `negocio360_app` (sin superusuario, sujeto a la seguridad por fila; lo crea `backend/docker/postgres/01-roles.sh`). Las contraseñas están en `backend/.env` (ignorado por git; la plantilla es `.env.example`). **No se tocaron** `backend-postgres-1` ni los demás contenedores de otros proyectos.
2. **Migraciones de Flyway** en `backend/src/main/resources/db/migration/`: `V001` (esquemas `plataforma`, `identidad`, `clientes`, roles de grupo, `negocio_actual()`), `V002` (outbox), `V010` (Identidad: cuenta, código, sesión) y `V020` (Clientes: 9 tablas P1, seguridad por fila forzada en las 8 con `negocio_id`, historial de solo inserción, anonimización irreversible).
3. **Outbox sin RabbitMQ:** es la tabla `event_publication` de Spring Modulith; el evento se guarda en la misma transacción, se entrega al menos una vez y queda pendiente si el listener falla. Probado contra PostgreSQL real.
4. **Revisión de `ecc:database-reviewer`** (solo lectura): 0 críticos, 2 altos, 9 medios, 12 bajos. Se corrigieron los medios que eran de la migración, de las pruebas o del entorno, con las pruebas escritas primero (detalle y estado de cada hallazgo en [`docs/pruebas/postgres-flyway-outbox.tdd.md`](pruebas/postgres-flyway-outbox.tdd.md)).
5. **Dependencias nuevas** (autorizadas): JDBC, driver de PostgreSQL, Flyway, `spring-modulith-starter-jdbc` y Testcontainers. **Testcontainers va en 1.21.4** (`<testcontainers.version>` en el `pom.xml`): la 1.21.0 que trae Spring Boot 3.5.0 no funciona con Docker 29.

**Cambios que afectan a quien arranque la aplicación:**
- La aplicación **ya no arranca sin la base**. Desde `backend/`: copiar `.env.example` como `.env`, rellenar las dos contraseñas (largas, solo letras y números) y correr `docker compose up -d --wait`. Luego `mvn spring-boot:run "-Dspring-boot.run.profiles=desarrollo"`. Las contraseñas no tienen valor por omisión: si faltan, falla cerrado.
- La colección de Postman necesita la base levantada.
- Las contraseñas se aplican solo al crear el volumen. Si se cambian después hace falta `docker compose down -v` (borra los datos) o un `ALTER ROLE`.
- `.gitattributes` fuerza finales de línea LF en los `.sh`, para que `01-roles.sh` no se rompa al clonar en Windows.
- **Cuando se haga el primer commit, un cambio al esquema debe ir en una migración nueva** (V003...): hoy las migraciones se editaron en su lugar porque nadie las había aplicado fuera de este equipo, y la base de desarrollo se recreó.

**Verificación hecha:** `mvn -B verify`: **824 pruebas, 0 fallas** (antes 705), cobertura cumplida; 119 de ellas son de persistencia contra PostgreSQL 17 real en Docker (Testcontainers). RED real en las dos rondas (43 y 18 fallas esperadas). Una mutación (política de seguridad por fila abierta): fallaron 23 de 38 pruebas de aislamiento. En la base real: las 4 migraciones se aplicaron y `POST /auth/codigo` respondió 202, sin contraseñas en el registro.

**No verificado:**
- **Ningún módulo usa la base todavía.** Los repositorios de Identidad y Clientes siguen en memoria; lo que se probó es el esquema, los permisos y el outbox. Falta el reemplazo por JDBC con `set_config('app.negocio_id', ..., true)` en cada transacción, que no está autorizado salvo que el equipo lo pida.
- Rendimiento: no hay `EXPLAIN` sobre volumen. Un solo pool y un solo rol de aplicación (miembro de los tres roles de grupo).
- Inventario no tiene migraciones (sin autorización escrita, HTC-19).
- No se volvió a pasar la revisión de agentes sobre las correcciones de esta ronda.

**Decisiones que necesitan al equipo** (ninguna se tomó por cuenta propia):
- **A1, purgas:** la preparación a las 24 horas, las marcas `ANULADO_PREVIO` a los 30 días y `evento_procesado` a los 90 no existen, y un trabajo programado que corra sin negocio **no ve esas filas** bajo seguridad por fila. Opciones: una función `SECURITY DEFINER` solo para el trabajo de purga, un rol de mantenimiento con política propia, o que el trabajo recorra los negocios.
- **A2, el superusuario en la aplicación:** Flyway corre dentro del proceso, así que ese proceso tiene la contraseña del superusuario del contenedor. Aceptable para la demo si se anota; para producción, Flyway como paso aparte y un dueño sin superusuario.
- Quién puede leer `event_publication` y `evento_procesado` entre negocios (M5), cómo se ejecuta la retención de consentimiento y de la bitácora de accesos (B11), el ciclo de vida del outbox (B8), y cómo se anonimiza una cuenta de Identidad sin inventar un teléfono (B7).

**Pendiente:** autorización para los repositorios JDBC y para IDN-2 en adelante; decidir A1 y A2; marcar como «Listo» en Jira HTC-21, HTC-22, HTC-23 y HTC-102 si el equipo lo aprueba (no se tocó Jira en este tramo).

**Archivos creados:** `backend/docker-compose.yml`, `backend/docker/postgres/01-roles.sh`, `backend/.env.example`, `backend/.env` (local, ignorado), `.gitattributes`, `backend/src/main/resources/db/migration/V001, V002, V010, V020`, `backend/src/test/java/mx/negocio360/persistencia/*` (11 clases), `backend/src/test/resources/META-INF/spring.factories`, `docs/pruebas/postgres-flyway-outbox.tdd.md`.
**Archivos modificados:** `backend/pom.xml`, `backend/src/main/resources/application.yml`, `backend/postman/*` (descripción), `docs/BITACORA.md` (esta entrada).

### 2026-10-14 — Diseño y plan de Ventas y Caja (propuesta)
**Pedido del usuario:** diseñar Ventas y Caja para el comerciante (museo y misceláneo), separados, con entidades, funciones, reglas con ID, eventos y comunicación con la API; los planes en Markdown y el código documentado con JavaDoc. Al clonar el repositorio respondió «si» a rehacer el plan dentro de él.

**Qué se vio:**
1. Un primer borrador se hizo antes de clonar el repositorio (página publicada, ver «Documentos relacionados»). Al leer la Bitácora y los diseños se encontraron choques y se corrigieron en los documentos nuevos: los eventos se llaman `venta.registrada` y `venta.anulada` (Clientes ya los consume); el cupón va por D-34 (Ventas llama a Pasaporte, no al revés); `C-xx` ya lo usa Inventario y `V-xx` Clientes, así que Ventas usa `VT-xx` y Caja `CJ-xx`; las mutaciones van por `prepararComando` / `confirmarComando` (D-55); redondeo HALF_UP (D-20); sin REST de bot (D-63).
2. `preparar` no escribe en las tablas de negocio, como en Inventario: no existen ventas «preparadas». La única excepción propuesta es el corte de caja (D-81).
3. Ventas no conoce a Caja para no crear un ciclo: el canal pasa el `turno_id` (D-76).
4. El plan da ≈ 9.9 días-persona contra un día disponible: **no cabe** antes del evento. Recomendación: mostrar Ventas y Caja como mockup en la demo y programar después.

**Decisiones:** D-76 a D-82 (PROPUESTA). D-83, JavaDoc obligatorio (ACEPTADA, dicho por el usuario).

**Pendiente:**
- Que el equipo responda §13 de `ventas.md`, §12 de `caja.md` y §13 del plan (programar o mockup, dueños de Catálogo y Pasaporte, quién anula, umbral del corte).
- Acordar D-77 con quien programa Clientes e Inventario, y `cotizarCupon` / `canjearCupon` con el dueño de Pasaporte.
- Si se acepta D-83 para todo el proyecto, agregarlo a la skill `negocio360-reglas` y a `.agent/rules/negocio360.md` (no se tocaron).
- **No hay autorización para programar Ventas ni Caja.**

**No verificado:** los diagramas Mermaid no se renderizaron. No se hizo commit ni se cambió de rama (los archivos quedan sin confirmar en `main`; el flujo de D-32 pide trabajar en la rama personal).

**Archivos creados:** `docs/diseno/ventas.md`, `docs/diseno/caja.md`, `docs/planes/ventas-caja-v1.md`.
**Archivos modificados:** `docs/BITACORA.md` (decisiones D-76 a D-83, documentos relacionados y esta entrada).

### 2026-10-14 — Página de Ventas y Caja, versión 2
**Pedido del usuario:** «Crea el artefacto como te lo pedi anteriormente, con la diferencia que corrigas y agreges lo que falto basandote en el repositorio y su contenido».

**Qué se hizo:**
1. Se republicó la página https://claude.ai/artifact/FVtCcUoJHC8A48rUcJ17xE (misma URL, versión 2) con el contenido de `docs/diseno/ventas.md`, `docs/diseno/caja.md` y `docs/planes/ventas-caja-v1.md`. Sigue la skill `generar-mockup-html`: chips de borrador y datos inventados, identidad talavera y cempasúchil, azul punteado = síncrono y dorado continuo = asíncrono, «Qué mirar» en cada figura y fuentes al pie.
2. Lo pedido originalmente (entidad-relación, diagrama de eventos, secuencias, tablas de entidades, funciones, reglas y eventos con JSON, y tabla de comunicación con la API) más lo que faltaba: tabla de qué se corrigió de la versión 1, decisiones con su estado, códigos de error, contratos con otros módulos, estados, criterios de aceptación, convención JavaDoc (D-83), plan con veredicto de calendario y preguntas.
3. Copia local en `docs/mockups/ventas-caja.html`, con Mermaid 11.4.1 desde jsDelivr (se comprobó que la URL responde 200).
4. Al publicar se vio que la página ya está **compartida con cualquiera que tenga el enlace** (lo cambió el usuario). La versión 2 se publicó en esa misma URL, así que quien tenga el enlace ve los cambios.

**Decisiones:** ninguna nueva. Se mantienen D-76 a D-82 (PROPUESTA) y D-83 (ACEPTADA).

**No verificado:** no se abrió la página ni la copia local en un navegador, así que no se revisó a 400 y 1280 px, en tema claro y oscuro, ni que los 8 diagramas Mermaid se dibujen sin error. Solo se comprobó que el JavaScript de la página no tiene errores de sintaxis (`node --check`). No se hizo commit.

**Archivos creados:** `docs/mockups/ventas-caja.html`.
**Archivos modificados:** `docs/BITACORA.md` (fila de «Documentos relacionados» y esta entrada).

### 2026-10-14 — Autorización para programar Ventas y Caja
**Autorización escrita (cita textual del usuario):** «Autorizo programar Ventas y Caja en la rama uriel».

**Alcance que le doy (interpretación mía):**
- Código de los módulos `ventas` y `caja` según `docs/diseno/ventas.md`, `docs/diseno/caja.md` y `docs/planes/ventas-caja-v1.md`, empezando por las fases F2 (Ventas) y F3 (Caja) **en memoria**, como Clientes I2: dominio, comandos con preparar → confirmar, eventos y listeners, con pruebas primero (TDD) y JavaDoc (D-83).
- Catálogo y Pasaporte no existen: Ventas los usa por puertos propios con dobles en las pruebas. No se crean esos módulos.
- **No incluye** migraciones de Flyway ni repositorios JDBC (F5), ni el esquema GraphQL (F4), salvo que el usuario lo pida. Tampoco cambia los contratos ya programados de Clientes.
- D-76 a D-82 siguen como PROPUESTA: se programan tal cual están en el diseño y, si el equipo cambia alguna, se ajusta el código.
- Trabajo en la rama local `uriel`, creada desde `main` (la remota `origin/uriel` estaba 4 commits atrás y sin nada propio). No se hace `git push` sin confirmar.
