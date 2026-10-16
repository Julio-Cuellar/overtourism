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
| `docs/pruebas/ventas-caja-f2-f3.tdd.md` | Evidencia TDD de Ventas y Caja en memoria (F2 y F3, 2026-10-15): RED y GREEN, qué garantiza cada prueba, cobertura, JavaDoc y pendientes. |
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
| D-66 | Identidad es un módulo horizontal solo para el personal de los negocios. Los turistas tienen una cuenta aparte en Visitantes, sin vínculo con la del personal | ACEPTADA | 2026-10-13 | Del Mapa de Identidad v0.1 (respuesta del usuario). Número anotado el 2026-10-15 |
| D-67 | Acceso por código al teléfono, sin contraseñas, con token de sesión opaco (solo su hash en la base) y sin Spring Security completo en el MVP | ACEPTADA | 2026-10-13 | Ídem |
| D-68 | Una cuenta puede tener membresías en varios negocios, un rol por membresía y un negocio activo en la sesión | ACEPTADA | 2026-10-13 | Ídem |
| D-69 | Roles base fijos con plantilla por giro; los roles personalizados quedan en P2 | ACEPTADA | 2026-10-13 | Ídem |
| D-70 | Cada módulo declara sus permisos como datos (`modulo.recurso.accion`); Identidad los recibe por una interfaz de `compartido` y no importa a los módulos | PROPUESTA | 2026-10-13 | Se implementa con IDN-2, autorizada el 2026-10-15 |
| D-71 | El envío del código es simulado en desarrollo y demo; el canal real se decide aparte | ACEPTADA | 2026-10-13 | Precisada por D-77 |
| D-72 | Identidad se expone por REST y por API Java, sin GraphQL ni WebSocket propios | ACEPTADA | 2026-10-13 | |
| D-73 | Las acciones sensibles de Identidad usan **confirmación reforzada** (código nuevo ligado a la acción) en lugar de preparar → confirmar. Excepción a `CLAUDE.md` | ACEPTADA | 2026-10-15 | Dicho por el usuario: «sobre la D-73 si, ocuparemos confirmación reforzada». Se programa con IDN-3.2 (HTC-115) |
| D-74 | PIN rápido, roles personalizados, titularidad, cuentas de gobierno y cambio de teléfono quedan en P2 | ACEPTADA | 2026-10-13 | |
| D-75 | Identidad absorbe la autenticación de HTC-44 y reemplaza SG-04 con permisos por módulo | PROPUESTA | 2026-10-13 | |
| D-76 | Registro de personal con nombre, apellidos, correo, celular y aviso de privacidad. El **id, el correo y el celular** identifican a la persona; los tres sirven para pedir el código | ACEPTADA | 2026-10-15 | Pedido del usuario. El conjunto exacto de campos (sin fecha de nacimiento ni otros) y el flujo `POST /registro` son propuesta del asistente en el plan del 2026-10-15, que el usuario mandó ejecutar; el correo queda sin verificar hasta que exista un canal |
| D-77 | El código de verificación se escribe en la **consola** (registro del logger `identidad.canal.simulado`, solo en desarrollo y demo), detrás del puerto `CanalDeCodigos`, para conectar después el servicio de mensajería que se elija | ACEPTADA | 2026-10-15 | Pedido del usuario. Excepción acotada a SI-15: solo ese logger, con el teléfono enmascarado, y nunca con el perfil `produccion`. Precisada por D-85: el código llega al correo y la consola lo enmascara (`a***@example.com`) |
| D-78 | **Identidad no registra negocios: publica un evento** (`negocio.registro_solicitado`) que consume el módulo de negocios. Cada negocio tiene un dueño o administrador, encargado del alta; el módulo de negocios responde con `negocio.registrado` y Identidad lo consume para crear el negocio de referencia y la membresía `DUENO` de quien lo solicitó | ACEPTADA | 2026-10-15 | Dicho por el usuario: «no vas a registrar un negocio, en su lugar vas a generar un evento que sea para el registro de un negocio, esto con la finalidad de que lo consuma el modulo correspondiente». Que Identidad consuma `negocio.registrado` es interpretación del asistente; el `negocio_id` lo asigna Identidad al solicitar (UUIDv7) para no cambiar el sobre común (D-37 sigue propuesta) |
| D-79 | **Alta única de persona y negocio:** `POST /registro` acepta un bloque opcional `negocio {nombre, giro}`; en una llamada crea la persona, anota el pedido y publica `negocio.registro_solicitado`. `POST /registro/negocio` queda para quien ya tiene cuenta | ACEPTADA | 2026-10-15 | Plan presentado con `/ecc:plan` y confirmado por el usuario: «Sí, confirmo el plan con los valores por omisión: ejecuta la Etapa A con /ecc:tdd-workflow y deja Visitantes e invitaciones como diseño». **Sustituida en parte por D-84:** el alta de la persona ahora es en dos pasos (formulario y verificación del correo); el bloque `negocio` opcional y la activación implícita (D-80) siguen igual |
| D-80 | **Activación implícita:** si la sesión no eligió negocio y la persona tiene una sola membresía activa, ese es su negocio activo, sin cambiar de token. El `negocio_id` sale del servidor, no del cliente. Con dos o más negocios se elige con `PUT /auth/negocio-activo`; si llega un segundo negocio, la sesión deja de tener negocio activo (falla cerrado) | ACEPTADA | 2026-10-15 | Ídem |
| D-81 | **Turista (diseño, Visitantes no está autorizado):** sin cuenta por omisión; con cuenta, dos llamadas (pedir código y verificar, que crea la cuenta y recibe `aceptaAviso`), sin nombre, apellidos ni correo obligatorios; celular de cualquier país; rutas `/visitantes/auth/*` porque Identidad ocupa `/auth/*`; el cuestionario de intereses es opcional | ACEPTADA | 2026-10-15 | Ídem. Es diseño: no hay autorización para programar Visitantes |
| D-82 | Cuando Visitantes se autorice, el acceso por código (código, sesión, tope de solicitudes, canal) se extrae a `compartido.acceso`; cada módulo conserva sus tablas y cuentas (D-66) | ACEPTADA | 2026-10-15 | Ídem. Diseño |
| D-83 | **Personal invitado (diseño, IDN-3):** aceptar una invitación crea la cuenta y la membresía en un paso; el código de la invitación prueba el celular | ACEPTADA | 2026-10-15 | Ídem. Diseño |
| D-84 | **Registro en dos pasos con pre-registro (modelo de Clinerya):** `POST /registro` guarda el formulario como **pre-registro** (sin cuenta, sin código en la petición) y manda un código al **correo**; `POST /registro/verificacion {correo, codigo}` crea la cuenta con el correo ya verificado, borra el pre-registro, abre la sesión y, si venía negocio, publica `negocio.registro_solicitado`; `POST /registro/reenvio {correo}` renueva el código con respuesta genérica. El pre-registro y su código viven **15 minutos** en la base y se borran al verificar, al vencer y al reenviar. Sustituye el flujo de D-79 en que el código se pedía antes de registrarse | ACEPTADA | 2026-10-15 | Pedido del usuario (ver la entrada de autorización). El diseño sale de `clinerya-users` del repositorio Julio-Cuellar/Clinerya; las mejoras sobre él (hash del código, 5 intentos, topes por correo y por IP) son propuesta del asistente |
| D-85 | **Todos los códigos de acceso (registro e inicio de sesión) llegan al correo de la cuenta.** El celular queda como dato e identificador; con celular, correo o id se pide el código y siempre llega al correo. Ninguno de los tres revela si la cuenta existe (SI-02). Precisa D-76 y D-77 (la consola sigue siendo el canal hasta que se elija el servicio de mensajería) | ACEPTADA | 2026-10-15 | Valor por omisión del plan que el usuario mandó continuar; la alternativa (solo el registro por correo) se descartó por mantener dos canales y dos topes |
| D-86 | El pre-registro se guarda en Postgres desde ya: tabla `identidad.preregistro` (migración V023) y primer repositorio JDBC de Identidad, con prueba contra Postgres real. El resto de los repositorios sigue en memoria hasta IDN-1.1 | ACEPTADA | 2026-10-15 | Pedido del usuario («los códigos se guardan en base de datos durante 15 min»); lo demás de Identidad en memoria es propuesta del asistente por alcance |
| D-87 | **Negocios es un módulo horizontal con una sola entidad de negocio.** Los verticales agregan su extensión 1:1 por `negocio_id`, creada al escuchar `negocio.registrado`. El giro es un dato (tabla) y hay un giro principal por negocio | PROPUESTA | 2026-10-15 | Recomendación del asistente a la pregunta del usuario «¿módulo horizontal o una entidad por vertical?»; el usuario pidió definirla. Ver `docs/diseno/negocios.md` |
| D-88 | **Reparto con Identidad:** Negocios lleva la ficha laboral del personal; Identidad lleva el acceso (cuenta, membresía, rol, permisos). Negocios copia el acceso en solo lectura desde los eventos. Ninguno llama al otro; ningún dato personal viaja por el bus | ACEPTADA (lo de empleados con y sin acceso) / el resto PROPUESTA | 2026-10-15 | Del pedido «dentro de negocio habrá empleados o usuarios que cumplen el papel de staff, similar a las clínicas de Clinerya». El usuario confirmó el 2026-10-15: «habrá empleados que requieran acceso a la plataforma y otros que no» |
| D-89 | **El `negocio_id` lo sigue asignando Identidad** al pedir el alta (P-7) y Negocios lo adopta del sobre del evento. No cambia `SobreEvento` ni `negocio.registrado` v1 | ACEPTADA (delegada) | 2026-10-15 | El usuario respondió «lo que consideres mejor» a la pregunta 2; se adopta la opción que no cambia ningún contrato |
| D-90 | **Alta automática** si giro y nombre son válidos; si no, `negocio.registro_rechazado` (cierra P-17). La ficha del dueño nace `POR_COMPLETAR` al llegar su membresía | PROPUESTA | 2026-10-15 | |
| D-91 | **API móvil:** GraphQL para todo lo de la app; REST solo para el catálogo público de giros, la subida de imágenes y servirlas con URL firmada; una suscripción WebSocket en P1 (`altaDeNegocio`) con consulta de respaldo | PROPUESTA | 2026-10-15 | Del pedido «móvil, GraphQL y REST para lo mínimo, documenta los WebSockets» |
| D-92 | **Excepción a preparar → confirmar** (`CLAUDE.md`): las ediciones reversibles son mutaciones directas con `version`; baja de empleado y cierre del negocio sí pasan por preparar → confirmar. Las creaciones llevan `Idempotency-Key` | PROPUESTA | 2026-10-15 | Choca con la convención; requiere que el equipo la acepte |
| D-93 | **Ficha mínima:** sin nómina, sueldo, RFC, CURP, NSS ni cuenta bancaria del empleado; sin turnos ni asistencia. Los datos fiscales del negocio quedan para la ruta de formalización | PROPUESTA | 2026-10-15 | Clinerya los guarda; aquí se descartan |
| D-94 | **`giro` y `puesto_plantilla` son tablas**, no `enum`. Puesto laboral y rol de acceso son cosas distintas, con un rol sugerido por puesto; `subcategoria` también es tabla | ACEPTADA (delegada) | 2026-10-15 | El usuario respondió «lo que consideres mejor» (pregunta 3) y «como veas más óptimo» (pregunta 9). Hoy el giro es un `enum` de tres valores en Identidad |
| D-95 | **Sucursales:** el modelo admite varias, P1 trabaja con una (la principal, creada sola). El límite es una regla versionada | PROPUESTA | 2026-10-15 | |
| D-96 | **Cambios a Identidad** (sección 8.5 del diseño): escuchar `negocio.registro_rechazado`, `negocio.estado_cambiado`, `negocio.actualizado` y `negocio.empleado_cambiado` (baja); campo opcional `referencia_externa`; `SUSPENDIDO` en `negocio_ref`; permisos `negocios.*` en `RolesBase` | PROPUESTA | 2026-10-15 | No autorizados todavía |
| D-97 | **Giro principal más subcategorías como capacidades.** Un negocio tiene un giro (su categoría) y hasta 8 subcategorías; si elige una de otro giro se muestra como capacidad. Una subcategoría clasifica y no activa el vertical de otro giro (eso es P3) | ACEPTADA | 2026-10-15 | Respuesta 1 del usuario: «colocaremos un giro principal más capacidades, esto como subcategorías». Los límites (8, no activar vertical) son míos |
| D-98 | **La información pública del negocio vive en Negocios:** categorías, subcategorías, **reseñas**, ubicación, horario y estado. El Directorio, la app del turista y Rutas la **consultan** a este módulo y no guardan copia | ACEPTADA (el principio) / el detalle PROPUESTA | 2026-10-15 | Respuesta 1: «reseñas, categorías y subcategorías sí pertenecen al negocio, la app de los turistas consulta estos parámetros del módulo horizontal, así toda la info del negocio vive en su módulo». **Cambia lo dicho en D-35, D-38 y D-42** (Reseñas y Directorio como dueños) y obliga a revisar `rutas.md` y el mockup de la app del turista |
| D-99 | **Superficie pública de lectura** para la app del turista: sin sesión, solo sobre vistas de solo lectura (`v_negocio_publico`, `v_resena_publica`), con tipos propios, lista permitida de campos, tasa y topes. Las reseñas son P2 porque dependen de Visitantes y Pasaporte | PROPUESTA | 2026-10-15 | Mi traducción de D-98 a API; abre dos excepciones acotadas a SG-01 y SG-02 |
| D-100 | **Un negocio solo lo suspenden el dueño y la plataforma.** El dueño lo pausa y lo reanuda; la plataforma lo suspende por una ruta de administración y solo ella lo levanta. Un solo estado `SUSPENDIDO` con `suspendido_por` | ACEPTADA (quién) / el mecanismo PROPUESTA | 2026-10-15 | Respuesta 5: «por el momento solo por el dueño y la plataforma». Falta definir cómo se autentica el operador de la plataforma |
| D-101 | **Las imágenes viven en un bucket** en producción y **en disco local** en desarrollo, detrás del puerto `AlmacenDeArchivos`. La base solo guarda la clave del objeto | ACEPTADA | 2026-10-15 | Respuesta 6. Falta elegir el proveedor del bucket (S3 compatible) |
| D-102 | **El responsable de los datos del personal es el patrón;** la plataforma es la persona encargada. El dueño acepta esa responsabilidad antes de registrar a su primer empleado | ACEPTADA (quién) / el mecanismo PROPUESTA | 2026-10-15 | Respuesta 7: «el responsable de los datos del personal es del patrón». El texto de aceptación, el aviso de privacidad y el contrato de encargo siguen sin redactar |
| D-103 | **La transacción de cada caso de uso vive en la infraestructura, no en la capa de aplicación.** Las APIs se envuelven con `FronteraTransaccional` (Identidad) o `ManejadorTransaccional` (Clientes); los servicios siguen siendo Java puro y sin `@Transactional`. Un evento solo se publica dentro de una transacción (`PublicadorEnBus` falla si no hay una) y por eso queda en el outbox junto con el cambio. Un error de negocio confirma y se relanza; cualquier otro deshace | ACEPTADA | 2026-10-15 | Confirmada por el usuario. Reemplaza el `@Transactional` del plan: las clases de aplicación son `final` y no admiten proxy, y con `@Transactional` un error de negocio habría deshecho los intentos fallidos de código que se cuentan |
| D-104 | **Reseñas en P1, en modo demostración.** Para que quien visite el stand pueda usar la app, las reseñas entran a P1 sin cuenta de Visitantes ni sello: el autor es un id de dispositivo, con tasa y marca `DEMOSTRACION`. Apagado por omisión y prohibido en producción (la app no arranca con el perfil de producción y la bandera encendida). La versión con identidad real y habilitación por sello sigue en P2 | ACEPTADA (la decisión) / el mecanismo PROPUESTA | 2026-10-15 | Respuesta 4 de la v0.2: «en P1, colocaremos negocios de simulación en la presentación, y el objetivo es que quienes quieran interactuar con la app en el stan lo puedan hacer». Cambia D-99 (las reseñas ya no son solo P2). El id de dispositivo es falsificable: por eso solo en la demo |
| D-105 | **Negocios simulados para la presentación:** `es_simulado`, rótulo «Negocio de demostración», siembra por el mismo camino del alta (`negocio.registro_solicitado` sintético) y comando de restablecimiento del perfil `demo` | ACEPTADA (la decisión) / el mecanismo PROPUESTA | 2026-10-15 | Misma respuesta 4. Cumple la regla de `CLAUDE.md` de etiquetar todo dato simulado. Qué negocios se siembran sigue abierto (pregunta 2 de la sección 12.4 de `negocios.md`) |
| D-106 | **Dieta, accesibilidad, duración de la visita y costo medio son atributos opcionales del negocio desde P1,** declarados por él y no verificados. Rutas los lee de Negocios | ACEPTADA (la decisión) / el catálogo PROPUESTA | 2026-10-15 | Respuesta 5 de la v0.2: «atributos opcionales desde ahora en P1». Los catálogos de dieta y accesibilidad los armó el diseño y el equipo no los ha visto. Resuelve HR-4 de `rutas.md` en favor de Negocios |
| D-107 | **Negocios absorbe la disponibilidad como un nivel** (`LIBRE`, `MODERADA`, `LLENA`) que vence a las 3 horas. En P1 la declara el negocio a mano; en P2 la declaran los verticales con `negocio.disponibilidad_cambiada`. Publica `negocio.disponibilidad_actualizada`, que reemplaza a `directorio.disponibilidad_actualizada` (E-12) | ACEPTADA (delegada) | 2026-10-15 | Respuesta 3 de la v0.2: «como veas mejor». Extiende D-33 y D-98. Obliga a cambiar Rutas (cambio 9 de la sección 8.5) |
| D-108 | **La plataforma se gestiona en un servicio aparte.** Negocios solo ofrece las rutas `/admin`; la autenticación del operador se ignora por ahora y las rutas quedan apagadas en producción | ACEPTADA | 2026-10-15 | Respuesta 1 de la v0.2: «la plataforma se gestionará en otro servicio aparte, esto lo ignoramos por el momento». Cierra la pregunta de la cuenta `PLATAFORMA` |
| D-109 | **Subcategorías de miscelánea:** abarrotes, dulces típicos, artesanías, talavera, textiles, joyería, arte y galería, antigüedades, papelería y regalos, ropa y accesorios | ACEPTADA (delegada) | 2026-10-15 | Respuesta 2 de la v0.2: «busca, analiza y determina cuáles serán». Se cruzó con grupos del SCIAN (versión 2007, según un documento de INEGI) y con guías de compras de Puebla. **No** se verificó el SCIAN 2023 ni se cruzó con el DENUE |
| D-110 | **Sin moderación de reseñas por ahora.** La ruta para ocultar existe; en el futuro la usarán un modelo de IA y moderadores desde el servicio de administración | ACEPTADA | 2026-10-15 | Respuesta 8 de la v0.2: «por el momento nadie, en el futuro un modelo de IA y moderadores desde el servicio de administración». Riesgo: comentarios libres de desconocidos en el stand |
| D-111 | **El texto legal y la retención de la ficha del ex-empleado se ven después.** Ahora solo importa la funcionalidad: el texto de la responsabilidad del patrón es provisional y la retención es P2 | ACEPTADA | 2026-10-15 | Respuestas 6 y 7 de la v0.2: «esto posteriormente lo colocaremos, ahora solo importa funcionalidad» y «esto se ve en P2». Sigue siendo necesario antes de un piloto con personal real |
| D-112 | **El puerto de preparaciones lleva el negocio y el token se quema al vaciar.** `porHash`, `marcarUsada` y `vaciar` reciben `negocioId`; el hash del comando es canónico (ordena claves, ignora espacios); `marcarUsada` vacía el comando en la misma sentencia y `vaciar` de una preparación sin usar la marca como usada | ACEPTADA | 2026-10-15 | Confirmada por el usuario (la llamó D-104 al confirmar; se renumeró a D-112 porque otra sesión ya usó D-104 a D-111 para Negocios). La seguridad por fila no muestra la fila sin negocio; `jsonb` reordena el comando y rompía el hash; la base exige comando o uso. Efecto visible: una segunda confirmación de un token vencido o alterado da `TOKEN_USADO` y no `TOKEN_EXPIRADO` ni `COMANDO_CAMBIO` |
| D-76 | Ventas y Caja son dos módulos horizontales separados (`ventas`, `caja`). Ventas no conoce a Caja: el canal obtiene el turno con `CajaApi.turnoActivo` y pasa el `turno_id`. Caja escucha `venta.*` y llama a `VentasApi.resumenTurno` en el corte | PROPUESTA | 2026-10-14 | Evita un ciclo. `docs/diseno/ventas.md` y `caja.md`. La separación la pidió el usuario; el reparto de dependencias es mío |
| D-77 | Contrato de `venta.registrada` v1 (`venta_id`, `folio`, `serie`, `turno_id`, `cliente_id?`, `cupon_id?`, `subtotal`, `descuento`, `total`, `lineas[]`, `pagos[]`) y `venta.anulada` v1 (`venta_id`, `turno_id`, `motivo?`, `lineas[]`, `pagos[]`) | PROPUESTA | 2026-10-14 | Une lo que piden Inventario (§7.2, D-25), Clientes (D-48, D-65) y Caja. Compatible con `VentaRegistradaV1` y `VentaAnuladaV1` de Clientes (lector tolerante). Falta acordarlo con quien programa Inventario y Clientes |
| D-78 | Cupón: Ventas lo cotiza con `PasaporteApi.cotizarCupon` (nuevo, solo lectura) al preparar y lo canjea con `canjearCupon` al confirmar, en la misma transacción | PROPUESTA | 2026-10-14 | Precisa D-34. Revertir el canje al anular está abierto (VT-10) |
| D-79 | El folio de venta es consecutivo por negocio y serie y se asigna al confirmar | PROPUESTA | 2026-10-14 | Sin huecos por preparaciones que expiran |
| D-80 | Ventas no conoce boletos ni venta a granel: la unidad y si un producto admite fracción vienen de Catálogo; lo propio del museo vive en Museos | PROPUESTA | 2026-10-14 | Cumple R4 |
| D-81 | Preparar el corte de caja pone el turno `EN_CORTE` (única preparación que cambia un estado); descartar o expirar lo regresa a `ABIERTO` | PROPUESTA | 2026-10-14 | Excepción a «preparar no escribe» de Inventario §6.2, para que no entren ventas entre el conteo y la confirmación |
| D-82 | Anular una venta usa preparar → confirmar, con motivo y permiso de Identidad; una venta registrada no se edita | PROPUESTA | 2026-10-14 | Quién puede anular y hasta cuándo está abierto |
| D-83 | Todo el código se documenta con **JavaDoc**: cada tipo y método público, con `@param`, `@return`, fallos (`@throws` o código de error), el ID de función y las reglas que aplica (`VT-04`, `CJ-07`), y escala y redondeo en todo monto. `mvn javadoc:javadoc` sin advertencias entra en la verificación | ACEPTADA | 2026-10-14 | Dicho por el usuario: «el codigo debe de ser documentado con JavaDoc para no batallar a la hora de encontrar errores». Falta reflejarlo en la skill `negocio360-reglas` (§7) si el equipo lo pide |
| D-84 | El patrón preparar → confirmar genérico vive en `compartido.comandos` (`ServicioPreparacion`, `ManejadorComando`, `ResultadoComando` con `datos`); lo usan Ventas y Caja. Clientes conserva el suyo hasta que se migre | PROPUESTA | 2026-10-15 | Evita copiar 180 líneas por módulo. Agrega el ámbito de la preparación para contar ventas preparadas por turno (CJ-07) |
| D-85 | Al confirmar, si el cálculo deja de cuadrar porque el mundo cambió entre los dos pasos (precio, cupón, ventas del turno), la respuesta es `COMANDO_CAMBIO` y no el error de validación | PROPUESTA | 2026-10-15 | Encontrado al programar: sin esto salían `PAGOS_NO_CUADRAN` o `CORTE_REQUIERE_AUTORIZACION`, que confunden a la persona |
| D-86 | Ventas verifica que el turno siga abierto al preparar y al confirmar, preguntando por la interfaz `VerificadorDeTurno` de su propia API pública; Caja la implementa (`VerificadorDeTurnoDesdeCaja`, estado efectivo de D-81). Ventas sigue sin depender de Caja | PROPUESTA | 2026-10-15 | Revisión de seguridad A1: antes Ventas confiaba solo en el canal y una venta en un turno cerrado o en corte se registraba igual |
| D-87 | `canjearCupon` recibe el descuento que vio el cajero; si Pasaporte calcula otro, rechaza con `COMANDO_CAMBIO` sin canjear | PROPUESTA | 2026-10-15 | Revisión de calidad (medio 2): evita que un cupón quede gastado en una venta que no se registra. Cambia el contrato J3 que hay que acordar con el dueño de Pasaporte |
| D-88 | El consumidor de Caja marca un evento como procesado solo si lo aplicó. Si el turno no existe o cerró responde `SIN_TURNO_ABIERTO`, y si el evento no cumple el contrato o no viene de Ventas responde `DATOS_INVALIDOS`; en los dos casos el evento no se pierde. La anulación revierte lo que Caja registró, no los montos del evento | PROPUESTA | 2026-10-15 | Revisión de seguridad y calidad (A1, M3, M4): antes la venta se descartaba y quedaba marcada, y un método desconocido lanzaba excepción en cada reintento |
| D-89 | El frontend se hace en **Dart y Flutter**, pensado sobre todo como app móvil, en la carpeta `frontend/` junto a `backend/`. Habla con el servidor por GraphQL (y REST para el inicio de sesión) | ACEPTADA | 2026-10-15 | Lo pidió el usuario: «comienza a maquetar el front con dart y flutter ya que mayormente sera app movil». Reemplaza la opción Next.js de `CLAUDE.md` |
| D-90 | Mientras no exista el módulo de negocios, en la demo las sesiones sin negocio usan un **negocio de demostración** fijado en la configuración del servidor (`negocio360.demo.negocio-id`). El cliente nunca elige el negocio. Solo con `negocio360.demo.habilitada=true` y nunca con el perfil `produccion` | PROPUESTA | 2026-10-15 | F4: Identidad todavía no asigna negocio a la sesión, y sin negocio no hay contexto para Ventas ni Caja |

### Decisiones de producto abiertas (vienen de la sesión de mockups)
| Tema | Opciones | Estado |
|---|---|---|
| Modelo de IA para rutas | Gemini o ChatGPT | ABIERTA |
| Bot de chat (Telegram o WhatsApp) para locatarios | Se mantiene o se descarta | DESCARTADO por ahora (D-63, 2026-10-13) |
| Verticales reales en la demo | Solo Restaurantes real, Museos y Misceláneos como mockup (recomendado) | ABIERTA |
| Aplicación del turista y del locatario | Una PWA con dos modos, o dos aplicaciones | ABIERTA |
| Frontend y orquestación del bot | Next.js y n8n según `CLAUDE.md`, sin confirmar con el nuevo backend | Frontend: **Dart y Flutter, móvil primero** (D-89, ACEPTADA, 2026-10-15). Bot: descartado (D-63) |

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

### 2026-10-15 — Base de desarrollo levantada con credenciales `postgres` / `root` en el perfil `desarrollo`
**Pedido del usuario:** «instala la imagen de postgres, y en el perfil de desarrollador que las claves sean, username: postgres contraseña: root. También crea la base en postgres para continuar».

**Qué se hizo:**
1. Se descargó `postgres:17-alpine` y se levantó `negocio360-postgres` con `docker compose up -d --wait` (127.0.0.1:5433). La base `negocio360` se creó sola (`POSTGRES_DB`). Se verificó: PostgreSQL 17.11, `postgres` superusuario y `negocio360_app` sin superusuario ni `BYPASSRLS`.
2. `backend/.env` (local, ignorado por git) con dueño `postgres`/`root` y rol de la aplicación `negocio360_app`/`root`, para que lo lea `docker compose`.
3. `application-desarrollo.yml` ahora trae las credenciales de desarrollo: Flyway con `postgres`/`root` y la aplicación con `negocio360_app`/`root`. Fuera de ese perfil siguen sin valor por omisión y la aplicación no arranca.
4. Se arrancó con `-Dspring-boot.run.profiles=desarrollo`: Flyway aplicó las 4 migraciones (V001, V002, V010, V020) y la aplicación inició en 3.8 s.

**Decisión mía a confirmar:** la aplicación **no** entra como `postgres` sino como `negocio360_app`, porque un superusuario se salta la seguridad por fila y las pruebas de aislamiento dejarían de representar a producción. Si el equipo prefiere que la aplicación use `postgres`, es cambiar `spring.datasource.username` en el perfil.

**Cambia respecto a la regla anterior:** la entrada del 2026-10-14 decía «contraseñas solo en `.env`, nunca en el repositorio». Por pedido del usuario, las de **desarrollo** quedan en `application-desarrollo.yml`, que sí se sube a git. Son válidas solo contra la base local; nunca usarlas en un servidor.

**No verificado:** `mvn verify` no se volvió a correr; los módulos siguen con repositorios en memoria, así que la aplicación aún no escribe en la base.

**Archivos creados:** `backend/.env` (local, ignorado). **Modificados:** `backend/src/main/resources/application-desarrollo.yml`, `docs/BITACORA.md`.

### 2026-10-15 — Autorización de IDN-2 y del registro ampliado de Identidad (compuerta de código)
**Autorización escrita (citas textuales del usuario en la sesión):** «Si, construiremos IDN-2»; «sobre la D-73 si, ocuparemos confirmación reforzada»; y, tras ver el plan presentado con `/ecc:plan`: «ahora si /ecc:tdd-workflow ejecuta el plan que hiciste junto con la correción que te estoy anexando». La corrección es D-78: Identidad no registra negocios, publica un evento para que lo consuma el módulo correspondiente.

**Antecedente que debe quedar escrito:** una vez avancé con el plan sin que el usuario lo hubiera confirmado (leí una invocación de `/ecc:tdd-workflow` como un «sí»). El usuario me detuvo y se revirtió todo ese trabajo antes de esta autorización.

**Alcance que le doy (interpretación mía):**
- **Etapa 1:** registro ampliado (`POST /registro`), acceso por id, correo o celular, código en la consola (D-76, D-77), migración `V021`, y la colección de Postman con los campos.
- **Etapa 2:** IDN-2 (membresías, roles base y plantillas, catálogo de permisos, negocio activo, último dueño, anti escalada) y el flujo de eventos del alta de negocio (D-78).
- **No incluye:** confirmación reforzada ni invitaciones y gestión del equipo por REST (IDN-3.2 a 3.5, etapa 3); repositorios JDBC de Identidad (IDN-1.1); `desafioId`; Inventario. Cada uno necesita su autorización escrita.

**Cómo se trabaja:** rama `julio`, TDD (RED, GREEN, refactor, evidencia en `docs/pruebas/identidad-idn2.tdd.md`), sin commits ni pushes hasta que el usuario los pida, sin dependencias nuevas. Los cambios de esquema van en migraciones nuevas (`V021` en adelante, a continuación de `V020`: Flyway no aplica una migración con número menor que la última aplicada), porque `V010` ya está en un commit.

**Decisiones registradas hoy:** D-66 a D-75 (antes provisionales) y D-76 a D-78.

### 2026-10-15 — Identidad: registro ampliado, acceso por id, correo o celular, e IDN-2 con alta de negocio por eventos
**Pedido del usuario:** el de la entrada de autorización de arriba: ejecutar con `/ecc:tdd-workflow` el plan de registro ampliado, código en la consola e IDN-2, con la corrección D-78 (Identidad publica un evento en lugar de registrar el negocio) y una colección de Postman que pida los campos del registro.

**Qué se hizo** (detalle y evidencia en [`docs/pruebas/identidad-idn2.tdd.md`](pruebas/identidad-idn2.tdd.md)):
1. **Registro (D-76):** `POST /registro` con nombre, apellidos, correo, celular y aceptación del aviso de privacidad. El id, el correo y el celular sirven para pedir el código (`POST /auth/codigo` y `POST /auth/sesion` reciben `identificador`). **`POST /auth/sesion` ya no crea cuentas**: un celular sin cuenta recibe `REGISTRO_REQUERIDO` y el código no se gasta. El correo queda sin verificar. Migración `V021`.
2. **Código en la consola (D-77):** `[CANAL SIMULADO] Código de acceso para •••• 0001: 123456`, en el logger `identidad.canal.simulado`, solo con `identidad.canal-simulado.habilitado=true` y nunca con `produccion`. `GET /dev/canal/codigo` sigue existiendo. Cuando se elija la mensajería, es otra implementación de `CanalDeCodigos`.
3. **IDN-2:** permisos como datos (`compartido.seguridad`, cada módulo declara los suyos: `PermisosDeIdentidad`, `PermisosDeClientes`), 8 roles base y plantilla por giro (`RolesBase`), membresías, negocio activo (`PUT /auth/negocio-activo` emite otro token y revoca el anterior), `GET /roles`, `GET /permisos`, `GET /auth/yo` con rol y permisos, y la administración del equipo a nivel de servicio (`EquipoApi`: cambiar rol, suspender, reactivar, revocar, salir). Migración `V022`.
4. **Alta de negocio por eventos (D-78):** `POST /registro/negocio` responde 202 con el id que Identidad asigna y publica `negocio.registro_solicitado`; **no** crea el negocio. `EscuchaDeNegocios` consume `negocio.registrado` (lector tolerante `NegocioRegistradoV1`) y deja a quien lo pidió como DUENO; es idempotente. Mientras no exista el módulo de negocios, `POST /dev/negocios/{id}/registrado` (solo desarrollo) responde en su lugar.
5. **Postman:** colección de 44 solicitudes en 5 carpetas y dos entornos. El entorno `identidad.local` trae los campos vacíos (celular, nombre, apellidos, correo, nombre y giro del negocio): si falta alguno, la carpeta no sale. `identidad.ejemplo` trae datos inventados.

**Verificación hecha:** `mvn -B verify`: **1125 pruebas, 0 fallas**; cobertura de Identidad 99.5 % de líneas y 95.8 % de ramas. Se arrancó el backend con el perfil `desarrollo`, Flyway aplicó `V021` y `V022` a la base de Docker, y las 44 solicitudes de la colección respondieron lo esperado (estado y código de error) con un ejecutor en Python. Cinco mutaciones a mano comprobaron que las pruebas de las correcciones de las revisiones fallan si se deshacen.

**Cambios que afectan a quien arranque la aplicación:**
- La base de desarrollo ya está en la versión 22 de Flyway. Quien tenga una instancia en marcha con el código anterior debe reiniciarla.
- Las migraciones nuevas se numeraron `V021` y `V022`, **no** `V011`/`V012` como decía la autorización: Flyway rechaza una migración con número menor que la última aplicada (`V020`).
- El contrato de `/auth/codigo` y `/auth/sesion` cambió (`identificador` en lugar de `telefono`, sin `nombre`); `GET /auth/yo` y `SesionIniciada` traen más campos. La colección anterior de Postman quedó reemplazada.

**Decisiones mías que el equipo debe confirmar o corregir** (propuestas, no aceptadas):
- **P-7.** El `negocio_id` lo asigna Identidad al pedir el negocio y viaja en el sobre; así no se toca `SobreEvento` (D-37 sigue propuesta). Si el módulo de negocios quiere asignarlo él, cambia el contrato.
- **P-8.** `negocio.registrado` v1 trae en `datos`: `solicitado_por` (cuenta), `nombre` y `giro`. Hay que congelarlo con quien haga el módulo de negocios.
- **P-9.** Un `negocio.registrado` para un negocio que ya existe, o con una cuenta inexistente o inactiva, o con un giro o nombre que Identidad no reconoce, se **ignora sin error**.
- **P-10.** Un mismo celular, correo o id puede pedir código; el código siempre llega al celular de la cuenta; el tope de 3 cada 15 minutos lo comparten los tres.
- **P-13.** Para que `negocio.registrado` haga dueña a una cuenta, Identidad exige haberlo pedido: guarda el pedido (negocio y cuenta) y lo consume al aplicarlo. Si Identidad se reinicia entre el pedido y la respuesta, el pedido se pierde (hoy está en memoria; con Postgres hará falta una tabla, a agregar con IDN-1.1).
- **P-14.** Con correo o id, un código vencido o agotado se responde como `CODIGO_INVALIDO` y agotar el tope del celular no da error (se omite el envío), para no delatar si la cuenta existe. Un rechazo de `POST /registro` por correo ocupado cuenta como intento fallido del código.
- **P-15.** `POST /registro/negocio` tiene un tope de 3 pedidos por cuenta cada 15 minutos (reutiliza el valor de `solicitudes-por-telefono`).
- **P-16.** Una cuenta puede tener varias membresías en un negocio si las anteriores están revocadas (se invita de nuevo); a lo más una no revocada. Un disparador de `V022` impide que una revocada vuelva.
- **P-11.** Se cambió una prueba existente (`EsquemaClientesPostgresTest`) para que `identidad.negocio_ref` quede fuera de la regla «toda tabla con `negocio_id` tiene seguridad por fila».
- **P-12.** `membresia` lleva una política de seguridad por fila que deja ver las filas del negocio de la transacción o las de la cuenta de la transacción (`app.cuenta_id`), para que cada persona pueda listar sus negocios. Esa variable aún no la fija nadie: la fijarán los repositorios JDBC.

**No verificado / límites:** los `pm.test` de la colección no se ejecutaron (no hay Node ni Newman); los repositorios siguen en memoria; el escucha no se probó con el bus real porque no existe el módulo de negocios; no hay tope de solicitudes para `POST /registro/negocio`; el correo no se verifica; no se repitieron las pruebas de mutación a mano.

**Revisiones:** `ecc:java-reviewer` y `ecc:security-reviewer` revisaron el código (solo lectura). Se corrigieron 11 hallazgos con pruebas primero; el detalle, lo que quedó documentado y lo que descartaron está en [`docs/pruebas/identidad-idn2.tdd.md`](pruebas/identidad-idn2.tdd.md), sección «Resultado de las revisiones». Lo más importante: sondear correos con un código, hacer dueña a una cuenta con un evento no pedido y delatar cuentas por el tipo de error del código **ya no se pueden**.

**Lo que las revisiones dejaron para los repositorios JDBC (IDN-1.1), anotado también en el código:** bloquear la fila de `negocio_ref` (`FOR UPDATE`) en toda operación que quite o degrade a un dueño y repetir el conteo; renovar la sesión con un `UPDATE ... WHERE revocada_en IS NULL`; guardar el negocio y la membresía en una sola transacción; enviar el código fuera de la transacción; y una tabla para las solicitudes de negocio.

**Para decidir con el equipo (vienen de la revisión de seguridad):** el `desafioId` del acceso (un atacante con el celular, el correo o el id de una persona puede agotar sus 3 códigos o sus 5 intentos), un tope global de mensajes del canal real, un resolvedor de IP de confianza detrás de un proxy, un `Idempotency-Key` en `POST /registro/negocio`, y no usar el correo para invitaciones hasta verificarlo.

**Migraciones:** `V022` cambió después de aplicarse a la base de desarrollo; se deshizo solo ella ahí (tablas vacías) y Flyway la volvió a aplicar. Quien tenga una base propia con la `V022` anterior debe hacer lo mismo, o recrearla.

**Pendiente:** autorización escrita para la confirmación reforzada y la gestión del equipo por REST (IDN-3.2 a 3.5), para los repositorios JDBC de Identidad (IDN-1.1, necesarios para que `app.cuenta_id` y `app.negocio_id` se fijen) y para IDN-4 (filtro de entrada); decidir A1 y A2 de la entrada del 2026-10-14; marcar en Jira las historias IDN-2 si el equipo lo aprueba (no se tocó Jira); reiniciar la instancia del puerto 8080.

**Archivos creados:** `backend/src/main/java/mx/negocio360/{compartido/seguridad,clientes/seguridad,identidad/{contratos,eventos}}`, clases nuevas de `identidad` (`dominio`, `aplicacion`, `infraestructura`, `web`), `backend/src/main/resources/db/migration/V021…`, `V022…`, `backend/postman/identidad.ejemplo.postman_environment.json`, las pruebas correspondientes y `docs/pruebas/identidad-idn2.tdd.md`.
**Archivos modificados:** `docs/BITACORA.md`, `backend/postman/identidad.postman_collection.json` y `identidad.local.postman_environment.json`, el código y las pruebas de Identidad que cambiaron de contrato, `EsquemaClientesPostgresTest` y `SesionSql`.

### 2026-10-15 — Autorización de la Etapa A: alta única de persona y negocio, y activación implícita
**Autorización escrita (cita textual del usuario):** «Sí, confirmo el plan con los valores por omisión: ejecuta la Etapa A con /ecc:tdd-workflow y deja Visitantes e invitaciones como diseño.» Responde al plan «registro de turistas y de personal de negocios» presentado con `/ecc:plan` tras la queja de que el flujo era ineficiente (5 llamadas y una espera para un dueño nuevo).

**Alcance que le doy (interpretación mía):** solo Identidad: D-79 y D-80, la respuesta con `negociosPendientes`, y la colección de Postman. **No incluye** programar Visitantes (D-81, D-82) ni las invitaciones (D-83): quedan como diseño. Tampoco incluye el vencimiento de los pedidos pendientes ni el evento `negocio.registro_rechazado`, que son propuestas del plan para acordar con quien haga el módulo de negocios.

**Cómo se trabaja:** rama `julio`, TDD (RED, GREEN, refactor, evidencia en `docs/pruebas/identidad-registro-unico.tdd.md`), sin commits ni pushes, sin dependencias nuevas ni migraciones nuevas.

### 2026-10-15 — Etapa A: alta única de persona y negocio, y activación implícita (hecho)
**Pedido del usuario:** el de la entrada de autorización de arriba (D-79 y D-80). Evidencia: [`docs/pruebas/identidad-registro-unico.tdd.md`](pruebas/identidad-registro-unico.tdd.md).

**Qué se hizo:**
1. **D-79:** `POST /registro` acepta `negocio {nombre, giro}`; en una llamada crea la persona, anota el pedido y publica `negocio.registro_solicitado`. El negocio se valida antes de tocar el código. Un dueño nuevo pasa de 5 llamadas y una espera a **2 llamadas** más `GET /auth/yo`.
2. **`negociosPendientes`:** `GET /auth/yo` y las respuestas de registro, inicio de sesión y elegir negocio dicen qué negocios están pendientes (`PENDIENTE`) para que la app sepa «pendiente» o «listo» con una sola llamada.
3. **D-80:** el único negocio con membresía activa es el activo sin elegir ni cambiar de token; con dos o más hay que elegir, y si llega un segundo la sesión pierde el activo implícito.
4. **Postman:** 51 solicitudes en 6 carpetas, con el registro de persona y negocio en una llamada.

**Verificación hecha:** `mvn -B verify`: **1150 pruebas, 0 fallas**; Identidad 99.3 % de líneas y 96.0 % de ramas. Cuatro mutaciones a mano detectadas. Corrida real en frío: las 51 solicitudes responden lo esperado.

**Lo que la corrida real descubrió y se corrigió:** el simulador de desarrollo del módulo de negocios llamaba al escucha `@ApplicationModuleListener`, que corre en otro hilo (**Spring Modulith activa `@Async` por sí solo**), y respondía 204 antes de aplicar el aviso; la primera petición siguiente al arranque daba 404. Ahora aplica el aviso en el mismo hilo (prueba escrita antes, vista fallar). **Corrijo además una afirmación mía errónea** de la entrada de las revisiones: el proyecto sí tiene `@Async` activo vía Modulith.

**Decisiones mías que el equipo debe confirmar o corregir** (propuestas, no aceptadas):
- **P-17.** Los pedidos de negocio pendientes no vencen y no existe `negocio.registro_rechazado`. Propongo vencimiento de 7 días y un evento de rechazo, a acordar con quien haga el módulo de negocios.
- **P-18.** `POST /registro` con negocio no aplica el tope de 3 pedidos por cuenta (la cuenta es nueva).
- **P-19.** Cuando se construya Visitantes: rutas `/visitantes/auth/*`, celular de cualquier país y, con cuenta, dos llamadas sin nombre ni correo obligatorios (D-81). Es diseño, no hay autorización para programarlo.

**No verificado / límites:** los `pm.test` de la colección no se ejecutaron; los pedidos pendientes viven en memoria; el escucha asincrónico no se probó con el bus real; **esta etapa no tuvo revisión de agentes** aunque toca la autenticación (`identificar`).

**Archivos creados:** `docs/pruebas/identidad-registro-unico.tdd.md`, `NegocioPendienteVista`, `DevNegociosControllerTest`, `NegociosPendientesTest`.
**Archivos modificados:** `SolicitudDeNegocio`, `SolicitudesDeNegocio` y su versión en memoria, `SolicitudDeRegistro`, `SesionIniciada`, `PerfilVista`, `EquipoServicio`, `IdentidadServicio`, `AuthController`, `DevNegociosController`, `IdentidadConfiguracion`, `EscuchaDeNegocios`, las pruebas que cambiaron de contrato, `backend/postman/*` y `docs/BITACORA.md`.
**Pendiente:** autorización escrita para Visitantes (D-81, D-82), las invitaciones (D-83), los repositorios JDBC y el filtro de entrada; una revisión de agentes de esta etapa si el equipo la quiere.

### 2026-10-15 — Postman: la persona simulada viene en la colección (hecho)
**Pedido del usuario (cita textual):** «quiero que este tenga datos de un usuario simulado para solo dar click» (sobre `identidad.postman_collection.json`). Es un ajuste de la colección de la Etapa A, que ya estaba autorizada; no toca código del producto.

**Qué se hizo:** la colección trae como variables la persona simulada (Ana María López Pérez, «Fonda Doña Chela», giro `RESTAURANTE`). El paso 1.1 inventa un celular y un correo nuevos en cada corrida (una cuenta no se registra dos veces), salvo que el entorno ya traiga `telefono` o `correo`. El entorno `identidad.local` queda solo con `baseUrl` y se **eliminó** `identidad.ejemplo` (ya no hace falta; las entradas de arriba que lo mencionan son históricas). La carpeta 1 ya no se detiene por campos vacíos salvo que alguien borre nombre, apellidos o los datos del negocio.

**Verificación:** con mi ejecutor en Python, 51 de 51 en dos corridas seguidas sobre el mismo backend (el segundo registro no choca). Los `pm.test` siguen sin ejecutarse (no hay Node ni Newman).

**Aviso:** el `POST /auth/codigo` por celular tiene tope de 3 por 15 minutos; con el tope de IP por omisión (20) conviene reiniciar el backend si se repite la colección muchas veces seguidas.


### 2026-10-15 — Autorización: registro en dos pasos con pre-registro y código al correo
**Autorización escrita (cita textual del usuario):** «continua con el plan estipulado» (con `/ecc:tdd-workflow`), respondiendo al plan que presenté tras leer `clinerya-users` y que dejaba tres decisiones con valor por omisión. El flujo que pidió está en su mensaje anterior: «El usuario que quiere usar la plataforma para su negocio entra, hace el formulario de registro, se envia un codigo de confirmación a su correo electronico, este codigo por el momento pasa como un log en la consola y lo colocamos en otro endpoint que sea el de verificación, los codigos de verificación se guardan en base de datos durante 15 min y si no se ingresa este se borra», y «No se porque colocas código y telefono si no está registrado el usuario».

**Interpretación mía (no es una confirmación literal de cada punto):** tomo «continúa con el plan» como aceptar el plan completo **con los valores por omisión** de sus tres decisiones: D-85 (todos los códigos al correo), D-86 (tabla y JDBC del pre-registro ahora) y la verificación que abre sesión. Si el usuario quería otra opción, lo corrige y se ajusta.

**Alcance:** solo Identidad: pre-registro, verificación, reenvío, código de acceso al correo, V023, repositorio JDBC del pre-registro, limpieza de vencidos, la colección de Postman y las pruebas. **No incluye** Visitantes, invitaciones, los demás repositorios JDBC ni el filtro de entrada.

**Cómo se trabaja:** rama `julio`, TDD (RED, GREEN, refactor, evidencia en `docs/pruebas/identidad-preregistro.tdd.md`), sin commits ni pushes, sin dependencias nuevas.


### 2026-10-15 — Registro en dos pasos con pre-registro y código al correo (hecho)
**Pedido del usuario:** el de la entrada de autorización de arriba (D-84, D-85, D-86). Evidencia: [`docs/pruebas/identidad-preregistro.tdd.md`](pruebas/identidad-preregistro.tdd.md).

**Qué se hizo:**
1. **D-84, pre-registro (modelo de Clinerya):** `POST /registro` ya no pide código ni celular verificado: guarda el formulario como pre-registro y manda un código de 6 dígitos al correo (responde 202 con el correo y el vencimiento). `POST /registro/verificacion {correo, codigo}` crea la cuenta con el **correo ya verificado**, borra el pre-registro, abre la sesión y, si venía negocio, publica `negocio.registro_solicitado`. `POST /registro/reenvio {correo}` renueva el código con respuesta genérica. El pre-registro y su código valen **15 minutos**; se borran al verificar, al reenviar o al vencer (una limpieza programada corre cada minuto).
2. **D-85, todo código al correo:** el código de acceso se pide con celular, correo o id y siempre llega al correo de la cuenta; los tres responden igual exista o no la cuenta. Un código usado, vencido, agotado o incorrecto responde siempre `CODIGO_INVALIDO`: dejaron de existir `CODIGO_EXPIRADO`, `CODIGO_AGOTADO` y `REGISTRO_REQUERIDO`. La política renombró `solicitudes-por-telefono` a `solicitudes-por-correo` y suma `preregistro-vigencia` (15 min).
3. **D-86, Postgres:** migración **V023** (`identidad.preregistro`; `codigo_acceso.telefono` pasa a `destino`) y el primer repositorio JDBC de Identidad, `RepositorioPreregistrosJdbc`. El resto sigue en memoria.
4. **Postman:** 62 solicitudes en 6 carpetas. La carpeta 1 es el formulario, leer el código de la consola, verificar y el negocio; la persona simulada sigue en la colección y el celular y el correo se inventan nuevos en cada corrida.
5. **Pruebas:** se reemplazó `RegistrarCuentaTest` por `RegistroEnDosPasosTest` y se adaptaron las de acceso, canal, web y Postgres.

**Verificación hecha:** `mvn -B verify`: **1222 pruebas, 0 fallas**; Identidad 99.4 % de líneas y 96.2 % de ramas. 18 mutaciones a mano, todas detectadas. Colección de Postman: **62 de 62**, dos veces seguidas. Con la base real: el pre-registro quedó en `identidad.preregistro` con 15 minutos de vigencia y la limpieza lo borró solo, vencido, en unos 45 segundos. V023 quedó aplicada en la base de desarrollo.

**Honestidad sobre el orden:** el repositorio JDBC, V023 y la limpieza se escribieron **antes** que sus pruebas; se compensó con mutaciones que las apuntan, pero no nacieron en rojo.

**Decisiones mías que el equipo debe confirmar o corregir** (propuestas, no aceptadas):
- **P-20.** El celular **ya no se verifica**: es un dato y un identificador. Alguien podría registrar el celular de otra persona y ella no podría registrarse hasta que se resuelva (aviso `CUENTA_YA_EXISTE`). Propongo verificarlo cuando haya canal de SMS, o permitir una recuperación.
- **P-21.** `/registro` dice si el correo o el celular ya tienen cuenta **sin probar nada antes** (como Clinerya), lo que permite sondear. Lo acotan 3 formularios por correo y 20 pedidos por IP cada 15 minutos. Alternativa: responder igual y avisar solo por correo.
- **P-22.** Quien llena el formulario con un correo ajeno reemplaza el pre-registro de ese correo (hasta 3 veces cada 15 minutos): el código que la persona recibió deja de servir. Alternativa: no reemplazar uno vigente.
- **P-23.** Cuentas, códigos de acceso y sesiones siguen en memoria, y los códigos de acceso vencidos no tienen limpieza hasta IDN-1.1. V023 ya dejó `destino` listo para ese repositorio.
- **P-24.** El servicio es `synchronized` y ahora llama a Postgres dentro del bloqueo; sirve con un solo nodo y poco tráfico. IDN-1.1 lo resuelve con transacciones y `DELETE` condicional.
- **P-25.** `@EnableScheduling` quedó en la configuración de Identidad y vale para toda la aplicación.

**No verificado / límites:** los `pm.test` de la colección (no hay Node ni Newman); dos instancias de la aplicación corriendo la limpieza a la vez; el canal real de correo (hoy es la consola); esta etapa **no tuvo revisión de agentes**.

**Archivos creados:** `Preregistro`, `RepositorioPreregistros`, `RepositorioPreregistrosEnMemoria`, `RepositorioPreregistrosJdbc`, `LimpiezaDePreregistros`, `PreregistroCreado`, `V023__identidad_preregistro.sql`, las pruebas `PreregistroTest`, `RegistroEnDosPasosTest`, `RepositorioPreregistrosEnMemoriaTest`, `LimpiezaDePreregistrosTest`, `LimpiezaProgramadaTest`, `EsquemaPreregistroPostgresTest`, `RepositorioPreregistrosJdbcPostgresTest`, `FlujoHttp`, y `docs/pruebas/identidad-preregistro.tdd.md`.
**Archivos eliminados:** `RegistrarCuentaTest` (lo reemplaza `RegistroEnDosPasosTest`) y el entorno `identidad.ejemplo` de Postman.
**Archivos modificados:** `IdentidadServicio`, `IdentidadApi`, `SolicitudDeRegistro`, `Cuenta`, `Correo`, `CodigoAcceso`, `CodigoError`, `PlantillasIdentidad`, `PoliticaDeAcceso`, `PropiedadesDePolitica`, `CanalDeCodigos`, `CanalSimulado`, `RepositorioCodigos` y su versión en memoria, `AuthController`, `DevCanalController`, `ErroresIdentidadAdvice`, `IdentidadConfiguracion`, la colección y el entorno de Postman, y las pruebas de acceso, web y Postgres que tocaban el flujo.
**Pendiente:** decidir P-20 a P-25; autorización escrita para Visitantes (D-81, D-82), las invitaciones (D-83), los demás repositorios JDBC (IDN-1.1) y el filtro de entrada; el canal de correo real; una revisión de agentes del acceso.

### 2026-10-15 — Documentación de Identidad en OpenAPI (hecho)
**Pedido del usuario (cita textual):** «genera la documentación de este modulo en OpenApi». Es documentación: no toca código del producto ni agrega dependencias. Cumple la convención de `CLAUDE.md` de que cada operación pública tenga su contrato documentado (entrada, salida, códigos de error).

**Qué se hizo:** [`docs/api/identidad.openapi.yaml`](api/identidad.openapi.yaml), OpenAPI 3.1 escrito a mano en español, con las 15 operaciones REST de Identidad (13 públicas y 2 solo de desarrollo), 26 esquemas, los 21 códigos de error con su estado HTTP, los ejemplos, los topes por omisión y la autenticación Bearer. Incluye, como extensión `x-eventos`, el contrato de los dos eventos con el módulo de negocios (`negocio.registro_solicitado`, que publica Identidad, y `negocio.registrado`, que consume). Se hizo a mano y no con springdoc a propósito: una librería que genera la documentación sumaría una dependencia y una ruta nueva, y SG-12 mantiene cerrada la superficie (sin introspección ni esquemas públicos).

**Verificación hecha** (con scripts míos fuera del repositorio, en la carpeta temporal de la sesión; no hay Node ni un validador de OpenAPI en esta máquina):
- El YAML carga, no tiene claves repetidas, las 134 referencias resuelven y los 26 esquemas son esquemas JSON 2020-12 válidos.
- Los 38 ejemplos cumplen el esquema de su operación.
- Las 15 operaciones del documento son exactamente las de los controladores; los 21 códigos de error y su estado HTTP coinciden con `CodigoError` y `ErroresIdentidadAdvice`; las propiedades de los 18 DTO coinciden con los `record` de `AuthController` y `DevCanalController`.
- **Contraste con el backend en marcha:** las 62 respuestas reales de la colección de Postman (aciertos y errores: 202, 201, 401, 409, 422, 429, 415…) caben en el documento: estado previsto, cuerpo que cumple el esquema y cabeceras `Cache-Control: no-store` y `X-Content-Type-Options: nosniff`. Con un documento a propósito equivocado el contraste da 11 violaciones, así que el control sí detecta.
- **Un error que encontré en el propio borrador:** había declarado `/auth/sesion` dos veces (el `POST` y el `DELETE`) y la segunda pisaba a la primera; la comparación con los controladores lo detectó y quedó fusionado.

**Límites / decisiones mías que el equipo debe confirmar** (propuestas, no aceptadas):
- **P-26.** El documento se mantiene a mano y puede desfasarse del código. Propongo una prueba automática que haga fallar la compilación si una ruta, un código de error o un campo de DTO cambia sin cambiar el documento (es lo que hicieron mis scripts, pero dentro del repositorio). Requiere una dependencia de prueba para leer YAML y tu autorización.
- **P-27.** La gestión de equipo (suspender, revocar, cambiar rol, salir) existe en la API de Java pero no tiene endpoints REST; sus códigos de error figuran como reservados. Cuando se autorice IDN-3 hay que agregarlos al documento.
- El documento declara `info.version: 0.3.0` por mi cuenta; no hay política de versiones de la API acordada.
- No se generó una vista HTML (Redoc o Swagger UI): el archivo se puede abrir en cualquiera de ellos o importar en Postman.

**Archivos creados:** `docs/api/identidad.openapi.yaml`.
**Pendiente:** confirmar P-26 y P-27; documentar de la misma forma Clientes (GraphQL) cuando se quiera.


### 2026-10-15 — Diseño del módulo de Negocios, con su personal (borrador v0.1)
**Pedido del usuario (cita textual):** «Comienza a definir la entidad de negocio, recuerda que dentro de negocio habrán empleados o "usuarios" los cuales cumplen en papel de staff, algo similar a las clínicas en clinerya, solo que ya no son entidades de salud si no comercios, utiliza la skill /ecc:plan para generarlo, todo esto colócalo en un .md y en un mockup dentro de un artefacto con sus tablas de entidad relación, consultas de API, funciones, alcance, eventos que publica y consume, diagramas de secuencia, recuerda que está enfocado principalmente a dispositivos moviles, asi que debemos usar GraphQL y REST para lo mínimo necesario, al igual que si requiere websockets lo documentas». Es **diseño**: no hay código ni migraciones y **no hay autorización escrita para programarlo**; el plan por etapas (N0 a N5) espera confirmación.

**Qué se hizo:**
1. [`docs/diseno/negocios.md`](diseno/negocios.md): el diseño completo (14 secciones, 15 diagramas Mermaid): alcance P1/P2/P3, reglas R1 a R6, reparto de datos con Identidad, 14 tablas con su diagrama entidad-relación, 48 reglas de dominio con su prueba de aceptación, funciones y permisos (9 nuevos, 2 sensibles), 7 eventos que publica y 4 que consume, API móvil (18 vías GraphQL, 1 suscripción WebSocket en P1, 3 rutas REST), esquema GraphQL, 8 diagramas de secuencia, seguridad y plan por etapas.
2. [`docs/mockups/negocios.html`](mockups/negocios.html) y el artefacto privado «Mapa de Negocios» (https://claude.ai/artifact/EtekcPtyYKh1eNtWAdH2uQ): el mismo contenido, **construido desde el .md** (una sola fuente), más un mockup móvil interactivo de 10 pantallas con la operación GraphQL, REST o WebSocket de cada una y una simulación del aviso del alta (llega el evento, llega un rechazo, se pierde el socket).
3. Decisiones **D-87 a D-96**, todas PROPUESTA.

**Verificación hecha:** los 15 diagramas se analizaron con Mermaid 11.4.1 (todos válidos); el HTML se abrió en el navegador integrado: el mockup recorre sus 10 pantallas, el filtro y la búsqueda del equipo y la simulación del alta sin errores de consola; las cifras del resumen (14 tablas, 48 reglas, 18 vías GraphQL, 9 permisos, 7 eventos) se calculan del propio .md. **No verificado:** el artefacto publicado no se abrió en un navegador (la sesión del navegador integrado no tiene cuenta de claude.ai), el ancho de teléfono real de la página (el navegador integrado no aplicó el `viewport` a un archivo local) ni el contenido legal de la sección 11.

**Decisiones mías que el equipo debe confirmar o corregir** (propuestas, no aceptadas): las diez D-87 a D-96 de la tabla y las nueve preguntas abiertas de la sección 12. Las más delicadas: **D-92** (excepción a preparar → confirmar, que choca con `CLAUDE.md`), **D-96** (cambios a Identidad que hoy no están autorizados) y la pregunta 7 (quién es responsable de los datos personales del personal).

**Dependencia dura:** ningún resolvedor de Negocios puede salir antes del filtro de entrada (IDN-4) y de cerrar SG-01 a SG-10, el mismo bloqueo de Clientes.

**Archivos creados:** `docs/diseno/negocios.md`, `docs/mockups/negocios.html`.
**Pendiente:** confirmar D-87 a D-96 y las preguntas abiertas; autorización escrita para programar N0 en adelante; acordar con Identidad los cambios de la sección 8.5; decidir dónde se guardan las imágenes.


### 2026-10-15 — Negocios v0.2: respuestas del equipo incorporadas (diseño)
**Pedido del usuario (cita textual):** respuestas a las nueve preguntas abiertas de la v0.1: «1. Si, como dices, colocaremos un giro principal mas capacidades, esto como subcategorias, de igual forma de una vez que quede dentro lo de reseñas, categorias y subcategorias, estas si pertenecen al negocio, la app de los turistas (directorio) consulta estos parametros del modulo horizontal, asi toda la info del negocio vive en su modulo / 2. lo que consideres mejor / 3. lo que consideres mejor / 4. si, habra empleados que requieran acceso a la plataforma y otros que no / 5. por el momento solo por el dueño y la plataforma / 6. las imagenes se guardaran en un bucket, solo que en entorno de desarrollo se guardarán en disco local / 7. el responsable de los datos del personal es del patron / 8. identidad ya lo permite por lo tanto ya no es tan complicado / 9. como veas mas optimo». Sigue siendo **diseño**: sin código ni autorización para programar.

**Qué se hizo:**
1. [`docs/diseno/negocios.md`](diseno/negocios.md) pasa a **v0.2** (reescrito): 19 tablas (nuevas `subcategoria`, `negocio_subcategoria`, `resena`, `habilitacion_resena`, `resumen_resenas`), 77 reglas de dominio (grupos nuevos CA, PU y RS), 13 permisos `negocios.*` (el catálogo pasa de 16 a 29), 9 eventos que publica y 5 que consume, 24 vías GraphQL en P1 (22 del panel y 2 de la superficie pública), 6 rutas REST, 18 diagramas. Nuevo: superficie pública para la app del turista (9.7), `SUSPENDIDO` con pausa del dueño y suspensión de la plataforma, puerto `AlmacenDeArchivos` (disco y bucket), aceptación de la responsabilidad del patrón antes del primer empleado, pantalla «Mis negocios» (usa Identidad) y la sección «Respuestas del equipo» (12.2).
2. [`docs/mockups/negocios.html`](mockups/negocios.html) y el artefacto «Mapa de Negocios» (misma URL, versión 2): mockup móvil de **16 pantallas** (nuevas: mis negocios, categorías y publicar, vista del turista, reseñas, pausa y cierre) con las simulaciones de publicar sin ubicación, suspensión de la plataforma y aceptación de la responsabilidad.
3. Decisiones **D-97 a D-102** nuevas; **D-89 y D-94** pasan a ACEPTADA (delegada); **D-88** queda aceptada en lo de empleados con y sin acceso.

**Verificación hecha:** los 18 diagramas se analizaron con Mermaid 10.9.1 (todos válidos); el HTML se abrió en el navegador integrado y las 16 pantallas del mockup cargan con su panel de operaciones; se probó el flujo publicar → falla con `PUBLICACION_INCOMPLETA` → guardar ubicación y horario → publica, la suspensión de la plataforma y el rechazo de un empleado sin aceptar la responsabilidad. **No verificado:** el artefacto publicado no se abrió en un navegador, el ancho de teléfono real, ni el contenido legal de la sección 11.

**Decisiones mías que el equipo debe confirmar o corregir:** los detalles de D-98 (reseñas como P2, no P1), D-99, D-100 y D-102 (mecanismos), y estas **preguntas abiertas nuevas** (sección 12.3): (1) cómo se autentica el operador de la plataforma; (2) subcategorías de las misceláneas; (3) quién expone la disponibilidad en vivo (hoy la compone el Directorio, D-33); (4) si las reseñas son P1 o P2; (5) si dieta, accesibilidad, duración y costo medio son atributos del negocio (los pide Rutas); (6) quién redacta el texto de responsabilidad, el aviso de privacidad del personal y el contrato de encargo; (7) cuánto tiempo se conserva la ficha de un ex-empleado; (8) quién modera las reseñas.

**Efecto en otros documentos (sin tocar todavía):** D-98 contradice que Reseñas y Directorio sean dueños de sus datos (D-35, D-38, D-42, `docs/diseno/rutas.md`, `docs/mockups/app-turista.html`). Hay que reconciliarlos antes de diseñar Reseñas o de programar Rutas. Los cambios a Identidad (D-96, sección 8.5) siguen sin autorización.

**Archivos modificados:** `docs/diseno/negocios.md`, `docs/mockups/negocios.html`, `docs/BITACORA.md`.
**Pendiente:** responder las ocho preguntas nuevas; autorización escrita para programar N0 en adelante; reconciliar rutas.md y la app del turista con D-98; acordar con Identidad la sección 8.5; elegir proveedor de bucket.

### 2026-10-15 — Negocios v0.3: respuestas a las ocho preguntas de la v0.2 (diseño)
**Pedido del usuario (cita textual):** «para las preguntas abiertas / 1. la plataforma se gestionará en otro servicio aparte, esto lo ignoramos por el momento / 2. busca, analiza y determina cuales serán estas subcategorias / 3. como veas mejor / 4. en p1, colocaremos negocios de simulación en la presentación, y el objetivo es que quienes quieran interactuar con la app en el stan lo puedan hacer / 5. atributos opcionales desde ahora en p1 / 6. esto posteriormente lo colocaremos, ahora solo importa funcionalidad / 7. esto se ve en p2 / 8. por el momento nadie, en el futuro un modelo de ia y moderadores desde el servicio de administración». Sigue siendo **diseño**: sin código ni autorización para programar.

**Qué se hizo:**
1. [`docs/diseno/negocios.md`](diseno/negocios.md) pasa a **v0.3**: 22 tablas (nuevas `catalogo_atributo`, `negocio_atributo` y `disponibilidad`; `negocio` gana `es_simulado`, `duracion_visita_min` y `costo_medio_mxn`; `resena` gana `origen`), 95 reglas (grupos nuevos AT, DI y DM, y RS-11), 14 permisos (nuevo `negocios.disponibilidad.editar`; el catálogo pasa de 16 a 30), 10 eventos que publica (nuevo `negocio.disponibilidad_actualizada`) y 6 que consume (nuevo `negocio.disponibilidad_cambiada`, P2), 30 vías GraphQL en P1 (26 del panel y 4 públicas), 6 REST. **Las reseñas pasan a P1 en modo demostración** (autor = id de dispositivo, sin sello, con tasa y marca), con negocios simulados, restablecimiento de la demo y arranque prohibido en producción con la bandera encendida. Plan: N6 «Reseñas y demostración» (P1) y N7 «Reseñas con identidad real» (P2).
2. **Subcategorías de miscelánea determinadas** (D-109): diez, con su origen en la sección 5.2, buscadas en grupos del SCIAN de INEGI y en guías de compras de Puebla (El Parián, Analco, Barrio del Artista, calles 5 y 6 Oriente).
3. [`docs/mockups/negocios.html`](mockups/negocios.html) y el artefacto «Mapa de Negocios»: el mockup agrega disponibilidad en «Mi negocio», atributos en «Categorías», la vista del turista con atributos, disponibilidad y reseñas de demostración, y las reseñas del panel con responder y los botones de simulación del stand.
4. Decisiones **D-104 a D-111** nuevas (D-103 ya era de la transacción por la infraestructura, de otra sesión; por eso mis números saltan de D-102 a D-104). Respuestas 1, 4, 5, 6, 7 y 8 aceptadas; 2 y 3 delegadas.

**Verificación hecha:** los 18 diagramas se analizaron con Mermaid 10.9.1 (todos válidos); el HTML se abrió en el navegador integrado: las 16 pantallas cargan y se probaron reseñas del stand (promedio 4,0 → 4,3), responder, ocultar por la plataforma, restablecer, disponibilidad, atributos y la vista del turista, sin errores de JavaScript. **No verificado:** el artefacto publicado, el ancho de teléfono real, que los grupos del SCIAN de 2023 coincidan con los de 2007 usados, y el contenido legal.

**Decisiones mías que el equipo debe confirmar o corregir:** el mecanismo del modo demostración (id de dispositivo en la cabecera `X-Dispositivo-Id`, 5 reseñas por hora, bandera `negocios.demo.activo`); los catálogos de dieta y accesibilidad; el vencimiento de 3 horas de la disponibilidad; y estas **preguntas abiertas** (sección 12.4): (1) qué se hace con las reseñas de dispositivo cuando exista Visitantes; (2) qué doce negocios simulados se siembran; (3) si los catálogos de dieta y accesibilidad se ven bien; (4) si alguien vigilará las reseñas durante el stand.

**Efecto en otros documentos (sin tocar todavía):** además de lo de la v0.2 (D-98 contra `rutas.md` y `app-turista.html`), D-106 y D-107 contradicen que el Directorio declare dieta, accesibilidad y disponibilidad (HR-4 y E-12 de `rutas.md`). Los cambios a otros módulos (sección 8.5, ahora diez) siguen sin autorización.

**Archivos modificados:** `docs/diseno/negocios.md`, `docs/mockups/negocios.html`, `docs/BITACORA.md`.
**Pendiente:** responder las cuatro preguntas; autorización escrita para programar N0 en adelante; reconciliar `rutas.md` y la app del turista con D-98, D-106 y D-107; acordar con Identidad la sección 8.5; elegir proveedor de bucket.

### 2026-10-16 — Negocios v0.3: respuestas a las preguntas 2 y 4 (diseño)
**Pedido del usuario (cita textual):** «1. a que te refieres? / 2. si / 3. que catalogos? / 4. pues solo nosotros (equipo)». Sigue siendo **diseño**.

**Qué se hizo:** en [`docs/diseno/negocios.md`](diseno/negocios.md) (sección 12.4) quedan respondidas la 2 (los doce negocios simulados se aceptan, D-105) y la 4 (el equipo vigila y oculta a mano las reseñas con R6 durante el stand, D-110). La 1 (qué hacer con las reseñas de dispositivo cuando exista Visitantes) y la 3 (los catálogos de dieta y accesibilidad) quedan abiertas con valor por omisión: no se migran las reseñas y se usan los catálogos de la sección 5.2; el usuario pidió que se le explicaran y se hizo en la conversación.

**Verificación:** solo cambió texto de la sección 12.4; no se volvió a probar el mockup. **Archivos modificados:** `docs/diseno/negocios.md`, `docs/BITACORA.md`.
**Pendiente:** lo de la entrada anterior (autorización para programar N0, reconciliar `rutas.md` y la app del turista, acordar la sección 8.5 con Identidad, bucket).

### 2026-10-16 — Autorización: programar Negocios, etapas N0 a N3
**Pedido del usuario (cita textual):** «se ve bien, continua»; y, preguntado por el siguiente paso, eligió **«Autorizar programar N0 a N3»**. Con eso el usuario aprueba el diseño `docs/diseno/negocios.md` v0.3 (incluidos los catálogos de dieta y accesibilidad y el valor por omisión de la pregunta 1 de la sección 12.4) y autoriza **escribir código del módulo `negocios`**, con TDD, en estas etapas del plan de la sección 13:
- **N0** Contratos: módulo `negocios`, `PermisosDeNegocios`, contratos de eventos, esquema GraphQL.
- **N1** Alta por evento: migración, listener de `negocio.registro_solicitado`, `misAltasDeNegocio` y `altaDeNegocio`.
- **N2** Perfil, ubicación y horario.
- **N3** Categorías, atributos, disponibilidad, publicación y superficie pública (sin las reseñas).

**Fuera de lo autorizado:** N4 (equipo), N5 (imágenes, pausa y suspensión), N6 (reseñas y demostración), N7, los cambios a Identidad y a otros módulos de la sección 8.5 y cualquier commit o push. Las dependencias nuevas, si hicieran falta, se preguntan antes.
**Condición heredada:** ningún resolvedor de Negocios sale a producción antes del filtro de entrada (IDN-4) y de cerrar SG-01 a SG-10.

### 2026-10-16 — Negocios N0 a N3 programadas (hecho)
**Pedido del usuario:** la autorización de la entrada anterior («Autorizar programar N0 a N3»). Con TDD y contra Postgres de verdad. Sin commit ni push.

**Qué se programó** (`backend/src/main/java/mx/negocio360/negocios/`, módulo con `allowedDependencies = "compartido"`):
- **N0:** `PermisosDeNegocios` (14 permisos, 2 sensibles), contratos `NegocioRegistroSolicitadoV1` y `MembresiaAgregadaV1` (lectores tolerantes), `negocios.graphqls` (panel, superficie pública y la suscripción `altaDeNegocio`, sin `negocioId` en el panel y con tipos públicos propios), `LimitesPublicos`.
- **N1:** migración `V030` (esquema `negocios`: catálogos con su siembra, `negocio`, `sucursal`, horario, atributos, disponibilidad, `alta_negocio`, historial solo-inserción, `evento_procesado`, la vista pública y los roles `rol_negocios` y `rol_negocios_publico`), `AltaServicio` (alta automática, rechazo y los dos eventos, idempotente), listener `EscuchaDeIdentidad` y el centro de avisos `AvisosDeAlta`. **La cadena completa Identidad → Negocios → Identidad funciona sin el simulador de desarrollo** (`AltaConNegociosRealTest`).
- **N2:** `miNegocio`, `actualizarNegocio`, `sucursales`, `actualizarSucursal`, `horarioDeSucursal`, `definirHorario` y las excepciones, con completitud, historial sin valores y los eventos `actualizado`, `sucursal_cambiada` y `horario_cambiado`. Migración `V031` (versión propia del horario).
- **N3:** `definirSubcategorias`, `definirAtributos`, `definirDisponibilidad`, `publicarNegocio`, `negocioPublico`, `negociosPublicos` (texto sin acentos, giro, subcategorías, dieta, accesibilidad, abierto ahora, cercanía, cursor), `vistaDeNegocio` (J2), `GET /catalogos/categorias` con ETag y evento `negocio.disponibilidad_actualizada`. Migración `V032` (RLS forzada en todas las tablas con `negocio_id`).

**Verificación hecha:** la suite completa (1446 pruebas: 1445 de la corrida completa más la de la cadena) pasa sin fallas, con la base de desarrollo en Docker. Pruebas nuevas de Negocios: dominio, contratos, esquema GraphQL, servicios contra Postgres (permisos, versión, aislamiento por negocio y por cuenta, rol público sin acceso a tablas base, reloj movible para «abierto ahora» y la vigencia de la disponibilidad), el bus real con el outbox y el catálogo REST. **Comprobación de que las pruebas muerden:** rompí a propósito tres reglas (la vigencia de la disponibilidad, la validación de publicación y el traslape al dar la vuelta a la semana) y las pruebas fallaron cada vez; se restauró el código. Cobertura de líneas del módulo, según JaCoCo: 92 a 100 % por paquete; **no llega al 100 % que pide la definición de terminado** (13.2) y no se persiguió.

**Honestidad sobre el TDD:** en N0 y N2/N3 las pruebas se escribieron antes del código. En **N1** el servicio de alta y sus repositorios se escribieron antes de sus pruebas y se probaron justo después; no hubo un rojo previo.

**Pruebas existentes que hubo que cambiar, y por qué** (no se bajó ninguna aserción de seguridad):
- `EstructuraModulosTest` y `CatalogoDePermisosPlataformaTest`: ahora hay tres módulos y 14 permisos más (30 en el catálogo; los dos sensibles nuevos).
- `EsquemaGraphqlClientesTest`: solo carga `compartido` y `clientes`; con el esquema de Negocios mezclado dejaban de ser ciertas sus aserciones sobre «las consultas son exactamente estas» y «ningún tipo menciona el negocio».
- `EsquemaPostgresTest`: el esquema `negocios` es uno más.
- `ClientesDePuntaAPuntaPostgresTest` y `EventosPorOutboxPostgresTest`: contaban filas del outbox y cada módulo que escucha el bus deja su propia fila por evento; ahora cuentan eventos distintos y exigen que todas las filas estén completas.
- `NegociosControllerTest`: `/permisos` ahora trae 30. `/roles` sigue en 16: los roles base de Identidad no incluyen `negocios.*`.

**Decisiones de implementación mías** (el equipo puede corregirlas): una sola vista pública con listas en `jsonb` y el rol público por `SET LOCAL ROLE`; RLS forzada en todo y una política de solo lectura para el dueño de la vista; la búsqueda pública pagina con un cursor opaco por desplazamiento y calcula «abierto ahora» en el servidor después de leer (hasta 1 000 filas por consulta); `primeros` se recorta a 20 y el resto de lo excedido da `CONSULTA_INVALIDA`; tres códigos de error nuevos (`DESCRIPCION_INVALIDA`, `DIRECCION_INVALIDA`, `CONSULTA_INVALIDA`); `negocios.md` se ajustó a esto.

**Lo que NO está hecho ni verificado:**
- **Ningún resolvedor GraphQL ni la suscripción por WebSocket**: esperan el filtro de entrada (IDN-4), igual que Clientes. El esquema y `AvisosDeAlta` ya están.
- **Nadie puede usar Negocios con una sesión real todavía**: los roles base de Identidad no traen `negocios.*` (D-96, cambio 5, sin autorización).
- La tasa por IP del catálogo y de la superficie pública (SG-10) llega con IDN-4.
- No se arrancó la aplicación a mano ni se probó con Postman; la cadena se probó con MockMvc y el bus real.
- Las migraciones V030 a V032 quedaron aplicadas en la base de desarrollo local (por las pruebas). Quien tenga otra base debe correrlas con Flyway.
- Reseñas, demostración y siembra (N6), equipo (N4), imágenes, pausa y cierre (N5) y la parte de P2 siguen sin programar y sin autorización.
- El simulador `POST /dev/negocios/{id}/registrado` de Identidad sigue existiendo en desarrollo y ya no hace falta; quitarlo es un cambio de Identidad.

**Archivos:** nuevos `backend/src/main/java/mx/negocio360/negocios/**`, `backend/src/main/resources/graphql/negocios.graphqls`, `V030` a `V032` y sus pruebas bajo `backend/src/test/java/mx/negocio360/negocios/**` más `AltaConNegociosRealTest`; modificados los siete archivos de prueba de arriba, `docs/diseno/negocios.md`, `docs/mockups/negocios.html` y `docs/BITACORA.md`.
**Pendiente:** autorización para N4 (equipo), N5 y N6 (reseñas y demostración, que es lo que el stand necesita) o para el filtro de entrada IDN-4; agregar `negocios.*` a los roles de Identidad; decidir si se quita el simulador de Identidad.

### 2026-10-16 — Autorización: programar Negocios, etapa N6
**Pedido del usuario (cita textual):** «Autorizo programar N6: reseñas en modo demostración, negocios simulados y restablecimiento de la demo.» Autoriza **escribir código** de la etapa N6 del plan (sección 13 de `docs/diseno/negocios.md`), con TDD:
- Tablas `resena` y `resumen_resenas`, vista `v_resena_publica`, `PublicarResena` (preparar → confirmar, con el id de dispositivo del turista, RS-11), `responderResena`, `resenas` del panel, `resenasDeNegocio` público, ocultar una reseña (R6) y los eventos `negocio.resena_publicada` y `negocio.resena_respondida`.
- La bandera `negocios.demo.activo` y su prueba de arranque (DM-03), la siembra de negocios simulados por el camino del alta (DM-01, DM-02, DM-05) y el restablecimiento de la demo (DM-04).

**Fuera de lo autorizado:** N4 (equipo), N5 (imágenes, pausa y cierre), N7 (reseñas con identidad real), los resolvedores GraphQL y el filtro IDN-4, los cambios a Identidad y cualquier commit o push.

### 2026-10-16 — Negocios N6 programada: reseñas en demostración, negocios simulados y restablecimiento (hecho)
**Pedido del usuario:** la autorización de la entrada anterior («Autorizo programar N6: reseñas en modo demostración, negocios simulados y restablecimiento de la demo»). Con TDD y contra Postgres. Sin commit ni push.

**Qué se programó** (módulo `negocios`, migración `V033`):
- **Reseñas (RS-01 a RS-11):** `PublicarResena` por preparar → confirmar con el id de dispositivo (token de un solo uso, 5 minutos, solo lo confirma el mismo dispositivo, comentario vaciado al confirmar), límite de 5 preparaciones por hora por dispositivo, una reseña por dispositivo y negocio, resumen recalculado en la misma operación, respuesta única del negocio, `resenas` del panel con filtro y cursor, `resenasDeNegocio` público sin autor, calificación real en la ficha pública, y los eventos `negocio.resena_publicada` y `negocio.resena_respondida` sin comentario ni respuesta.
- **Moderación (R6, RS-05):** ocultar y restituir, con su controlador aparte: la ruta no existe sin un token de servicio configurado (`negocios.admin.token`, cabecera `X-Servicio-Admin`, comparado en tiempo constante) ni con el perfil `produccion`.
- **Demo (DM-01 a DM-05):** la bandera `negocios.demo.activo` apagada por omisión y la aplicación **no arranca con el perfil `produccion` si está encendida** (ni con un valor que no sea booleano); `DemostracionApi` siembra **doce negocios simulados** (5 restaurantes, 3 museos, 4 misceláneas, en el Centro, Analco, el Barrio del Artista y Cholula) por el mismo camino del alta y los casos de uso del panel, con 24 reseñas de demostración y algunas respuestas, y los marca `es_simulado`; `restablecer` borra las reseñas, la disponibilidad y los límites del stand y deja la siembra como estaba sin tocar ningún negocio real. Con el perfil `demo` se siembra al arrancar y existen `POST /demo/sembrar` y `POST /demo/restablecer` (nunca con `produccion`).
- Esquema GraphQL: `resenas`, `resenasDeNegocio`, `responderResena` y `publicarResena` como variante de `ComandoInput`.

**Verificación hecha:** la suite completa pasa: **1504 pruebas, 0 fallas**. Pruebas nuevas: dominio de reseñas, servicios contra Postgres (permisos, token ajeno, vencido, usado y en carrera, límite por hora y su reinicio, aislamiento, moderación, historial), la siembra y el restablecimiento, la bandera de producción, el controlador de administración (401, 404, 200) y el de la demo con y sin perfil. **Comprobación de que las pruebas muerden:** rompí a propósito el límite por hora, la verificación del dispositivo del token, el recálculo del resumen al moderar y el borrado de preparaciones al restablecer; las pruebas fallaron cada vez (la última no se detectaba y reforcé la prueba de restablecer); se restauró el código. Los 18 diagramas del diseño se volvieron a validar con Mermaid. Cobertura de líneas del módulo, según JaCoCo: 96 % (85 a 100 % por paquete); **no es el 100 % de la definición de terminado** (13.2).

**Pruebas existentes que hubo que cambiar** (sin bajar aserciones de seguridad): `EsquemaClientesPostgresTest` (las dos tablas nuevas sin `negocio_id` por negocio se anotan como excepción con su razón: `preparacion_resena` se busca por el hash de su token y `demostracion_negocio` es la lista global de la demo; se limitan por permisos), `OutboxPostgresTest` (con varios módulos escuchando el bus, «completado» significa que todas las filas del evento lo están; antes dependía del orden de las filas).

**Decisiones de implementación mías** (el equipo puede corregirlas): `preparacion_resena` propia en lugar de la `preparacion` general (el turista no tiene sesión ni negocio activo); la ruta de moderación lleva el negocio (`/admin/negocios/{negocioId}/resenas/{id}/ocultar`) porque la seguridad por fila no deja buscar una reseña sin él; un negocio no público responde `NEGOCIO_NO_DISPONIBLE` al reseñar, sin decir por qué; el panel muestra también las reseñas ocultas (como solo lectura); las reseñas sembradas se reinsertan desde el catálogo en cada restablecimiento; las preparaciones vencidas se borran al preparar la siguiente (con dos horas de margen). `negocios.md` se ajustó a esto (24 tablas).

**Lo que NO está hecho ni verificado:**
- **No hay resolvedores GraphQL ni el encabezado `X-Dispositivo-Id`**: esperan el filtro de entrada (IDN-4). Hoy las reseñas solo se pueden probar por el código del módulo, no desde una app. Tampoco hay tope por IP (solo por dispositivo).
- **Las 12 fichas simuladas, sus textos y sus reseñas son un borrador mío**: el equipo no los ha revisado (era la pregunta 2 de 12.4; aceptó la lista, no el contenido).
- **Los datos de la demo quedaron sembrados en la base de desarrollo local** por las pruebas (12 negocios publicados y rotulados como simulados). No se arrancó la aplicación con el perfil `demo` a mano.
- Los permisos `negocios.*` siguen sin estar en los roles de Identidad: ningún dueño real podría usar el panel de reseñas todavía (D-96, cambio 5).
- Sin moderación activa (D-110): el equipo oculta a mano con la ruta de administración durante el stand, y para eso hay que configurar `negocios.admin.token` en esa máquina.
- Sin commit. N4, N5, N7 y los resolvedores siguen sin autorización.

**Archivos:** nuevos `negocios/dominio/{Resena,ResumenCalificacion}`, `aplicacion/{ResenasServicio,ResenasPublicasServicio,AdministracionServicio,DemostracionServicio,CatalogoDeDemostracion,CursorDeDesplazamiento}`, puertos de reseñas, preparaciones, demostración y modo, los repositorios JDBC, `ModoDemostracionConfiguracion`, `DemostracionPerfilConfiguracion`, `web/{AdministracionController,DemostracionController,ErroresNegociosAdvice}`, `V033` y las pruebas correspondientes; modificados `negocios.graphqls`, `NegociosServicio`, `PublicoServicio`, `NegociosConfiguracion`, los dos tests de arriba, `docs/diseno/negocios.md`, `docs/mockups/negocios.html` y `docs/BITACORA.md`.
**Pendiente:** revisar el contenido de la demo; autorización para IDN-4 y los resolvedores (sin eso la demo no se ve en una app), o para N4 y N5; permisos de Negocios en los roles de Identidad.

### 2026-10-16 — Autorización: IDN-4 (filtro de entrada) y resolvedores GraphQL de Negocios
**Pedido del usuario (cita textual):** «Autorizo programar IDN-4 (el filtro de entrada con sesión, tasa por IP y el encabezado X-Dispositivo-Id) y los resolvedores GraphQL de Negocios.» Autoriza **escribir código**, con TDD, del filtro de entrada de `/graphql` (sesión, tasa por IP y por dispositivo, y el encabezado `X-Dispositivo-Id`) y de los resolvedores GraphQL del módulo Negocios para lo ya programado (N0 a N3 y N6), incluida la suscripción `altaDeNegocio`.

**Fuera de lo autorizado:** N4, N5 y N7, los resolvedores de Clientes, los cambios a los roles base de Identidad (`negocios.*` en `RolesBase`, D-96 cambio 5) salvo que el filtro de entrada los necesite de forma estricta (en ese caso se pregunta antes), y cualquier commit o push.

### 2026-10-16 — IDN-4: filtro de entrada de /graphql y resolvedores GraphQL de Negocios (hecho)
**Pedido del usuario:** la autorización de la entrada anterior (IDN-4 y los resolvedores de Negocios). Con TDD. Sin commit ni push.

**Qué se programó:**
- **Filtro de entrada** `FiltroDeEntrada` (paquete web de Identidad), solo para `/graphql`: tasa por IP (120/min, la de la conexión, sin confiar en `X-Forwarded-For`) y por dispositivo (60/min), solo `POST` con JSON y de tamaño conocido de hasta 16 KB (405, 415, 411, 413), `X-Dispositivo-Id` validado como UUID (400), y `Authorization: Bearer` autenticado con Identidad: **un token que no sirve es 401 y nunca se degrada a anónimo**; sin token la petición sigue anónima. Deja una `EntradaGraphQl` (cuenta, contexto del negocio, permisos, dispositivo) que sale de la sesión y nunca de un argumento. Todas las respuestas, también los rechazos, salen sin caché; el cuerpo de un rechazo es un código y un texto fijos.
- **En `compartido`:** `EntradaGraphQl`, `NoAutenticadoException` (`NO_AUTENTICADO`, `SIN_NEGOCIO_ACTIVO`), el interceptor que la lleva al contexto de GraphQL (sin entrada, la del contexto es anónima), los **topes de la consulta** medidos antes de ejecutar (profundidad 10, 10 alias, 200 campos; cuentan los fragmentos) y el resolvedor de errores comunes (sin sesión, sin permiso y argumentos ilegibles).
- **Resolvedores de Negocios** (`negocios.web`): todo el panel (`miNegocio`, sucursales, horario, perfil, subcategorías, atributos, disponibilidad, publicar, reseñas y responder), `misAltasDeNegocio` sin negocio activo, la superficie pública sin sesión (`negocioPublico`, `negociosPublicos`, `resenasDeNegocio`), publicar una reseña con `prepararComando` y `confirmarComando` (con el dispositivo del encabezado) y la suscripción `altaDeNegocio`. Los errores de negocio salen con su código, su texto fijo y, en `PUBLICACION_INCOMPLETA`, lo que falta. Cambio de esquema mío: `NegocioPublicoConexion` pasa a `{ negocios, pageInfo }`, como las reseñas.

**Verificación hecha:** la suite completa pasa: **1560 pruebas, 0 fallas**. Pruebas nuevas: el filtro (17), los topes de consulta (7), la entrada y el interceptor, y de punta a punta por HTTP con una Identidad simulada (permisos, aislamiento entre sesiones, perfil, ubicación, horario, categorías, atributos, disponibilidad, reseñas de ida y vuelta con el dispositivo, errores enmascarados, introspección apagada, GET y cuerpo enorme) y **con una sesión REAL de Identidad**: `misAltasDeNegocio` responde, lo público no pide sesión, un token falso es 401, y el panel falla cerrado con `PERMISO_DENEGADO`. La suscripción se probó **por SSE sobre el mismo `/graphql`** y como flujo. **Comprobación de que las pruebas muerden:** rompí a propósito el degradar un token malo a anónimo, el tope del cuerpo, el interceptor (que no fuera anónimo sin entrada), el tope de alias y el filtrado por cuenta de `misAltasDeNegocio`; las pruebas fallaron cada vez (el del interceptor no se detectaba y agregué su prueba). Cobertura de líneas: `compartido.graphql` 100 %, `identidad.web` 98 %, `negocios.web` 88 %.

**Lo que NO está hecho ni verificado:**
- **Un dueño real todavía no puede usar el panel.** Los roles base de Identidad no traen `negocios.*` (D-96, cambio 5, fuera de lo autorizado): con una sesión real, todo el panel responde `PERMISO_DENEGADO`. Es el siguiente paso obligado para que se vea algo desde una app. Lo público y las reseñas del turista sí funcionan sin eso.
- **El WebSocket no está activado** (SG-15): el resolvedor ya funciona por SSE, pero el transporte `graphql-transport-ws` con el token en `connection_init` pide `spring-boot-starter-websocket`, una dependencia que no se agregó sin preguntar.
- Del cierre de SG-01 a SG-10 faltan: el tiempo máximo de ejecución de una consulta (solo hay el de la base, 30 s), la tasa compartida entre varias instancias (el límite es en memoria) y un tope más bajo para operaciones sensibles.
- **`prepararComando` y `confirmarComando` hoy son solo de Negocios**: GraphQL admite un resolvedor por campo, así que cuando Clientes tenga los suyos habrá que repartirlos entre módulos. Clientes sigue sin resolvedores.
- No se arrancó la aplicación a mano ni se usó una app o Postman real; todo fue con MockMvc y el bus real. El filtro se registra con la configuración por omisión de producción (no hay implementación de `LimiteDeTasa` en producción todavía, igual que de los repositorios de Identidad).
- Sin commit. N4, N5 y N7 siguen sin autorización.

**Archivos:** nuevos `compartido/graphql/{EntradaGraphQl,InterceptorDeEntrada,LimitesDeConsultaGraphQl,ResolvedorDeErroresComunes,EntradaGraphQlConfiguracion}`, `compartido/seguridad/NoAutenticadoException`, `identidad/web/{FiltroDeEntrada,EntradaDeGraphQlConfiguracion}`, `negocios/web/{NegociosGraphQlController,PublicoGraphQlController,ResolvedorDeErroresDeNegocios}` y sus pruebas; modificados `negocios.graphqls`, `NegociosConfiguracion`, `AltaConNegociosRealTest`, `docs/diseno/negocios.md`, `docs/mockups/negocios.html` y `docs/BITACORA.md`.
**Pendiente:** autorización para agregar `negocios.*` a los roles base de Identidad (D-96, cambio 5) y, si se quiere WebSocket, la dependencia `spring-boot-starter-websocket`; revisar el contenido de la demo; N4 y N5 si se quieren.

### 2026-10-16 — Autorización: permisos `negocios.*` en los roles base de Identidad (D-96, cambio 5)
**Pedido del usuario (cita textual):** «Autorizo agregar los permisos negocios.* a los roles base de Identidad (D-96, cambio 5): DUENO con todos y el resto de los roles como propongas en negocios.md.» Autoriza **cambiar `RolesBase` de Identidad** (y la migración de siembra si la hay) con TDD: el DUENO recibe los catorce permisos `negocios.*` y los demás roles los de la matriz de la sección 7.7 de `docs/diseno/negocios.md`. Los permisos de empleados y de pausar o cerrar existen en el catálogo pero sus funciones (N4, N5) no están programadas ni autorizadas; solo se otorgan a los roles.

**Fuera de lo autorizado:** otros cambios a Identidad (invitaciones, simulador de desarrollo), N4, N5, N7 y cualquier commit o push.

### 2026-10-16 — Permisos `negocios.*` en los roles base de Identidad (hecho)
**Pedido del usuario:** la autorización de la entrada anterior (D-96, cambio 5). Con TDD (primero las pruebas, que fallaron por lo esperado: 10 rojas). Sin commit ni push.

**Qué cambió** (Identidad): `RolesBase` y la migración `V034`, que siembra los catorce permisos `negocios.*` en el catálogo y sus 37 filas de `rol_permiso`. La matriz es la de la sección 7.7 de `negocios.md`: **DUENO** los catorce (pasa de 16 a 30 permisos); **ADMINISTRADOR** todos menos pausar y cerrar el negocio; **GERENTE** perfil, horario, disponibilidad, ver empleados y las reseñas (ver y responder); **CAJERO, MESERO, COCINERO, TAQUILLERO y GUIA** solo ver el perfil. Lo sensible (`negocio.cerrar`) es solo del dueño; `empleado.baja` también lo tiene el administrador.

**Verificación hecha:** la suite completa pasa: **1562 pruebas, 0 fallas**. Hay una prueba que compara cada rol contra la matriz completa, otra que comprueba que lo sembrado en Postgres coincide con `RolesBase` y con lo que declara `PermisosDeNegocios` (descripción y sensibilidad), y la de punta a punta con una **sesión REAL de Identidad**: un dueño recién registrado ya consulta `miNegocio`, edita su perfil y declara su disponibilidad por `/graphql` (antes recibía `PERMISO_DENEGADO`).

**Cambios a pruebas existentes** (por el cambio de datos, sin bajar aserciones): los conteos de permisos (16 → 30), los permisos de cajero, mesero, cocinero, taquillero y guía, y los catálogos copiados de las pruebas aisladas de Identidad, que ahora incluyen los de Negocios.

**Lo que NO está hecho ni verificado:**
- La matriz para los roles que no son el dueño es la que **yo propuse** y el equipo no la ha revisado más allá de aceptar «como propongas». Hoy solo se puede probar con el dueño: las invitaciones (IDN-3) no existen, así que nadie más tiene sesión real.
- Los permisos de empleados, pausar y cerrar existen en los roles pero sus funciones (N4, N5) no están programadas.
- Los cambios 1 a 4, 6 y 8 a 10 de la sección 8.5 (Identidad consume más eventos de Negocios, `referencia_externa`, etc.) siguen sin autorización.
- Sigue pendiente: el transporte WebSocket (dependencia no agregada), el tiempo máximo de ejecución de una consulta y la tasa compartida entre instancias. Sin commit.

**Archivos:** `identidad/dominio/RolesBase.java`, `V034__identidad_permisos_de_negocios.sql`, `RolesBaseTest`, `RolesYPermisosTest`, `NegocioActivoTest`, `NegociosControllerTest`, `EsquemaEquipoPostgresTest`, `Escenario` y `PerfilProduccionTest` de Identidad, `AltaConNegociosRealTest`, `docs/diseno/negocios.md`, `docs/mockups/negocios.html` y `docs/BITACORA.md`.
**Pendiente:** revisar el contenido de la demo; decidir si se agrega el WebSocket; invitaciones (IDN-3), N4 y N5 si se quieren; commit.

### 2026-10-15 — Swagger UI en el perfil de desarrollo (hecho)
**Pedido del usuario (cita textual):** «agregalo» (la dependencia de springdoc, tras preguntar si Swagger estaba en el proyecto; propuesta mía: dejarla solo en el perfil `desarrollo`).

**Qué se hizo:**
- [`backend/pom.xml`](../backend/pom.xml): dependencia `org.springdoc:springdoc-openapi-starter-webmvc-ui` 2.8.9 (propiedad `springdoc.version`). Es la única dependencia nueva.
- [`application.yml`](../backend/src/main/resources/application.yml): `springdoc.api-docs.enabled` y `springdoc.swagger-ui.enabled` en `false` por omisión, para respetar SG-12 (superficie cerrada).
- [`application-desarrollo.yml`](../backend/src/main/resources/application-desarrollo.yml): ambos en `true`. Con `mvn spring-boot:run -Dspring-boot.run.profiles=desarrollo` quedan `/swagger-ui.html` y `/v3/api-docs`.
- [`ArranqueYSeguridadTest`](../backend/src/test/java/mx/negocio360/ArranqueYSeguridadTest.java): prueba nueva que exige 404 en `/swagger-ui.html`, `/swagger-ui/index.html` y `/v3/api-docs` sin el perfil.

**Verificación hecha:** `ArranqueYSeguridadTest` pasa (6 de 6). Con el perfil `desarrollo` (puerto 8089 para no tocar otro backend que ya ocupaba el 8080) las tres rutas responden 200 y `/v3/api-docs` trae OpenAPI 3.1 con las 14 rutas REST de Identidad. No corrí la suite completa ni el chequeo de cobertura de `mvn verify`.

**Límites / decisiones mías que el equipo debe confirmar** (propuestas, no aceptadas):
- **P-28.** Ahora hay dos contratos: el generado por springdoc, que refleja el código, y [`docs/api/identidad.openapi.yaml`](api/identidad.openapi.yaml), escrito a mano, con los 21 códigos de error, ejemplos y `x-eventos`. El generado no trae esa riqueza. Propongo que el escrito a mano siga siendo el contrato oficial y el generado sirva solo para probar en desarrollo.
- Solo cubre REST; GraphQL (Clientes) no aparece, porque la introspección sigue cerrada.
- "Try it out" no sabe de la autenticación Bearer salvo que se declare un esquema de seguridad en springdoc; no lo hice.
- Hay un proceso Java ajeno (PID 279954) que ocupa el puerto 8080 con el perfil `desarrollo` y no tiene esta dependencia; hay que reiniciarlo para ver Swagger ahí.

**Archivos modificados:** `backend/pom.xml`, `backend/src/main/resources/application.yml`, `backend/src/main/resources/application-desarrollo.yml`, `backend/src/test/java/mx/negocio360/ArranqueYSeguridadTest.java`.
**Pendiente:** confirmar P-28; decidir si se declara el esquema Bearer en springdoc.

### 2026-10-15 — Eventos de Identidad y Clientes por el bus de Spring y el outbox (hecho)
**Pedido del usuario (cita textual):** «haz que los eventos que se lanzan en estos modulos pasen por el outbox y por el bus de eventos de Spring Boot» y, tras ver el plan, «autorizo el plan, ejecuta la skill /ecc:tdd-workflow para ejecutarlo». **Autorización escrita para programar esto: sí.**

**Qué se hizo** (TDD: pruebas primero, rojas por clases inexistentes; luego el código):
- [`PublicadorEnBus`](../backend/src/main/java/mx/negocio360/compartido/eventos/PublicadorEnBus.java) (compartido): publica el `SobreEvento` en el bus de Spring. **Falla cerrado**: sin transacción activa lanza `IllegalStateException`, porque `@ApplicationModuleListener` solo recibe lo publicado dentro de una transacción y fuera de una el evento se perdería sin aviso.
- [`FronteraTransaccional`](../backend/src/main/java/mx/negocio360/compartido/transaccion/FronteraTransaccional.java) (compartido): proxy que vuelve cada llamada a una API una sola transacción. Un error de negocio declarado (en Identidad, `IdentidadException`) **confirma** y se relanza; cualquier otro error deshace.
- Identidad: `PublicadorEventosEnBus` (bean de todos los perfiles), `IdentidadFachada` (une `IdentidadApi` y `EquipoApi`) y `identidadApi` envuelto en la frontera. En desarrollo, `PublicadorEventosEnMemoria` ahora acepta un destino: reenvía al bus y solo guarda lo que el bus aceptó (es `@Primary`). `DevNegociosController` corre `alNegocioRegistrado` en una transacción.
- Clientes: `PublicadorEventosEnBus`, `ClientesConfiguracion` (el bean) y `ManejadorTransaccional` (decorador: `confirmar` en una transacción, `preparar` sin ella).
- `application.yml`: `spring.modulith.events.republish-outstanding-events-on-restart: true`.

**Decisión que difiere de lo autorizado (D-103, ACEPTADA el 2026-10-15; cita textual del usuario: «Confirmo D-103: la transacción queda en la infraestructura»). El plan autorizado decía `@Transactional`; la variante la propuse yo por la limitación técnica:** el plan proponía `@Transactional` en los servicios. No se pudo: `EquipoServicio`, `IdentidadServicio` y los manejadores son `final` y su capa es Java puro a propósito, y Spring no puede hacer proxy de una clase `final`. Se puso la transacción en la infraestructura. Con `@Transactional` los errores de negocio habrían deshecho también los intentos fallidos de código que se cuentan (SI-02/rate limit) en cuanto los repositorios sean de Postgres.

**Verificación hecha:** suite completa con `mvn verify`: **1291 pruebas, 0 fallos** (línea base antes: 1265, 0 fallos). 26 pruebas nuevas, entre ellas contra PostgreSQL real: `solicitarNegocio` y `membresia_agregada` quedan en `plataforma.event_publication` y el escucha las completa; `negocio.registrado` por el bus real llega a `EscuchaDeNegocios` y, entregado tres veces (mismo id y otro id), deja una sola membresía y una sola fila `membresia_agregada` (R3); el publicador de Clientes escribe en el outbox dentro de una transacción y rechaza hacerlo fuera; el sobre sobrevive a la serialización de ida y vuelta y se lee con campos nuevos (R5). Cobertura de las clases nuevas: entre 90 % y 100 %. Corregí `PerfilProduccionTest` (el contexto aislado necesitaba un `TransactionOperations`).

**Límites / no verificado:**
- **Clientes aún no tiene repositorios ni manejadores como beans**, así que `ManejadorTransaccional` está probado en unidad pero no cableado de punta a punta; hay que envolver cada manejador cuando exista el cableado (HTC-21 en adelante). `marcarUsada` y `vaciar` en su propia transacción (`REQUIRES_NEW`, contrato de `ServicioPreparacion`) tampoco se hicieron: no hay repositorio de preparaciones en Postgres.
- Los repositorios de Identidad (salvo pre-registros) siguen en memoria: la transacción protege el evento, no deshace el estado en memoria. La atomicidad real llega con IDN-1.1.
- El outbox de Spring Modulith es un registro **por listener**: un evento solo queda registrado si hay al menos un listener que lo reciba. Hoy `EscuchaDeNegocios` recibe todo `SobreEvento` (e ignora lo que no entiende), así que todos quedan registrados; si ese escucha cambia a un tipo concreto, los eventos sin consumidor dejarían de registrarse.
- El reenvío de pendientes al arrancar no se probó reiniciando la aplicación; se probó que la propiedad está activa.
- No creé commits.

**Archivos creados:** `compartido/eventos/PublicadorEnBus.java`, `compartido/transaccion/FronteraTransaccional.java`, `identidad/infraestructura/{PublicadorEventosEnBus,IdentidadFachada}.java`, `clientes/infraestructura/{PublicadorEventosEnBus,ManejadorTransaccional,ClientesConfiguracion}.java` y siete clases de prueba.
**Archivos modificados:** `IdentidadConfiguracion`, `IdentidadDesarrolloConfiguracion`, `PublicadorEventosEnMemoria`, `DevNegociosController`, `application.yml`, `PerfilProduccionTest`, `PublicadorEventosEnMemoriaTest`.
**Pendiente:** cablear los manejadores de Clientes con `ManejadorTransaccional` cuando existan sus repositorios; transacciones propias de `marcarUsada`/`vaciar`.

### 2026-10-15 — Autorización: repositorios JDBC de Clientes
**Autorización escrita (respuesta del usuario a la pregunta «¿Qué parte del plan sigue?»):** eligió «Repositorios JDBC de Clientes», que desbloquea lo pendiente del plan de eventos: cablear los manejadores con `ManejadorTransaccional` y dar a `marcarUsada` y `vaciar` su propia transacción. **Alcance autorizado:** los cuatro repositorios de Clientes en PostgreSQL y su cableado. **No incluye** los repositorios de Identidad (IDN-1.1), los resolvedores GraphQL ni Inventario.

### 2026-10-15 — Repositorios JDBC de Clientes y cableado transaccional (hecho)
**Pedido del usuario:** seguir el plan con `/ecc:tdd-workflow`; al preguntar qué parte seguía, «Repositorios JDBC de Clientes».

**Qué se hizo** (TDD; evidencia en [`docs/pruebas/clientes-jdbc.tdd.md`](pruebas/clientes-jdbc.tdd.md)):
- `backend/.../clientes/infraestructura/`: `RepositorioClientesJdbc`, `RepositorioAvisosJdbc`, `RepositorioConsentimientosJdbc`, `RepositorioPreparacionesJdbc`, `EjecutorEnNegocio` (fija `app.negocio_id` en cada transacción y falla si intenta cambiar de negocio) y `MomentosJdbc`.
- `ClientesConfiguracion` ahora cablea todo en todos los perfiles: repositorios, `ServicioPreparacion` con los tres manejadores envueltos en `ManejadorTransaccional`, y el bean `ClientesApi`. `marcarUsada` y `vaciar` corren en su propia transacción (`REQUIRES_NEW`), cumpliendo el contrato del Javadoc de `ServicioPreparacion`.
- `RepositorioPreparaciones` recibe `negocioId` en `porHash`, `marcarUsada` y `vaciar`; `ServicioPreparacion.hashDelComando` es canónico.

**Decisión D-112 (ACEPTADA el 2026-10-15; cita textual del usuario: «Confirmo D-104», que es como la llamé al proponerla; se renumeró a D-112 porque otra sesión ya había usado D-104 a D-111 para Negocios):** cuatro consecuencias de que la base imponga las reglas.
1. El puerto de preparaciones lleva el negocio (la seguridad por fila no muestra la fila sin él).
2. El hash del comando es canónico: `jsonb` reordena claves y agrega espacios, y con el hash del texto original todo token habría dado `COMANDO_CAMBIO`.
3. `vaciar` de una preparación sin usar la marca como usada, porque la base exige comando o uso. Una segunda confirmación de un token vencido o alterado da `TOKEN_USADO` en lugar de `TOKEN_EXPIRADO` o `COMANDO_CAMBIO`.
4. `marcarUsada` también vacía el comando en la misma sentencia.

**Verificación hecha:** `mvn -o -q verify`: **1318 pruebas, 0 fallos** (antes 1291). 27 pruebas nuevas contra PostgreSQL real con el rol de la aplicación (sin superusuario, sujeto a la seguridad por fila): aislamiento entre dos negocios, teléfono único parcial, bloqueo optimista, búsqueda sin acentos y paginada, avisos con cierre del vigente, historial de consentimientos, token de un solo uso, `marcarUsada` que sobrevive al rollback de fuera y el recorrido completo por `ClientesApi` (aviso, alta con dos consentimientos, búsqueda, eventos en el outbox, alta fallida sin cliente ni evento, fila de preparación vacía tras confirmar).

**Límites / no verificado:**
- **P-29.** Los resolvedores GraphQL de Clientes siguen sin existir (el arranque avisa de campos sin mapear): `ClientesApi` es un bean pero ningún controlador lo llama todavía.
- **P-30.** Falta la purga de `preparacion` a las 24 horas y de `evento_procesado` a los 90 días (HTC-38, SC-10); las preparaciones que nunca se confirman conservan su comando hasta entonces.
- **P-31.** `EjecutorEnNegocio` fija el negocio en cada llamada al repositorio; si un día se agrega un caso de uso que mezcle dos negocios, falla con `IllegalStateException` (es lo que se busca, pero hay que recordarlo).
- No se probó una carrera real de dos hilos sobre el mismo token ni la carrera de dos altas con el mismo teléfono: se garantizan por el conteo del `UPDATE` y por el `ON CONFLICT`.
- `ClientesConfiguracion` usa el `Clock` que declara Identidad (`@ConditionalOnMissingBean`); si Identidad no está, Clientes no arranca.
- No creé commits. `docs/pruebas/clientes-jdbc.tdd.md` es nuevo.

**Archivos creados:** seis clases en `clientes/infraestructura/` (repositorios, `EjecutorEnNegocio`, `MomentosJdbc`), `RepositoriosClientesJdbcPostgresTest`, `ClientesDePuntaAPuntaPostgresTest`, `docs/pruebas/clientes-jdbc.tdd.md`.
**Archivos modificados:** `ClientesConfiguracion`, `RepositorioPreparaciones`, `ServicioPreparacion`, `RepositorioPreparacionesEnMemoria` (doble de pruebas), `PreparacionTest`, `docs/BITACORA.md`.
**Pendiente:** resolvedores GraphQL de Clientes (CLI-I6); purga de preparaciones (HTC-38); listeners de Ventas (CLI-I3); repositorios JDBC de Identidad (IDN-1.1), que necesitan su propia autorización.
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

### 2026-10-15 — Ventas y Caja programados en memoria (F2 y F3)
**Pedido del usuario:** «Autorizo programar Ventas y Caja en la rama uriel» (entrada del 2026-10-14), con el flujo TDD.

**Qué se hizo:**
1. **`compartido.comandos`** (nuevo, D-84): preparar → confirmar genérico para Ventas y Caja. **`compartido.dinero.Redondeo`** (nuevo): la utilidad única de D-20.
2. **Módulo `ventas`**: `VentasApi`, `RegistrarVenta` y `AnularVenta` con preparar → confirmar, folios por negocio y serie, cupón cotizado y canjeado en la misma confirmación (D-78), eventos `venta.registrada` v1 y `venta.anulada` v1 con el contrato de D-77, y `resumenTurno`. Catálogo, Pasaporte y permisos son puertos con dobles en las pruebas. Solo depende de `compartido`.
3. **Módulo `caja`**: `CajaApi`, crear caja, abrir turno con fondo, entradas y retiros, corte con EN_CORTE al preparar (D-81) y conciliación contra Ventas (CJ-07), descartar corte, y `ConsumidorVentas` idempotente por evento y por venta, con marca de anulación previa. Depende de `compartido` y `ventas` en un solo sentido (D-76).
4. Todo el código nuevo lleva JavaDoc que cita funciones, reglas y códigos (D-83). `mvn javadoc:javadoc -Ddoclint=all` da 0 advertencias en el código nuevo.
5. `caja.md`: tres códigos nuevos (`CAJA_DUPLICADA`, `NOMBRE_CAJA_INVALIDO`, `TURNO_NO_ENCONTRADO`) y la precisión de CJ-02 (fondo 0.00 sin movimiento).

**Verificación hecha:** RED real dos veces (no compilaba por las clases faltantes). GREEN: Ventas 54/54, Caja 41/41 y compartido 32 nuevas. `mvn -B verify` sin las 10 clases que necesitan Docker: **805/805, BUILD SUCCESS, cobertura cumplida** (Ventas 87.8 %, Caja 92.8 %, `compartido.comandos` 94.0 % de líneas). Detalle en `docs/pruebas/ventas-caja-f2-f3.tdd.md`.

**No verificado:**
- `mvn -B verify` completo: 146 errores en las 10 clases de persistencia y de arranque porque **Docker Desktop está apagado**. Ningún error viene de Ventas ni de Caja, pero hay que repetirlo con Docker encendido.
- No hubo revisión de agentes de Java ni de seguridad, ni ronda de mutaciones.
- **No hay commits**: la máquina no tiene identidad de Git (`user.name` y `user.email`). Todo está sin confirmar en la rama local `uriel`.

**Decisiones:** D-84 y D-85 (PROPUESTA).

**Pendiente:** identidad de Git y commits; verify completo con Docker; revisión de agentes; JavaDoc de `DecimalPlano` (4 advertencias, anterior a esta etapa); adaptadores reales y JDBC (F5) y GraphQL (F4), ambos sin autorización; VT-10 y CJ-11 (P2).

**Archivos creados:** `backend/src/main/java/mx/negocio360/compartido/comandos/*` (8), `compartido/dinero/Redondeo.java`, `ventas/**` (30), `caja/**` (48); pruebas en `backend/src/test/java/mx/negocio360/{compartido/comandos,compartido/dinero,compartido/pruebas,ventas,caja}/**`; `docs/pruebas/ventas-caja-f2-f3.tdd.md`.
**Archivos modificados:** `backend/src/test/java/mx/negocio360/EstructuraModulosTest.java` (módulos `ventas` y `caja`), `docs/diseno/caja.md`, `docs/BITACORA.md`.

### 2026-10-15 — Correcciones de la revisión de seguridad y calidad de Ventas y Caja
**Pedido del usuario:** «realiza los 6 puntos como dices» (los seis puntos propuestos tras las revisiones de `ecc:security-reviewer` y `ecc:code-reviewer`). Dentro de la autorización del 2026-10-14; sin base de datos, GraphQL ni push.

**Qué se hizo (con pruebas primero):**
1. **Caja no pierde eventos** (D-88): un evento solo se marca como procesado si se aplicó. `SIN_TURNO_ABIERTO` y `DATOS_INVALIDOS` dejan el evento pendiente (`ResultadoAplicacion.completado()`). Exige origen `ventas`, método conocido y montos positivos, acotados y con 2 decimales. La anulación revierte los movimientos que Caja registró, no los montos del evento. Ya no lanza excepción en cada reintento.
2. **Ventas verifica el turno** (D-86): interfaz `ventas.VerificadorDeTurno` que Caja implementa con `VerificadorDeTurnoDesdeCaja` (estado efectivo de D-81). Ventas sigue sin depender de Caja (`EstructuraModulosTest` en verde).
3. **`turnoActivo` devuelve el estado efectivo** (`Turno.vigenteEn`): un corte vencido sale ABIERTO.
4. **Límites con código propio:** `DEMASIADAS_LINEAS`, `DEMASIADOS_PAGOS`, `TEXTO_DEMASIADO_LARGO`; cupón de más de 40 caracteres es `CUPON_INVALIDO` sin llamar a Pasaporte; `Redondeo.dineroValido` (tope `NUMERIC(14, 2)`) rechaza `1E+999999999` sin calcular, en Ventas y en Caja. Los límites se revisan antes de consultar a Caja, Catálogo o Pasaporte.
5. **El cupón no se gasta si la venta falla** (D-87): `canjear` recibe el descuento esperado y el puerto rechaza sin canjear.
6. **Detalles:** el resumen de la venta lleva cada línea con su precio (detecta cambios que se compensan); copias defensivas en `RegistrarVentaComando` y `CerrarTurnoComando`; `toString` sin cupón, cliente, actores, motivo ni nota; los textos de `CONCURRENCIA`, `ERROR_TEMPORAL` y `CORTE_EVENTOS_PENDIENTES` piden volver a empezar porque el token ya se gastó; pruebas renombradas (`folios_preparacionVencida_noConsumeFolio` ahora sí deja vencer el token; `eventoMalFormado_falla` pasó a `metodoDesconocido_datosInvalidos`).

**Verificación hecha:** RED en compilación (faltaban `VerificadorDeTurno` y su implementación) y luego RED en ejecución con firmas vacías: 37 fallas por la razón esperada. GREEN: `mvn -B verify` sin las 10 clases que necesitan Docker: **848/848, BUILD SUCCESS, cobertura cumplida** (Ventas 96.6 %, Caja 97.6 %, `compartido.comandos` 94.0 %, `compartido.dinero` 100 %). JavaDoc con `-Ddoclint=all`: 0 advertencias en los paquetes tocados.

**No verificado:** `verify` completo con Docker (sigue apagado). No se repitió la revisión de agentes sobre estas correcciones.

**Decisiones:** D-86, D-87 y D-88 (PROPUESTA). Siguen abiertas para el equipo: venta con total 0.00 (cupón del 100 %), quién puede retirar efectivo y preparar el corte, si quien autoriza una diferencia debe ser otra persona, y quitar el texto libre (`motivo`, `serie`) de los eventos (cambia D-77).

**Pendiente para F5 (base de datos):** bloqueos del turno al cerrar y retirar, una sola transacción para marcar el evento y aplicarlo, limpieza de preparaciones viejas, firma del comando guardado y cola de revisión para `DATOS_INVALIDOS`.

**Además:** documento para el equipo en lenguaje simple (qué se hizo, qué falta, dónde va el frontend y qué será funcional, demo o simulación en el concurso): https://claude.ai/code/artifact/b1efa956-2b71-48d5-b553-e81d2a9c6a67 (privado hasta que se comparta).

**Archivos creados:** `ventas/VerificadorDeTurno.java`, `caja/aplicacion/VerificadorDeTurnoDesdeCaja.java`; pruebas `RevisionVentasTest`, `RevisionCajaTest` y `ventas/fakes/TurnosEnMemoria`.
**Archivos modificados:** Ventas (manejador de venta, calculadora, plantillas, códigos, comandos, `Venta`, servicio, puerto de cupones), Caja (consumidor, resultado, servicio, API, `Turno`, validaciones, plantillas, códigos, comandos), `compartido/dinero/Redondeo`, dobles y pruebas existentes, `docs/diseno/ventas.md`, `docs/diseno/caja.md`, `docs/pruebas/ventas-caja-f2-f3.tdd.md` y esta Bitácora (D-86 a D-88).

### 2026-10-15 — Rama `uriel` unida a `main`; autorización de F4 y del frontend en Flutter
**Pedido del usuario (cita textual):** «los cambios qu ya hayas realizado y se encuentren bien y correctos [ ] Subir la rama uriel a GitHub y unirla a main, para que el equipo trabaje sobre Ventas y Caja. Dame opciones de datos publicos del gobierno del estado de puebla que se pueden colocar en la app, abrir las puertas de Ventas y Caja (fase F4) y crear la carpeta frontend/. que quede anotado en la Bitácora. Finalmente una vez creada la carpeta del front, comienza a maquetar el front con dart y flutter ya que mayormente sera app movil».

**Hecho:** `git push` de `uriel` y avance directo (fast-forward) de `main` en GitHub hasta `407124c`, sin conflictos porque `uriel` contenía a `main`. Se hizo sin pull request porque la máquina no tiene `gh`.

**Autorización escrita y alcance que le doy (interpretación mía):**
- **F4 de Ventas y Caja:** exponer sus comandos y consultas por GraphQL (D-55: `prepararComando` y `confirmarComando`, más consultas propias), encender los módulos al arrancar con Spring y permitir CORS para el frontend. Catálogo y Pasaporte siguen sin existir: se cargan productos y cupones de demostración **rotulados como simulados**. Los repositorios siguen en memoria (F5, JDBC, **no** está autorizada).
- **Carpeta `frontend/`** en la raíz, junto a `backend/`.
- **Frontend en Dart y Flutter**, pensado sobre todo para móvil. Esto cierra la decisión abierta «Frontend» de la sección 4 (antes Next.js sin confirmar): **D-89 ACEPTADA**.

### 2026-10-15 — F4 de Ventas y Caja por GraphQL y maqueta de la app en Flutter
**Pedido del usuario:** la entrada anterior (autorización de F4, carpeta `frontend/` y Flutter).

**Qué se hizo:**
1. **F4 (commit `3c9419f`, sin push):** módulo nuevo `canal` con `prepararComando` y `confirmarComando` (D-55) para Ventas y Caja; el token lleva al frente su módulo (`ventas.` o `caja.`). Consultas `venta`, `ventasDelTurno`, `cajas`, `turnoActivo`, `saldoTurno` y `cortes`. Los errores de dominio salen con `codigo`, `regla` y el texto fijo. El negocio sale de la sesión de Identidad (D-90 para la demo).
2. Ventas y Caja se encienden al arrancar con repositorios **en memoria** y el bus de Spring (sin outbox: F5). `canal.demo` simula Catálogo (6 productos), Pasaporte (cupones `PASAPORTE-20` y `PASAPORTE-50`, reutilizables en la demo) y permisos (toda cuenta puede anular y autorizar); todo va rotulado como simulado.
3. Todo detrás de `negocio360.demo.habilitada=true`, encendido en `application-desarrollo.yml` con CORS para `localhost`. Nunca con el perfil `produccion`.
4. Spring Modulith: interfaces con nombre `puertos`, `dominio`, `comandos` y `textos` en Ventas y Caja para que `canal` use solo lo expuesto.
5. **Frontend:** proyecto Flutter en `frontend/` (Flutter 3.44.6, Dart 3.12.2; Android, iOS y web), sin dependencias extra. Una app con dos modos: turista (explorar por intereses, ficha, Pasaporte y ruta del día) y negocio (abrir turno, cobrar con cupón y forma de pago, ventas y corte). Funciona con datos de ejemplo rotulados; aún no llama al servidor. Instrucciones en `frontend/README.md`.

**Verificación hecha:**
- Backend: RED en compilación (faltaban las clases de `canal`). GREEN: `CanalGraphQlTest` 7/7 (camino dorado por GraphQL sin base de datos) y `ContextoDeSesionTest` 3/3. `mvn -B verify` sin las 10 clases de Docker: **858/858, BUILD SUCCESS, cobertura cumplida**. `EstructuraModulosTest` acepta el módulo `canal`.
- Se ajustaron tres pruebas de `EsquemaGraphqlClientesTest` que suponían que todo el esquema era de Clientes. Siguen siendo listas exactas, así que una consulta o lista plana nueva sin anotar sigue haciéndolas fallar.
- `PostgresDePruebasInicializador` ahora se puede apagar con `negocio360.pruebas.sin-base=true`; las demás pruebas no cambian.
- Frontend: RED (no existían los archivos) y GREEN: `flutter analyze` sin avisos y `flutter test` **14/14** (carrito en centavos, caja simulada y cuatro recorridos de pantalla).

**No verificado:** el arranque real con `mvn spring-boot:run -Dspring-boot.run.profiles=desarrollo` y una llamada HTTP a `/graphql` (Docker apagado: la aplicación no arranca sin PostgreSQL). La app no se abrió en un emulador ni en el navegador; solo pruebas de widgets.

**Pendiente:** conectar la app al servidor (sesión REST + cliente GraphQL); paginar `cortes` antes de producción; outbox, JDBC y bloqueos (F5); reemplazar los negocios ficticios por datos reales.

**Fuentes de datos públicas de Puebla investigadas (para elegir hoy):**
- Gobierno del Estado: boletines trimestrales de turismo del CEIGEP/SEI (`sei.puebla.gob.mx/docs/boletines/turismo-*.pdf`; ocupación hotelera, llegada de turistas, derrama), indicadores del Observatorio Turístico de la Secretaría de Turismo (`sectur.puebla.gob.mx/observatorioturistico`), «Información estadística general» por municipio del SEI (CSV, replicada en datamx.io; última actualización 2019) y la Estrategia Estatal de Pueblos Mágicos (12 Pueblos Mágicos). El portal `datos.puebla.gob.mx` no respondió (DNS) el 2026-10-15.
- Federales con detalle de Puebla: DENUE de INEGI (negocios con ubicación y giro) y DataTur (monitoreo hotelero por destino).

**Decisiones:** D-89 (ACEPTADA), D-90 (PROPUESTA).

**Archivos creados:** `backend/src/main/java/mx/negocio360/canal/**`, `ventas/infraestructura/*`, `caja/infraestructura/*`, `compartido/comandos/PreparacionesEnMemoria.java`, `package-info` de puertos y dominio, `graphql/{ventas,caja,canal}.graphqls`, pruebas de `canal`; `frontend/**`.
**Archivos modificados:** comandos y plantillas de Ventas y Caja (anotaciones), `compartido.graphqls` (`Resultado.datos`), `application-desarrollo.yml`, `EstructuraModulosTest`, `EsquemaGraphqlClientesTest`, `PostgresDePruebasInicializador` y esta Bitácora.
### 2026-10-15 — Núcleo de Inventario a partir del análisis de Clinerya (plan, autorización y TDD)
**Pedido del usuario:** analizar `Julio-Cuellar/Clinerya` (sin clonarlo ni copiar código) y crear bajo nuestra arquitectura algunas de sus funciones.

**Lo que pasó, en orden (corrige un error mío):**
1. Analicé Clinerya por la API de GitHub (módulos de inventario, caja, contabilidad y núcleo). Le pregunté al usuario con qué función empezar y eligió «Núcleo de Inventario».
2. **Error mío:** tomé esa elección como autorización y empecé a escribir pruebas y código. El usuario me interrumpió: solo había pedido un plan. Una primera versión de esta entrada decía que su elección era la autorización; **no lo era**.
3. Se hizo el plan con `/ecc:plan`. El usuario respondió **«continua ejecuta la skill /ecc:tdd-workflow y continua»**. Esa es la autorización escrita, y cubre solo el alcance del plan: artículos, unidades, movimientos, saldo y CPP (U-01 a U-05, C-01 a C-08, A-01 a A-04).

**Qué se tomó de Clinerya (ideas, no código):** la fórmula del CPP al recibir mercancía, guardar el costo y la presentación usados en cada movimiento, y los tipos de movimiento. Se cambió: Clinerya bloquea el stock negativo (aquí se permite, A-01), no maneja saldo ≤ 0 al promediar (aquí C-03), usa `UUID.randomUUID`, `LocalDateTime.now`, setters y RabbitMQ (aquí UUIDv7, `Clock`, records inmutables y outbox).

**Resultado, con TDD:**
- **RED:** 11 clases de prueba sin código de producción: `test-compile` falló con 146 errores, todos por clases que faltaban.
- **GREEN:** `mvn -B verify` con JDK 21: **931 pruebas, 0 fallas** (824 antes + 107 nuevas), cobertura cumplida. Una prueba falló en la primera corrida por un error de mi doble de pruebas (`unica()` exigía una sola preparación y el `@BeforeEach` dejaba dos); se corrigió el doble, no el código de producción.
- Cobertura del módulo nuevo: `inventario.aplicacion` 95.7 % de instrucciones y 80.4 % de ramas, `inventario.dominio` 96.8 % y 89.3 %, `inventario.eventos` y `compartido.dinero` 100 %.
- Archivos: `inventario/` (dominio, aplicación, puertos, eventos e `InventarioApi`), `compartido/dinero/Redondeo.java`, pruebas en `src/test/.../inventario/` y `RedondeoTest`, y `EstructuraModulosTest` ahora exige el módulo `inventario`.

**Decisiones nuevas (propuestas, a confirmar por el equipo):**
- **D-76:** códigos de error que el diseño no trae: `NOMBRE_INVALIDO`, `NOMBRE_DUPLICADO`, `CATEGORIA_INVALIDA`, `UNIDAD_PROPIA_INVALIDA`, `FECHA_FUTURA`, `MOTIVO_REQUERIDO` y `CONTEO_SIN_DIFERENCIA`.
- **D-77:** un mínimo de cero significa «sin mínimo»: no dispara `bajo_minimo` ni `repuesto` (sí `saldo_negativo`). El diseño (A-02) no distingue ese caso.
- **D-78:** al confirmar se recalcula el resumen y, si cambió (por ejemplo, el saldo), falla con `COMANDO_CAMBIO`. Es el comportamiento que pide el diseño en 6.2, pero obliga a volver a preparar.
- **D-79:** los movimientos reciben UUIDv7 estrictamente crecientes dentro del proceso (`SecuenciaDeIds`) porque el kardex se ordena por id y dos UUIDv7 del mismo milisegundo salen en orden arbitrario.

**Límites / no verificado:**
- Repositorios solo en memoria: no hay migraciones, JDBC, seguridad por fila ni outbox real para Inventario. Nada de esto se verificó contra PostgreSQL.
- No hay GraphQL ni resolvedores; la demo no usa Inventario todavía.
- `ServicioPreparacion` es una copia del de Clientes (R1 impide compartirlo); mover a `compartido` es HTC-89.
- Fuera de alcance, sin autorización: lotes y FEFO, series, consumo por venta (K-01 a K-05).
- Sin commits ni pushes (convención del repo). No se tocó Jira.

**Revisión de código (`ecc:java-reviewer`, solo lectura) y qué se hizo:**
- **Corregido con TDD (RED real, 9 de 10 pruebas fallaron; luego 941 en verde):** A1 (el costo por gramo redondeado a 4 decimales falseaba el total: 25 kg a $18.55/kg daban $465.00 en lugar de $463.75; ahora el total sale de lo capturado), A2 (un decimal como `1E+3000000` tardaba más de 500 ms o agotaba memoria: ahora se compara el rango antes de operar), A3 (saldo, costo total y valor ya no pueden desbordar `NUMERIC(14,4)` ni `NUMERIC(14,2)`), M2 (se comparaba el resumen con una lectura y se escribía con otra; ahora es una sola) y M3 (los ids no quedaban ordenados con un reloj de microsegundos).
- **Decisión pendiente D-80:** para que el CPP y `valor` también sean exactos hacen falta más decimales en costos y CPP (el revisor propone 8, `NUMERIC(18,8)`). Contradice D-20 y el esquema de 5.1, así que no se cambió. Mientras tanto el total de cada entrada es exacto, pero el CPP puede diferir hasta media diezmilésima por gramo.
- **Pendiente para la fase de base de datos (no se hizo):** M1 (`marcarUsada` en una transacción aparte quema el token ante un error reintentable; el revisor propone una sola transacción por `confirmar` con reintento acotado de `CONCURRENCIA`); M4 (baja concurrente con una entrada puede dejar un artículo inactivo con saldo: bloquear la fila de saldo); M5 (`preparacion.comando` y `resumen` deben ser `TEXT`, no `jsonb`; traducir la violación del índice único de nombre a `NOMBRE_DUPLICADO`; escapar `%` y `_` en la búsqueda; persistir `unidad_articulo` en la misma transacción; filtrar por `negocio_id` en cada `UPDATE`; las `IllegalArgumentException` de paginación saldrían como 500); M6 (los tipos de `aplicacion` que usa `InventarioApi` hay que exponerlos con `@NamedInterface` antes de que Restaurantes lo use; Clientes tiene el mismo problema); M7 (la unidad base permite `kg` y `l`, lo que pierde precisión: ¿limitarla a `g`, `ml` y `pieza`?).
- **Bajos:** `ocurridoEn` sin cota inferior, sin purga de preparaciones vencidas, nombres de unidad con acentos descompuestos (NFD) y el mensaje de `COMANDO_CAMBIO` dice «El saldo cambió» aun cuando el JSON es ilegible.
- **Evidencia:** [`docs/pruebas/inventario-nucleo.tdd.md`](pruebas/inventario-nucleo.tdd.md). La verificación hecha es `mvn -B verify` con JDK 21; **no** se volvió a pasar el revisor sobre las correcciones.

### 2026-10-16 — Autorización: cerrar `/graphql` con Spring Security (IDN-4, SG-01 a SG-21)
**Autorización escrita (citas textuales del usuario, tras ver el plan v2 hecho con `/ecc:plan`):** «1. acepto / 2. no, debes agregar Spring Security desde ya / 3. No / 4. si las 3 nombradas», y luego «sí, acepto tus recomendaciones, empieza por la Fase 0».

**Qué cubre:** el filtro de entrada de `/graphql` (es IDN-4 / HTC-96, HTC-120 y HTC-121 de Identidad, que estaba sin autorización escrita): SG-01, 02, 03, 04, 06, 08, 09, 10, 11, 13, 14, 15, 16, 18, 19, 20 y 21. Valores iniciales aceptados: profundidad 7, complejidad 100, 10 alias, 5 campos raíz, cuerpo de 64 KB, 10 s, 60 por minuto por cuenta y 20 por minuto por IP sin autenticar. Decisiones aceptadas con las recomendaciones: WebSocket activado y protegido (el usuario no quiso dejarlo apagado); SG-19 con las operaciones sensibles **negadas por omisión** hasta que IDN-3.2 (confirmación reforzada, D-73) aporte la comprobación; SG-11 con lista de operaciones permitidas por hash, exigida solo en producción; SG-18 con enforcer, perfil de escaneo y Dependabot, sin workflow de CI.

**Dependencias autorizadas:** `spring-boot-starter-security`, `spring-security-test` (pruebas), `spring-boot-starter-websocket`, `maven-enforcer-plugin` y `dependency-check-maven` (perfil aparte).

**Fuera de alcance:** IDN-3.2 (la confirmación reforzada en sí), los repositorios JDBC de Identidad, resolvedores de Clientes o Inventario y cualquier cambio en Jira. Se trabaja en la rama `julio`, con TDD y sin commits ni pushes.

### 2026-10-16 — IDN-4, Fase 0: base de Spring Security (hecho)
**Qué se hizo, con TDD:**
- **RED:** `SeguridadHttpTest` (13 pruebas) falló como debía: `/graphql` respondía **200 a una petición anónima** y las rutas desconocidas respondían 404, sin cabeceras de seguridad.
- **GREEN:** dependencias `spring-boot-starter-security` y `spring-security-test` (pruebas); `SeguridadHttpConfiguracion` con una cadena sin estado, sin cookies, sin formulario ni autenticación básica, con CSRF desactivado (el token va en `Authorization: Bearer`, no hay credencial que el navegador adjunte solo) y **falla cerrado**: lo que ningún módulo declare público exige autenticación. Respuestas fijas `{"codigo":"NO_AUTENTICADO"}` (401, con `WWW-Authenticate: Bearer`) y `{"codigo":"PERMISO_DENEGADO"}` (403). La interfaz `RutasPublicas` (en `compartido`) hace que cada módulo declare las suyas, como `CatalogoDePermisos`; Identidad declara `/auth/**`, `/registro/**`, `/roles`, `/permisos` y `/cuenta`, y la configuración de desarrollo agrega `/dev/**` (que no existe en producción). Un `UserDetailsService` vacío evita que Spring cree un usuario con contraseña generada.
- **Resultado:** `mvn -B verify` con JDK 21: **1352 pruebas, 0 fallas** (1339 + 13), cobertura cumplida, sin contraseña generada en el registro.

**Cambio en una prueba existente:** `ArranqueYSeguridadTest` esperaba 404 en `/graphiql` y `/graphql/schema`; ahora esperan 401 (fallar cerrado). Siguen sin publicarse.

**Todavía no hay autenticación real:** hoy `/graphql` rechaza a todos con 401, incluso con un token válido, porque el filtro de Bearer es la Fase 1. Las rutas de Identidad siguen validando su propio token en `AuthController`.

**Límites / no verificado:** no se corrió contra un servidor real (solo MockMvc); el despacho de error está permitido para que un 404 o 405 no salgan como 401, y eso solo se ejercita con un servidor real. Sin commits ni pushes. Falta el resto de las fases (1 a 10); la Fase 1 es la autenticación (SG-01, SG-02).

### 2026-10-16 — IDN-4, Fase 1: autenticación de `/graphql` con la sesión de Identidad (hecho; SG-01 y SG-02)
**Qué se hizo, con TDD:**
- **RED:** 4 clases de prueba (29 pruebas) sin código de producción: 22 errores de compilación, todos por las clases que faltaban.
- **GREEN:** en `compartido.seguridad`: `Autenticador` (interfaz que implementa Identidad), `SesionDePeticion` (cuenta, negocio activo y permisos; sin negocio activo no hay contexto ni permisos, AZ-04), `SesionAuthentication`, `SesionActual` (lo que usarán los resolvedores) y `BearerTokenFilter`. En Identidad: `autenticadorDeIdentidad`, que traduce `SesionAutenticada` a la sesión neutral. `/graphql` exige sesión **con negocio activo**: sin token, 401; sesión válida sin negocio, 403.
- **El filtro:** lee `Authorization: Bearer` (sin importar mayúsculas), rechaza con el mismo 401 fijo una cabecera mal formada, repetida o con un token de más de 256 caracteres **sin consultar a Identidad**, y el mismo 401 para un token falso, vencido o revocado (AS-08). No toca las rutas públicas (validan su propio token). Guarda la sesión también en el atributo de la petición, o el despacho asíncrono de GraphQL llegaría sin sesión.
- **SG-02:** el resolvedor de prueba recibe el negocio y el actor de la sesión; una cabecera `X-Negocio-Id` o una variable `negocioId` del cliente se ignoran; pedir un argumento `negocioId` es un error de validación; una prueba recorre **todo** el esquema y exige que no exista `negocioId` en ningún tipo, campo ni argumento.
- **Integración con Identidad real:** se prueba con el flujo HTTP completo (registro, negocio, elección de negocio, cierre de sesión), no con dobles.
- **Mutación:** quitar la condición del negocio activo hace fallar `sesionSinNegocioActivo_responde403`; se restauró.
- **Resultado:** `mvn -B clean verify` con JDK 21: **1381 pruebas, 0 fallas** (1352 + 29), cobertura cumplida.

**Dos detalles de entorno de pruebas:** (1) Spring GraphQL busca los esquemas con `classpath:graphql/**`, que toma solo la primera raíz del classpath: en pruebas perdía los esquemas de los módulos. `src/test/resources/config/application.yml` ahora usa `classpath*:` (en producción no cambia nada). (2) El esquema de prueba (`contextoDePrueba`) vive en `src/test/resources/graphql-prueba/` y no en `graphql/`, porque `EsquemaGraphqlClientesTest` exige que `Query` tenga exactamente los campos de Clientes (SG-21) y no debía ver el de prueba. Si se mueve un recurso de prueba hay que correr `mvn clean`, o `target/` conserva el viejo.

**Límites / no verificado:** solo con MockMvc, no con un servidor real. Cada petición con un token válido renueva la expiración por inactividad de la sesión (es lo que hace `autenticar`). Si Identidad lanza una excepción inesperada al autenticar, hoy se propaga como error 500 con el manejo por omisión de Spring: se enmascara en la Fase 5 (SG-13). Todavía sin límite de tasa (Fase 4), por lo que un cliente puede probar tokens sin freno hasta entonces. Sin commits ni pushes.

**Siguiente:** Fase 2 (HTTP estricto y tamaño, SG-14 y SG-09).

### 2026-10-16 — IDN-4, Fase 2: puerta HTTP de `/graphql` — HTTP estricto y tamaño (hecho; SG-14 y SG-09)
**Qué se hizo, con TDD:**
- **RED:** 3 clases de prueba (50 pruebas) sin código de producción; el RED fue de compilación (`PeticionGraphqlEstrictaFilter` y `PropiedadesHttp` no existían).
- **GREEN:** `PeticionGraphqlEstrictaFilter`, que corre **antes** de autenticar (un cuerpo enorme no llega ni a Identidad) y rechaza con respuestas fijas: `Origin` que no esté permitido, 403; método distinto de POST, 405 con `Allow: POST`; tipo distinto de `application/json` en UTF-8, 415; cuerpo de más de 64 KB, 413; cuerpo que no sea un solo objeto JSON (lotes, arreglos, texto, vacío), 400. `PropiedadesHttp` (`seguridad.http.cuerpo-maximo-bytes`, por omisión 64 KB y como mucho 1 MiB; `seguridad.http.origenes-permitidos`) falla al arrancar con comodines, rutas o valores mal escritos. `CuerpoYaLeidoRequest` entrega el cuerpo ya acotado al resto de la cadena, que lo lee igual que antes.
- **Cuerpo acotado de verdad:** si el cliente anuncia más del tope, ni se lee; si no anuncia longitud (chunked), se lee a lo más tope + 1 bytes y se corta (una prueba comprueba que no se leen 2.6 MB).
- **Mutaciones:** leer `tope` en vez de `tope + 1` hace fallar la prueba del cuerpo sin longitud; dejar pasar cualquier origen hace fallar 3 pruebas. Se restauró el archivo.
- **Resultado:** `mvn -B clean verify` con JDK 21: **1431 pruebas, 0 fallas** (1381 + 50), cobertura cumplida.

**Decisiones de diseño que tomé (a confirmar):** los orígenes permitidos van por configuración y **por omisión no hay ninguno**: toda petición con `Origin` se rechaza (las apps nativas no lo envían). Solo se acepta el tipo `application/json`, no `application/graphql` ni `application/graphql+json`. Se rechaza también `Origin: null`.

**Limitación importante — CORS no está configurado:** un navegador que llame a la API desde **otro origen** (por ejemplo un panel web en otro dominio) primero manda un `OPTIONS` de verificación, y hoy responde 405: no funcionaría aunque su origen esté en la lista. La lista sirve para un panel servido desde el mismo origen que la API. Si el panel va en otro dominio hay que decidir y configurar CORS (SG-14); lo dejé cerrado a propósito. Pendiente: dónde se aloja el panel (PWA única o dos apps, decisión abierta).

**Otros límites / no verificado:** la puerta solo cubre `/graphql`; las rutas REST de Identidad (`/auth/**`, `/registro/**`) **no tienen tope de cuerpo** propio. La subida a WebSocket (un GET con `Upgrade` a `/graphql`) hoy recibiría 405 y se ajusta en la Fase 7 (SG-15). Solo MockMvc, no un servidor real: los límites propios de Tomcat no se ejercitaron. Sin commits ni pushes.

**Siguiente:** Fase 3 (profundidad, complejidad, alias, tamaño de la consulta y tiempo máximo; SG-06, SG-08 y SG-09).

### 2026-10-16 — IDN-4, Fase 3: límites de consulta de `/graphql` (hecho; SG-06, SG-08 y SG-09)
**Qué se hizo, con TDD:**
- **RED:** 4 clases de prueba nuevas sin código de producción (de compilación: faltaban `MedidorDeConsulta`, `MedidaDeConsulta` y `PropiedadesGraphql`) y una quinta de punta a punta por HTTP.
- **GREEN, en `compartido.graphql`:** `MedidorDeConsulta` (profundidad, complejidad, alias y campos de la raíz), `LimitesDeConsultaInstrumentation` (rechaza **antes de ejecutar** cualquier resolvedor con un error fijo: `DEMASIADOS_CAMPOS_RAIZ`, `DEMASIADOS_ALIAS`, `CONSULTA_MUY_PROFUNDA` o `CONSULTA_MUY_COSTOSA`, con `extensions.codigo`, sin decir cuánto vale el límite ni qué parte de la consulta lo causó), `LimitesDeSolicitudInterceptor` (`CONSULTA_MUY_LARGA` medida antes de analizar el texto, y `TIEMPO_AGOTADO` si el motor no termina; aplica también a WebSocket cuando exista) y `PropiedadesGraphql` (`seguridad.graphql.*`, falla al arrancar con valores fuera de rango). Valores iniciales aceptados: profundidad 7, complejidad 100, 10 alias, 5 campos raíz, 10 s; **consulta de 10 000 caracteres** (ver hallazgo).
- **Medidor propio, no los instrumentos de `graphql-java`:** estos expanden cada fragmento cada vez que se cita; una «bomba» de fragmentos (cada nivel cita dos veces al anterior) cuesta 2^n pasos con unos pocos KB de texto. El medidor mide cada fragmento una sola vez (tiempo lineal) y satura las cifras en 1 000 000. Mutación: sin esa memoria, la prueba de la bomba (40 fragmentos) excede los 500 ms.
- **Cada límite se prueba en su valor exacto (pasa) y en el siguiente (se rechaza):** profundidad 7/8, complejidad 100/101, alias 10/11, campos de la raíz 5/6, consulta 10 000/10 001 caracteres. Mutación: quitar `.timeout(...)` hace fallar la prueba del tiempo.
- **Resultado:** `mvn -B clean verify` con JDK 21: **1459 pruebas, 0 fallas** (1431 + 28), cobertura cumplida.

**Hallazgo — Jackson limita a 10 000 caracteres cualquier cadena JSON:** la consulta viaja como una cadena JSON y una de 10 001 caracteres se rechaza con un 400 sin cuerpo *antes* de llegar al motor (`StreamReadConstraints`). Mi tope inicial de 16 000 era inalcanzable por HTTP, así que se alineó a 10 000 (y es el máximo admitido de `consulta-maxima-caracteres`, con un comentario que explica el acoplamiento). **Consecuencia futura:** cualquier campo de texto largo que viaje en el JSON (por ejemplo el `textoIntegral` del aviso de privacidad de Clientes, o las `notas`) tropezará con el mismo tope. Hay que decidirlo al escribir esos resolvedores: subir el límite de lectura de Jackson o aceptar el máximo. No lo cambié.

**Cambio en una prueba de otra sesión (aditivo):** `FlujoHttp` (pruebas de Identidad) ganó `sesionConNegocio()`, que arma una sesión con negocio activo; mi prueba de la Fase 1 ahora lo usa en vez de repetir el armado.

**Límites / no verificado:**
- La complejidad cuenta 1 por campo; **no pondera el tamaño de página** (`primeros`). El tope del tamaño de página lo pone cada resolvedor (SG-07), y el límite de tasa llega en la Fase 4.
- Al vencer el tiempo máximo la respuesta sale a tiempo, pero **el motor no cancela un resolvedor que ya corre**: un resolvedor lento sigue ocupando su hilo hasta terminar.
- No se tocaron las opciones del analizador de `graphql-java` (`ParserOptions`, estáticas y globales); la protección del analizador es el tope de 10 000 caracteres.
- Los campos de prueba (`arbolDePrueba`, `lentoDePrueba`, `contextoDePrueba`) viven solo en `src/test/resources/graphql-prueba/`.
- Solo MockMvc y llamadas directas al manejador, no un servidor real. Sin commits ni pushes.

**Siguiente:** Fase 4 (límite de tasa por cuenta y por IP; SG-10).

### 2026-10-16 — IDN-4, Fase 4: límite de tasa de `/graphql` por cuenta y por IP (hecho; SG-10)
**Qué se hizo, con TDD:**
- **RED:** 5 clases de prueba (4 unitarias y una de integración con sesiones reales); el RED fue de compilación (faltaban `LimitadorDeTasa`, `PropiedadesTasa` y los dos filtros).
- **GREEN, en `compartido.seguridad`:** `LimitadorDeTasa` (una cubeta de fichas por clave; **aritmética entera**, porque con decimales `0.999 + 0.001` puede dar `0.9999999999999999` y negar justo en el borde; memoria acotada: al pasar el máximo de claves se descarta la menos usada), `LimiteDeTasaPorIpFilter`, `LimiteDeTasaPorCuentaFilter`, `PropiedadesTasa` (`seguridad.tasa.*`: 60 por minuto por cuenta, 20 fallos por minuto por IP, 100 000 claves) y la respuesta fija 429 `{"codigo":"DEMASIADAS_PETICIONES"}` con `Retry-After`.
- **Qué cuenta cada límite:** por IP se cuentan los **intentos sin sesión válida** (respuestas 401: sin token, o token falso, vencido o revocado), no todas las peticiones; lo que rechaza la puerta HTTP (405, 415, 413...) no gasta fichas. Por cuenta se cuenta cada petición con sesión válida. Una IP agotada recibe 429 sin que se consulte a Identidad.
- **Error que encontraron las pruebas de integración:** los unitarios pasaron desde el primer intento, pero con sesiones reales el límite de IP no contaba los intentos **sin token**. El 401 de ese caso lo escribe `ExceptionTranslationFilter`, que estaba *antes* del filtro de IP y recibe la excepción de autorización cuando el filtro ya salió, así que nunca veía la respuesta final. Con un token falso sí funcionaba (ese 401 lo escribe mi filtro de Bearer). Se corrigió registrando la puerta HTTP y el límite de IP *antes* de `ExceptionTranslationFilter`; `OrdenDeFiltrosTest` fija el orden real (puerta HTTP, IP, traducción de excepciones, Bearer, cuenta, autorización).
- **Mutaciones:** dejar de contar los 401 hace fallar 3 pruebas unitarias; volver al orden anterior hace fallar 3 de integración (incluida `OrdenDeFiltrosTest`). Se restauraron los archivos.
- **Resultado:** `mvn -B clean verify` con JDK 21: **1484 pruebas, 0 fallas** (1459 + 25), cobertura cumplida.

**Decisiones de diseño (a confirmar):**
- **IP compartida:** una IP bloqueada por acumular fallos recibe 429 también para quien traiga una sesión válida (no se puede saber antes de validarla). Con 20 por minuto solo ocurre ante un abuso, pero afecta a todos los que comparten esa IP (una oficina, una red celular). Hay una prueba que lo documenta.
- **La IP es la de la conexión**, no la de `X-Forwarded-For` (cualquiera puede escribir esa cabecera). Detrás de un proxy hay que lograr que la IP real llegue a la conexión; si no, todos los clientes parecerían una sola IP.

**Límites / no verificado:**
- Los contadores viven en la memoria de **cada instancia**: con varias instancias cada una cuenta aparte, y al reiniciar se reinician.
- **Tasa más baja por operación** (`cliente(id)`, `sugerirClientes`, la exportación; clientes.md SG-10) **no se hizo**: necesita las operaciones reales y llega con sus resolvedores. Hoy hay un solo tope por cuenta.
- El límite no se aplica todavía a la subida de WebSocket (Fase 7). Las rutas REST de Identidad (`/auth/**`, `/registro/**`) siguen con su propio límite interno, sin cambios.
- La configuración de pruebas (`src/test/resources/config/application.yml`) sube los topes a 10 000 para que las pruebas que comparten contexto e IP no se estorben; `TasaGraphqlConSesionTest` los baja a 5 y 3 en su propio contexto, y `PropiedadesTasaTest` fija los valores iniciales.
- Solo MockMvc, no un servidor real. Sin commits ni pushes.

**Siguiente:** Fase 5 (errores enmascarados y registro por operación; SG-13 y SG-20).

### 2026-10-16 — IDN-4, Fase 5: errores enmascarados y registro por operación en `/graphql` (hecho; SG-13 y SG-20)
**Qué se hizo, con TDD:**
- **RED:** 5 clases de prueba (33 pruebas) y campos de prueba que fallan a propósito (con una excepción cuyo mensaje trae SQL, una contraseña y un teléfono); el RED fue de compilación (faltaban `ErrorTraducido`, `TraductorDeErrores` y los traductores de los módulos).
- **SG-13, en `compartido.graphql`:**
  - `ResolvedorDeErroresGraphql`: sin permiso, `PERMISO_DENEGADO` (no dice qué permiso faltó); sin sesión, `NO_AUTENTICADO`; un error de negocio lo traduce el traductor de su módulo; todo lo demás, `ERROR_INTERNO` con una referencia al azar de 12 caracteres hexadecimales. Desenvuelve las excepciones asíncronas.
  - `TraductorDeErrores` (interfaz en `compartido`; cada módulo aporta su bean, como `CatalogoDePermisos`): `TraductorDeErroresDeClientes` y `TraductorDeErroresDeInventario` dan el código del diseño, el texto fijo de su plantilla, el id del recurso involucrado como extensión (`recursoId`, por ejemplo el cliente que ya tenía ese teléfono) y `reintentable: true` para `CONCURRENCIA` y `ERROR_TEMPORAL`. Nunca usan el mensaje de la excepción.
  - `EnmascaradoDeErroresInterceptor`: **lista blanca**. Solo pasan tal cual los errores con `extensions.codigo` en mayúsculas (los nuestros). Todo lo que produce el motor sale como `CONSULTA_INVALIDA` (sintaxis, validación, variables; sin posiciones ni texto) o `ERROR_INTERNO` con referencia (por ejemplo un null en un campo que no lo admite).
- **SG-20, `RegistroDeOperacionesInterceptor`:** una línea por operación en el logger `auditoria.graphql`: `cuenta=… negocio=… operacion=tipo:campo1,campo2 resultado=OK|ERROR errores=CODIGO1,CODIGO2 ms=n`. La operación sale de los **nombres de campo que el esquema ya validó** (no de los alias). **Nunca** se registran el texto de la consulta, las variables ni el nombre de la operación que manda el cliente (podría traer saltos de línea para falsificar renglones). Una consulta que no valida o que se rechaza por tamaño sale como `no_ejecutada`.
- **Registro de errores internos sin el mensaje:** la línea lleva la referencia, el campo, las clases de la cadena de causas y los primeros 8 marcos de la pila, **no** el mensaje de la excepción (puede traer SQL o valores de columnas). Costo: quien depure no ve el mensaje original; ve la pila y la clase.
- **Evidencia de que el enmascarado era necesario:** al apagarlo (mutación), 7 pruebas fallan: los mensajes del motor sí repiten el nombre del campo, del tipo, del fragmento y el valor de una variable. La introspección cerrada (SG-12) no servía de mucho si los errores lo contaban. Otra mutación (el registro incluye el mensaje de la excepción) hace fallar la prueba del registro. Cero apariciones de los datos de prueba en toda la salida de la corrida, incluidos los registros de Spring y de `graphql-java`.
- **Resultado:** `mvn -B clean verify` con JDK 21: **1517 pruebas, 0 fallas** (1484 + 33), cobertura cumplida.

**Cambio en una prueba de la Fase 3:** `LimitesGraphqlTest` comprobaba que un error de sintaxis conservaba su texto normal; ahora exige `CONSULTA_INVALIDA` y que no se confunda con los códigos de límites.

**Convención que dejé (a confirmar):** los errores de la plataforma llevan el código como mensaje (`PERMISO_DENEGADO`, `CONSULTA_INVALIDA`, `CONSULTA_MUY_PROFUNDA`…) y los de negocio llevan el texto de la plantilla del módulo; en ambos casos el contrato es `extensions.codigo`.

**Límites / decisiones pendientes:**
- **Lo que se rechaza antes de GraphQL no deja línea de registro:** 401, 403, 429, 405, 415, 413. Hoy nadie ve un intento de adivinar tokens. Registrarlos útilmente exige la IP, que es un dato personal; hay que decidir si se registra (y con qué retención) o si bastan contadores sin IP.
- Falta configurar a dónde va el logger `auditoria.graphql` (archivo o servicio de registros), su retención y quién puede leerlo (el pendiente B11 de retención de la bitácora de accesos).
- `IdentidadException` no tiene traductor porque Identidad hoy se expone por REST (su `ErroresIdentidadAdvice`); lo necesitará si Identidad se expone por GraphQL.
- Cuando existan suscripciones (Fase 7), el registro de una operación de larga vida y su `ms` tendrán que revisarse.
- Solo MockMvc y llamadas directas al manejador. Sin commits ni pushes.

**Siguiente:** Fase 6 (autorización por operación: tabla operación → permiso que impide arrancar si falta una, y operaciones sensibles negadas por omisión; SG-03, SG-04, SG-19 y SG-21).

### 2026-10-16 — IDN-4, Fase 6: autorización por operación en `/graphql` (hecho; SG-03, SG-04, SG-19 y SG-21)
**Qué se hizo, con TDD:**
- **RED:** 7 clases de prueba y campos de prueba nuevos; el RED fue de compilación (faltaban la tabla, el instrumento y la confirmación reforzada).
- **SG-21, el inventario de operaciones, en `compartido.graphql`:** cada módulo declara las operaciones de GraphQL que aporta con `OperacionesGraphql` (igual que sus permisos con `CatalogoDePermisos`), y `TablaDeOperaciones` las junta. **Al arrancar, `VerificadorDeOperaciones` compara la tabla con el esquema y, si no coinciden, la aplicación no arranca**, reportando todos los problemas juntos: un campo de `Query`, `Mutation` o `Subscription` que nadie declaró; una declaración de un campo o tipo que no existe; un permiso que ningún módulo declara; una variante de comando sin declarar o de más; una declaración doble. Las operaciones de la plataforma (`prepararComando`, `confirmarComando`) las declara `OperacionesDePlataforma`; Clientes declara las suyas en `OperacionesDeClientes`.
- **SG-03 y SG-04, la puerta (`AutorizacionDeOperacionesInstrumentation`):** antes de ejecutar un campo de la raíz comprueba, en orden: sesión con negocio activo (si no, `NO_AUTENTICADO` o `PERMISO_DENEGADO`); que la tabla conozca el campo (si no lo conoce lo **niega**, falla cerrado); que la persona tenga el permiso, que para `prepararComando` es el de la **variante del comando** (`registrarCliente`, `anonimizarCliente`…) leída del argumento ya resuelto. El resolvedor no se ejecuta si algo falla. Solo se vigilan los campos de la raíz; cada campo negado falla por su cuenta (resultado parcial). Los errores no dicen qué permiso faltaba.
- **SG-19, operaciones sensibles:** si el permiso está declarado `sensible`, además exige `ConfirmacionReforzada` (interfaz en `compartido.seguridad`). **No existe implementación (IDN-3.2 no está hecho), así que toda operación sensible se niega por omisión** con `CONFIRMACION_REQUERIDA`; hasta el DUENO con el permiso. Se comprueba primero el permiso: sin él sale `PERMISO_DENEGADO`, para no revelar que existe el flujo. Cuando IDN-3.2 aporte un bean `ConfirmacionReforzada`, esas operaciones se abren para lo que confirme; una prueba fija ese contrato.
- **Permisos por operación de Clientes (a confirmar):** `clientes`→`clientes.lista.ver`; `cliente`, `historialCliente`, `consentimientosCliente`→`clientes.ficha.ver`; `sugerirClientes`→`clientes.sugerir`; `panoramaClientes`→`clientes.panorama.ver`; **`avisoPrivacidadVigente`→`clientes.cliente.registrar`** (decisión mía: quien da de alta necesita el aviso, L-01); `solicitudesArco`→`clientes.arco.gestionar` (sensible). Variantes: `registrarCliente`, `editarCliente`, `registrarConsentimiento` con su permiso; `anonimizarCliente`→`clientes.cliente.anonimizar` (sensible); `publicarAvisoPrivacidad`→`clientes.aviso.publicar` (sensible); las tres de ARCO→`clientes.arco.gestionar` (sensible). Una prueba (`TablaRealDeOperacionesTest`) fija todo esto.
- **Mutaciones (5, todas detectadas):** sin la confirmación de lo sensible; sin comprobar el permiso; un campo sin entrada que queda abierto; un verificador que no verifica; sin comprobar la variante del comando. Se restauraron los archivos.
- **Resultado:** `mvn -B clean verify` con JDK 21: **1589 pruebas, 0 fallas** (1517 + 72), cobertura cumplida, cero fugas de datos de prueba en toda la salida.

**Cambio en pruebas de las fases 3 a 5 (esperado):** 16 pruebas ejecutaban campos de la raíz **sin sesión**, que era justo el hueco; ahora fallaban con `NO_AUTENTICADO`. En vez de relajar la puerta, esas pruebas (`LimitesGraphqlTest`, `ResolvedorDeErroresGraphqlTest`, `EnmascaradoDeErroresTest`, `TiempoMaximoGraphqlTest`) entran con una sesión de prueba (`SesionDePrueba`). La del tiempo máximo entra dentro del hilo que ejecuta, porque `assertTimeoutPreemptively` corre en otro hilo y la sesión de prueba es del hilo de la prueba.

**Límites / decisiones pendientes:**
- **`confirmarComando` solo pide sesión.** Su permiso se comprueba al *preparar* el comando; el token está ligado a quien lo preparó y al comando, y vale 5 minutos. Si se revoca el permiso en ese lapso, el comando aún podría confirmarse: la revalidación del permiso al confirmar la hará cada módulo (para Inventario, la fachada ya acordada); Clientes aún no la tiene.
- **Los comandos sensibles de Clientes (anonimizar, publicar el aviso, ARCO) no se pueden preparar por GraphQL hasta que exista IDN-3.2**, ni siquiera el DUENO. La exportación (`GET /clientes/:id/exportacion`, REST) no está bajo esta puerta: necesita la suya.
- La puerta sigue el hilo de la sesión: depende de que Spring propague el contexto de seguridad al resolvedor, como ya prueban las pruebas por HTTP.
- Inventario aún no tiene esquema GraphQL ni `OperacionesGraphql`; cuando lo tenga, el arranque exigirá declararlas.
- Solo MockMvc y llamadas directas al manejador. Sin commits ni pushes.

**Siguiente:** Fase 7 (WebSocket: activarlo en `/graphql` con autenticación en `connection_init`, `Origin` verificado, tope de suscripciones por cuenta y aislamiento de eventos por negocio; SG-15 y SG-16).

### 2026-10-16 — IDN-4, Fase 7: WebSocket de `/graphql` autenticado y aislado por negocio (hecho; SG-15 y SG-16)
**Qué se hizo, con TDD:**
- **Dependencia y configuración:** `spring-boot-starter-websocket`; `spring.graphql.websocket.path=/graphql` (mismo endpoint, D-02), `connection-init-timeout=5s` y `keep-alive=30s`. El mayor mensaje es el mismo tope de 64 KB del cuerpo HTTP, por el parámetro de contexto de Tomcat `org.apache.tomcat.websocket.textBufferSize` (que sigue a `seguridad.http.cuerpo-maximo-bytes`): uno mayor cierra la conexión con 1009, y uno de 9000 caracteres, que excede los 8 KB que Tomcat trae por omisión, pasa.
- **El apretón de manos (un GET con `Upgrade`):** un navegador no puede mandar `Authorization` ahí, así que pasa sin sesión (`SubidaWebSocket`), pero la puerta HTTP **verifica el origen igual que en HTTP** (403 si no está permitido) y un filtro propio limita las **conexiones nuevas por IP** (`seguridad.websocket.conexiones-por-minuto-ip`, 30 por omisión). Un GET sin `Upgrade` completo sigue siendo 405.
- **Autenticación en `connection_init` (`AutenticacionWebSocketInterceptor`):** el cliente manda `{"Authorization": "Bearer <token>"}`. Cualquier problema (sin token, mal formado, desconocido, o sesión sin negocio activo) cierra la conexión con **4401**, y cuenta como intento fallido de la IP con **el mismo limitador de HTTP**: tras 20 fallos por minuto el apretón siguiente recibe 429 aunque traiga una sesión válida. Se guarda el **token**, no la sesión, y **cada operación nueva lo valida de nuevo**: una sesión revocada se nota en la siguiente operación. También aplica a cada operación el tope de peticiones por cuenta.
- **Tope de operaciones abiertas por cuenta (`ControlDeSuscripciones`, `seguridad.websocket.suscripciones-por-cuenta`, 5 por omisión):** suma todas las conexiones de la cuenta; una suscripción ocupa un lugar mientras su flujo siga abierto y lo libera al terminar, al cancelarse (el cliente manda `complete`) o al cerrarse la conexión. La siguiente recibe `TOPE_DE_SUSCRIPCIONES`. Una cuenta sin nada abierto se olvida, así que la memoria no crece.
- **SG-16, aislamiento (`EventosPorNegocio`):** `filtrar(flujo, negocioDe)` y `deSobres(flujo)` dejan pasar solo los eventos del negocio de la sesión, fijado **al suscribirse** y nunca de un argumento; un evento sin negocio se descarta; sin sesión o sin negocio la suscripción falla al abrirse. Con servidor real, dos sesiones de negocios distintos reciben cada una solo lo suyo.
- **SG-13 en suscripciones:** los errores de un flujo van dentro de cada resultado, no en la primera respuesta, y por ahí también podían filtrar lo que el motor dice del esquema: `EnmascaradoDeErroresInterceptor` ahora enmascara cada resultado del flujo. Y `ResolvedorDeErroresDeSuscripcion` evita un hueco que encontré en el código de Spring: cuando nadie resuelve la excepción de un flujo, su manejador la escribe en el registro **con su mensaje completo**. La regla de clasificación quedó en un solo lugar (`ClasificadorDeErrores`) para consultas y suscripciones.
- **SG-20:** el registro de una operación por WebSocket lee la cuenta y el negocio del contexto de Reactor.
- **Resultado:** `mvn -B clean verify` con JDK 21: **1660 pruebas, 0 fallas** (1589 + 71), cobertura cumplida, cero fugas de datos de prueba. Cinco mutaciones, todas detectadas: sin aislamiento por negocio, sin liberar el lugar de la suscripción, sin enmascarar el flujo, sin contar los `connection_init` fallidos y sin verificar el origen en el apretón.

**Hallazgo — la sesión viajaba por el hilo y la puerta de autorización no la veía en un WebSocket:** lo encontraron solo las pruebas con servidor real (9 de 16 fallaron). El registro mostraba la cuenta y el negocio correctos, pero toda operación respondía `NO_AUTENTICADO`. Mi puerta (`AutorizacionDeOperacionesInstrumentation`) envuelve al resolvedor por *fuera* del decorador de Spring GraphQL, que es quien restaura la sesión en el hilo desde el contexto de Reactor; cuando la puerta corre, el hilo aún no la tiene. En HTTP no se notaba porque la sesión ya vive en el hilo de la petición. **Arreglo:** la sesión viaja en el contexto de GraphQL (`ClavesDeContexto.SESION`): la pone `AutenticacionWebSocketInterceptor` en WebSocket y `SesionEnContextoInterceptor` en HTTP, y la puerta lee de ahí y solo recurre al hilo si no hay. Las pruebas unitarias de la Fase 6 no podían verlo porque ejecutaban con la sesión en el hilo.

**Infraestructura de pruebas:** los contextos nuevos (varios con puerto real) agotaron las 100 conexiones de la base de pruebas (`remaining connection slots are reserved`): cada combinación de propiedades crea un contexto que Spring guarda en caché con su pool de 10. Los *pools* de pruebas bajaron a 3 (`src/test/resources/config/application.yml`). Una corrida posterior falló por un arranque lento de Docker (Ryuk) y se repitió sin cambios; no puedo separar con certeza cuál de los dos ajustes ayudó en la corrida verde.

**Límites / decisiones pendientes:**
- **Una suscripción ya abierta sigue viva aunque la sesión se revoque o se quite un permiso**; se nota en la siguiente operación nueva o al cerrarse la conexión. Un tiempo máximo de vida por suscripción lo resolvería; no lo hice.
- **Hoy el esquema de producción no tiene ninguna suscripción**: el endpoint WebSocket está activo, protegido, y solo responde consultas y mutaciones. Las suscripciones de prueba viven en `src/test`. `EventosPorNegocio` **no se aplica solo**: cada módulo que agregue una suscripción debe pasar su flujo por ahí (por ejemplo `alertasClientes`, P2, y las alertas de Inventario). Falta una prueba arquitectónica que lo exija cuando existan.
- La carga de `connection_init` usa la clave `Authorization` con el esquema `Bearer`, como el extractor estándar de Spring; es una propuesta a confirmar con quien haga el cliente (Flutter, panel).
- El cierre 4401 no distingue token falso, sin negocio o IP agotada, a propósito.
- La línea de registro de una suscripción reporta lo que tardó en abrirse, no su duración.
- El tamaño de mensaje es una configuración de Tomcat; con otro servidor habría que ponerla en otro lugar.
- Solo servidor real de pruebas con Tomcat; no hay pruebas de carga ni de reconexión. Sin commits ni pushes.

**Siguiente:** Fase 8 (consultas permitidas por hash, exigidas solo en producción; SG-11).

### 2026-10-16 — Autorización: unir las dos puertas de `/graphql` (opción 1)
**Contexto:** al traer `origin/julio` (commit `230766e`, «modulo de negocios») apareció una segunda implementación de IDN-4 hecha por el equipo: `FiltroDeEntrada`, `EntradaGraphQl`, `InterceptorDeEntrada`, `LimitesDeConsultaGraphQl`, `ResolvedorDeErroresComunes` y los resolvedores de Negocios. Chocan con las Fases 0 a 7 de este trabajo (sesión obligatoria en todo `/graphql` frente a superficie pública sin sesión; dos instrumentos de límites; dos portadores de sesión). El árbol no compilaba en verde (2000 pruebas: 1 falla y 525 errores de contexto).

**Pedido del usuario (cita textual):** «la opción 1, hazme el plan» y, tras ver el plan, «acepto el plan ejecuta la skill /ecc:tdd-workflow para ejecutarlo». **Autorización escrita para programar esto: sí.** El «sí» acepta también las recomendaciones 1 a 5 del plan.

**Qué autoriza:** con TDD, quedarse con el filtro de entrada del remoto (`FiltroDeEntrada`, `EntradaGraphQl`, resolvedores de Negocios) y portar encima las capas de las Fases 0 a 7, en las fases M0 a M7: línea base, un solo portador de sesión (`EntradaGraphQl`), operaciones públicas en la tabla de operaciones, filtro de entrada unificado (incluye WebSocket con origen), errores (Negocios pasa a ser un `TraductorDeErrores`), un solo instrumento de límites, WebSocket con la suscripción `altaDeNegocio` (aislada por cuenta) y cierre con revisión.

**Decisiones aceptadas con las recomendaciones:** (1) cuerpo máximo de 64 KB; (2) tasa: anónimas válidas 120/min por IP y 60/min por dispositivo, fallos de autenticación 20/min por IP, con sesión 60/min por cuenta; (3) topes de consulta profundidad 7 y costo 100, que solo se suben si una consulta real de Negocios no cabe y con evidencia; (4) `confirmarComando` pública para las reseñas del turista (la dueña del token es la reseña con su dispositivo); (5) un solo mecanismo de tasa en `/graphql`.

**Reglas que se respetan:** no se borra ni se debilita ninguna prueba, del remoto o mía (si una clase se retira, sus aserciones pasan a la que la reemplaza); sin commit ni push; Jira intacto.

**Fuera de lo autorizado:** SG-11 (consultas por hash), SG-18, la conexión Identidad↔Inventario, el adaptador de Inventario al bus de eventos y cualquier commit o push.

### 2026-10-16 — IDN-4: una sola puerta de `/graphql`, fases M0 a M7 (hecho)
**Pedido del usuario:** la autorización de la entrada anterior («Autorización: unir las dos puertas de `/graphql`»). Con TDD (`/ecc:tdd-workflow`). Sin commit ni push.

**Qué quedó:** el filtro de entrada del equipo (`FiltroDeEntrada`, `EntradaGraphQl`, resolvedores de Negocios) es la base, y encima están las capas de las Fases 0 a 7.
- **Un solo portador de sesión en GraphQL:** `EntradaGraphQl`. La puerta de autorización, la confirmación reforzada, el WebSocket, el tope de suscripciones, el aislamiento de eventos (`EventosPorNegocio.filtrar(flujo, entrada, …)`) y el registro la leen de ahí. Se retiraron `SesionEnContextoInterceptor` y `ClavesDeContexto.SESION`.
- **Tabla de operaciones con tres accesos** (`Acceso.PUBLICA`, `CUENTA`, `NEGOCIO`) y variantes de comando públicas (`ComandoDeclarado.publico`, con marca explícita: un permiso ausente sin la marca es un error). Negocios declara sus 19 operaciones (`OperacionesDeNegocios`): el panel con el mismo permiso que ya exige su servicio, `misAltasDeNegocio` y `altaDeNegocio` de cuenta, y tres consultas públicas del turista más la variante `publicarResena`. `confirmarComando` pasó a pública (decisión 4).
- **Filtro de entrada ampliado:** deja pasar el apretón de manos de WebSocket (a los controles de origen y de conexiones por IP), cuenta los tokens falsos como fallos de la IP y consulta antes de autenticar, aplica la tasa por cuenta, topa el token en 256 caracteres y manda `WWW-Authenticate: Bearer` en el 401. La cadena de Spring Security solo deja pasar `/graphql` si el filtro dejó la entrada (falla cerrado). Cuerpo máximo 64 KB para todo.
- **Errores:** Negocios es ahora un `TraductorDeErrores` (conserva códigos, estados y `faltantes`); `ClasificadorDeErrores` entiende `NO_AUTENTICADO`, `SIN_NEGOCIO_ACTIVO` y los argumentos ilegibles (`CONSULTA_INVALIDA`, con una línea WARN sin el valor). Un solo código de tasa: `DEMASIADAS_SOLICITUDES`. Un solo medidor de consultas (el mío); los 7 casos del otro se portaron.
- **WebSocket:** una sesión sin negocio activo ya puede conectarse (`altaDeNegocio` es para quien aún no tiene negocio); lo que pide negocio lo niega la puerta con `SIN_NEGOCIO_ACTIVO`. Probado con servidor real, con dos cuentas.
- **Rutas REST de Negocios** declaradas públicas una por una (`RutasPublicasDeNegocios`) y las de documentación (para que fuera de desarrollo respondan 404 y no 401).

**Verificación hecha:** `mvn -B clean verify` con JDK 21: **2069 pruebas, 0 fallas**, cobertura cumplida, ningún dato de prueba sensible en los registros. La suite del equipo (1560 en su rama) sigue en verde dentro de ella. 18 mutaciones en los puntos críticos, todas detectadas. Dos revisiones independientes (`java-reviewer`, `security-reviewer`): ningún camino para saltarse la autorización; 7 hallazgos corregidos con prueba primero. Detalle, tablas y estado de SG-01 a SG-21 en [`docs/pruebas/idn4-puerta-unificada.tdd.md`](pruebas/idn4-puerta-unificada.tdd.md).

**Hallazgos de verdad (los encontraron las pruebas, no el plan):**
- **Bug de raíz en `LimiteDeTasaEnMemoria` (de Identidad):** el barrido usaba la ventana de la llamada que lo disparaba y borraba los intentos de todas las claves. Con el tráfico de `/graphql` (1 minuto) se liberaban los topes de solicitud de código de acceso (hasta 24 h). Corregido: cada clave guarda su ventana.
- **Regresión de auditoría (SG-20)** al quitar la sesión del hilo: el registro salía con `cuenta=- negocio=-`. Corregido.
- `InterceptorDeEntrada` podía pisar la entrada autenticada del WebSocket (funcionaba por un orden implícito). Ahora ignora el WebSocket.

**Cambios de contrato que verá quien consuma la API** (todos intencionales): sin token una consulta llega a GraphQL y lo protegido responde `NO_AUTENTICADO` con HTTP 200 (token falso o revocado sigue siendo 401, idéntico); una sesión sin negocio activo recibe `SIN_NEGOCIO_ACTIVO` en vez de `PERMISO_DENEGADO`/403; los rechazos del filtro salen como `{"errors":[…]}`; el tope de cuerpo es 64 KB; los límites de consulta devuelven `DEMASIADOS_ALIAS`, `DEMASIADOS_CAMPOS_RAIZ`, `CONSULTA_MUY_PROFUNDA` o `CONSULTA_MUY_COSTOSA`.

**Lo que no se hizo igual que en el plan:** la decisión 5 (un solo mecanismo de tasa) no se unificó: el puerto `LimiteDeTasa` de Identidad no tiene «consultar sin consumir», que los fallos por IP necesitan. Tres de mis filtros siguen en la cadena (origen, conexiones de WebSocket, y los de tasa para las rutas REST) porque hacen algo que el filtro del equipo no hace. No se creó `EventosPorCuenta`: `AvisosDeAlta` ya aísla por cuenta.

**Pendiente y decisiones abiertas** (registradas, no corregidas): proxy y IP real al desplegar; las suscripciones por SSE no pasan por el tope de suscripciones; no hay tope de conexiones WebSocket simultáneas por cuenta; `X-Dispositivo-Id` rotable (reseñas fuera de demo); `/demo/**` y `/admin/**` abiertos a nivel de cadena (protegidos por perfil y por token); `confirmarComando` pública exige una prueba arquitectónica cuando Clientes e Inventario tengan resolvedores; falta registro de los 401/429 del filtro y agrupar IPv6 por /64. **SG-11 y SG-18 siguen pendientes** (no necesarias para la demo). Sigue pendiente la conexión Identidad↔Inventario y el adaptador de Inventario al bus de eventos.

**Cierra:** el pendiente «cerrar `/graphql` antes del primer resolvedor (SG-01 a SG-10)» queda levantado para Negocios y el resto, con las reservas de arriba.

### 2026-10-16 — Se trae la rama `uriel` (Ventas, Caja, canal de demo y maqueta Flutter) a `julio`
**Pedido del usuario:** «trae lo que hay en la rama uriel a la rama julio, ahí está venta y caja». Merge local, sin push.

**Qué se hizo:** se adelantó `julio` a `origin/julio` (un commit de documentación, `inventario.md`) y se mezcló `origin/uriel` (6 commits sobre la base vieja `98762d3`, anterior a Negocios y a todo IDN-4) con un commit de mezcla (`6366210`). Trae los módulos `ventas`, `caja` y `canal`, el esquema `ventas/caja/canal.graphqls` y la maqueta en Flutter (`frontend/`). El trabajo sin confirmar de IDN-4 se guardó antes en el stash, con copia en archivo, y se devolvió después; **sigue sin confirmar**.

**Conflictos resueltos conservando ambos lados:** `application-desarrollo.yml`, `EstructuraModulosTest` y `BITACORA.md`. Además, **`compartido/dinero/Redondeo`** existía en las dos ramas con contenido distinto (la de Ventas y Caja tenía `precio`, `dineroValido` y `cantidadComoTexto`; la de Inventario, `costoUnitario`, `factor` y `dividir`). Eran compatibles (mismas constantes y mismo `dinero` y `cantidad`): quedó una sola clase con la unión, y la prueba de Inventario pasó a llamarse `RedondeoDeInventarioTest`. `ContextoDeSesionTest` (del canal) usaba un doble escrito a mano contra la API vieja de Identidad; ahora usa un mock, sin tocar sus aserciones.

**Estado tras la mezcla (no está en verde):** `mvn clean verify`: 2249 pruebas, 3 fallas y 542 errores. **Una sola causa para casi todo:** la verificación de la tabla de operaciones (SG-21) falla al arrancar porque `Query.cajas`, `cortes`, `turnoActivo`, `saldoTurno`, `venta`, `ventasDelTurno`, `productosSimulados`, `cuponesSimulados` y sus mutaciones no están declaradas; ningún contexto de Spring arranca. Las 3 fallas son `EsquemaGraphqlClientesTest`, que aún supone un esquema con solo Clientes y compartido.

**Lo que hay que integrar (necesita plan y autorización):**
1. Declarar las operaciones de Ventas, Caja y canal en la tabla (`OperacionesGraphql`) con permisos reales; hoy el canal usa `PermisosSimulados`.
2. El canal trae su **propio interceptor de sesión** (`InterceptorDeSesion`, `ContextoGraphQl`) que autentica el Bearer por su cuenta y asigna un **negocio de demostración** a las sesiones sin negocio. Hay que moverlo a `EntradaGraphQl` y decidir qué se queda del negocio de demostración.
3. `ComandosController` también resuelve `prepararComando` y `confirmarComando`, igual que Negocios: GraphQL admite un solo resolvedor por campo. Hace falta el despachador de comandos por variante (el mismo que necesitan Clientes e Inventario).
4. Ajustar `EsquemaGraphqlClientesTest` y verificar el aislamiento de datos de Ventas y Caja con la puerta unificada.

### 2026-10-16 — Plan de integración de Ventas y Caja (plan, sin autorización para programar)
**Pedido del usuario (cita textual):** «define el plan de integración, el objetivo es únicamente traer caja y venta de la rama uriel. De igual forma define cómo quedarán las queries de GraphQL para que en lo que estamos integrando esto trabajemos a la par en misceláneos y en restaurantes para que cuando todo quede podamos consumir los módulos horizontales, esto preséntalo en un .md y en un artefacto para mejor lectura, sé específico y no omitas nada». Con `/ecc:plan`. **Es un plan: no hay autorización escrita para programar.**

**Qué se hizo:** [`docs/planes/integracion-ventas-caja.md`](planes/integracion-ventas-caja.md) (19 secciones y 3 anexos) y su artefacto. Cubre: alcance (qué entra de `uriel` y qué no), 16 hallazgos, decisiones, arquitectura objetivo, el **esquema GraphQL final** de Ventas y Caja (con la tabla de operaciones, permisos, límites y ejemplos), 11 permisos nuevos y la matriz de roles, el contrato Java, los eventos, cómo consumen los verticales, las fases F0 a F7 con TDD, el trabajo en paralelo con Misceláneos y Restaurantes (hitos, propiedad de archivos, reparto de migraciones y de decisiones), riesgos, criterios de aceptación y estimación.

**Hallazgos que cambian el plan:** (H-01) los identificadores `D-76` a `D-90` están **usados dos veces** (Identidad y Negocios, y Ventas y Caja); (H-02, H-03, H-04) el canal de `uriel` trae una tercera autenticación, un negocio de demostración que se asigna a toda sesión sin negocio y permisos simulados que dejan a cualquier cuenta anular y autorizar diferencias; (H-05, H-06) `prepararComando` y `confirmarComando` los resuelven dos controladores y `Resultado.datos` es no nulo pero Negocios no lo devuelve; (H-11) las migraciones `V030` y `V040` que reservó `uriel` ya no están libres; (H-10) sus eventos salen sin outbox.

**Decisiones propuestas:** D-110 a D-124 (las de `uriel`, renumeradas) y D-125 a D-134 (despachador único de comandos, permiso al preparar y al confirmar, 11 permisos, SPI de productos en `compartido.catalogo`, revocar el negocio de demostración, prefijo de tipos GraphQL, paginación, outbox, reparto de migraciones, cupones inválidos sin Pasaporte). **Todas PROPUESTA.**

**Pendiente (preguntas P-1 a P-10 del documento):** quitar `canal` y `frontend` de `julio`; aprobar la renumeración, los permisos y la matriz, las queries, el SPI de productos; si la persistencia entra ahora o después; quién mantiene Negocios y quién toma Misceláneos y Restaurantes.

### 2026-10-16 — Maqueta completa de Restaurantes en Flutter (personal, comensal por QR y turista)
**Pedido del usuario (cita textual):** «genera un mockup completo para la app de restaurantes […] basándote en el siguiente artefacto [Módulo Restaurantes v0.5] […] y de la arquitectura que se tiene en el repositorio […] frontend/lib/pantallas […] estilo limpio con bordes semirredondeados y afilados con colores claros #FFFFFF #002F6C #4A90E2 #0A192F #FDFBF7 #3D2314 #E65F00 #1A4314 #8B2520 […] formal pero no de oficinista, cómodo e intuitivo […] con Dart y la biblioteca de Flutter, para móvil y para web».

**Alcance y autorización:** es una **maqueta** del frontend (D-89, aceptada: Dart y Flutter, móvil primero), con **datos de ejemplo en memoria** y sin tocar el servidor. **No hay autorización para programar el módulo `restaurantes` del servidor** (P1b y la persistencia siguen sin autorizar según el diseño v0.5); la maqueta no lo sustituye ni lo adelanta. El diseño fuente (`docs/diseno/restaurantes.md`) no está en esta rama: se leyó del artefacto.

**Qué se hizo (`frontend/`):**
- **Tema:** `tema.dart` con la paleta pedida (nueve colores; los tonos suaves se mezclan con blanco o crema, no se agregan colores) en claro y oscuro. Formas: tarjetas y paneles de 14 px, controles de 10 px y **sellos casi rectos (3 px)** para estados, más una barra lateral recta de color en las tarjetas de estado. Colores con significado fijo (`ColoresSemanticos`): verde = libre o listo, naranja = ocupado o en preparación, rojo = agotado, cancelado o alergia, azul cielo = información.
- **Adaptable:** `widgets/adaptativo.dart`. Teléfono (barra inferior, con «Más» si hay más de cinco secciones), tableta (riel lateral) y escritorio o web (riel extendido y vistas maestro-detalle: menú con detalle, salón con cuenta, tablero de cocina en tres columnas, carta con pedido al lado).
- **Personal (`pantallas/restaurantes/`):** Panel (ventas, ticket, mesas libres, avisos del salón, lo más vendido, alertas y margen por platillo), Menú (búsqueda, filtros, platillos con etiquetas, temporada, agotado y publicado; alta, edición y baja), Receta (versiones, insumos con merma, costo teórico y margen, flujo preparar → confirmar), Salón (mesas por zona, estado derivado, QR de cada mesa con regenerar, pausar y bloquear), Cuenta (líneas, enviar a cocina, anular con motivo, pedir la cuenta, cobrar y ver los insumos descontados), Cocina y barra (comandas por estación, tiempo resaltado desde 15 min, marca de alergia), Insumos (existencias, mínimo, CPP y consumos sin descontar) y Ajustes (modo de servicio, pedidos por QR, publicación). **El puesto se elige en la barra superior** (Dueño, Mesero, Cocinero, Cajero) para ver cómo cambian las secciones y los permisos; el costo y el margen solo los ve el dueño (RC-10).
- **Comensal por QR (`pantallas/comensal/`):** escáner simulado, y la página que abre el QR sin cuenta (carta, Mi pedido, Info y reseñas), con límites, idempotencia, seguimiento por línea (en cocina, preparando, listo, cancelado con su motivo) y pedir la cuenta. Es la pantalla que el diseño pide como web pública (D-185, D-190).
- **Turista (`pantallas/turista/restaurantes.dart`):** nueva sección «Restaurantes» con filtros por tipo de cocina, mesas libres **en vivo** del restaurante de la demo, ficha con menú público (sin costos ni recetas), reseñas y entrada al QR.
- **Estado (`estado/restaurante_demo.dart`):** reproduce las reglas del diseño que la maqueta necesita (MN, RC, CM, MS, CT, CD y QR); cada operación exige su permiso y falla con el código del contrato y su texto en español. Dinero en centavos y cantidades de insumos en diezmilésimas, sin `double`. La cemita poblana da 43.25 de costo y 41.75 de margen (49.12 %), igual que el diseño.

**Pruebas:** `flutter analyze` sin hallazgos y `flutter test` con **80 pruebas en verde** (14 anteriores, 42 de reglas del estado y 24 de recorridos en teléfono y pantalla ancha). Se revisó el resultado en un navegador, en teléfono (390 px), escritorio (1440 px) y modo oscuro.

**Límites y decisiones de la maqueta (sin ID: si el equipo las acepta se numeran):**
- Todo es simulado y rotulado «Datos de ejemplo»: no hay GraphQL, ni sesión de Identidad, ni Ventas ni Inventario reales; el cobro y el descuento de insumos son locales. El dominio de la web pública sigue sin elegirse (se muestra `dominio-por-definir`).
- El QR es un dibujo de ejemplo, no escaneable; la cámara se simula con botones. El «refresco cada ~10 s» del comensal es inmediato porque el estado es local.
- El selector de puesto existe **solo en la maqueta**; con el servidor el puesto sale de la sesión.
- Los permisos de la maqueta (`permisos_restaurante.dart`) siguen los nombres del diseño pero **no son** la matriz final (sección 9 del diseño); hay que cotejarlos cuando se programe el servidor.
- Se agregó en la maqueta una regla que el diseño no lista: el nombre de un platillo de 2 a 80 caracteres falla con `NOMBRE_INVALIDO`.

**Pendiente:** confirmar la paleta y las formas como estándar de toda la app (la maqueta de Ventas y Caja ya las usa por el tema común); decidir si la web pública es esta misma app en Flutter web; conectar con el servidor cuando Restaurantes esté autorizado e integrado con Ventas e Inventario.
