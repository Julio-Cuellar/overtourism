# Negocio 360 — Contexto del proyecto

Proyecto para el Hackathon "Puebla Turística 360°: Datos que transforman experiencias" (Tech-Capital Puebla 2026). Evento: 15 al 17 de octubre de 2026, 11:00 a 17:00, Centro Expositor. Documento creado el 7 de octubre de 2026 y actualizado el 8 de octubre de 2026.

> **Estado y fuente de verdad.** El estado del proyecto, las decisiones (`D-xx`) y los pendientes viven en [`docs/BITACORA.md`](docs/BITACORA.md); el diseño de cada módulo, en `docs/diseno/`. Este archivo conserva lo estable (reglas de la convocatoria, principio rector, seguridad y convenciones). Si algo de aquí contradice la Bitácora, **manda la Bitácora**. Las secciones marcadas como «original» describen el diseño previo al cambio de rumbo del 2026-10-08 y se conservan como referencia.
> **Fase actual: diseño. No hay código y no se programa hasta que el equipo lo autorice por escrito.**

## Objetivo

Convertir el uso diario de un negocio local en una ruta hacia la digitalización, el control financiero y la formalización voluntaria. Se logra con un asistente conversacional (Telegram para la demo, WhatsApp como siguiente paso) y un panel de gestión en la nube. Los usuarios objetivo son prestadores turísticos de Puebla: artesanos, fondas, talleres, guías y comerciantes de mercado.

Eje de la convocatoria: Desarrollo económico y conexión con prestadores locales.

### Alcance vigente (desde el 2026-10-08)
El producto pasó de «asistente + panel» a una **plataforma modular de turismo**. Lo que cambió, en nueve puntos:
1. Módulos **horizontales** compartidos por todos los giros (inventario, contabilidad, caja, ventas, clientes, catálogo).
2. Giros como módulos **verticales** (restaurantes, museos, misceláneos) que usan a los horizontales.
3. Plataforma central para turistas y locales: reseñas, menú, información, horas pico y disponibilidad de mesas o atención.
4. Afiliación de negocios a un pasaporte turístico con cupones.
5. Filtros por intereses y tipo de viaje (cultura y arte, gastronomía local, festividades, noche o día, familiar, presupuesto).
6. Búsqueda y rutas de visita generadas con IA (Gemini o ChatGPT, por decidir) sobre nuestra API.
7. Categorías y subcategorías (restaurante: gluten free, verano, mixto, gastronomía típica, comida corrida, extranjera; museos: historia, cultura, arte).
8. Ubicación en tiempo real y recomendaciones.
9. Cuestionario de intereses posterior al registro (tipo de visitante, 3 a 5 intereses, estilo de viaje).

Además: rutas del transporte público y de la red RUTA.

El asistente por chat y el panel del diseño original pasan a ser canales de esta plataforma; **si el bot continúa es una decisión abierta**. Los «Objetivos específicos» y la mayor parte del «Alcance del MVP» de más abajo son los originales; donde chocaban con lo vigente se corrigieron o se marcaron.

### Problemas que ataca
- Informalidad de muchos negocios.
- Falta de herramientas para gestionar finanzas.
- Falta de presencia y herramientas digitales.
- Flujo turístico concentrado en el centro histórico; derrama económica desigual.

### Objetivos específicos (alcance original; siguen como referencia)
1. Digitalizar un negocio en minutos (nota de voz + fotos) y publicarlo en un directorio turístico verificado.
2. Registro de ventas y gastos por voz o texto, con resúmenes simples y panel de consulta.
3. Pronóstico de demanda cruzando historial del negocio con ocupación, eventos y temporadas.
4. Ruta de formalización explicable y gradual, con beneficio tangible (historial financiero verificable para microcrédito o cobro digital, siempre con consentimiento).
5. Conectar turistas con prestadores locales y desconcentrar flujos.
6. Tablero agregado y anónimo para la Secretaría de Desarrollo Turístico.

## Reglas de la convocatoria que condicionan el diseño

- No hay dataset predeterminado: el equipo debe localizar, justificar, documentar y limpiar sus datos. La identificación de datos vale 20% de la rúbrica.
- La IA debe tener valor real y justificado. Consumir una API o poner un chatbot genérico no es suficiente.
- Se debe poder explicar qué componentes usan IA, su función y sus limitaciones.
- Equipos: 3 estudiantes + 1 profesor tutor. La solución y el desarrollo los hacen principalmente los estudiantes.
- Una sola sesión de mentoría (no puntúa). La calificación es sobre el resultado final en Tech-Capital.
- Etapa final: versión funcional o demostrable, pitch ejecutivo, demo y ronda de preguntas.
- Un proyecto puede ganar el Gran Premio + una categoría especial, nunca dos categorías especiales.
- Propiedad intelectual queda en los autores.

### Rúbrica
| Criterio | Peso |
|---|---|
| Identificación, pertinencia, calidad y aprovechamiento de datos | 20% |
| Relevancia y comprensión del problema | 15% |
| Innovación y creatividad | 15% |
| Solidez técnica y funcionamiento del prototipo | 15% |
| Impacto turístico, económico y social | 15% |
| Viabilidad, implementación y escalabilidad | 10% |
| Experiencia de usuario y utilidad | 5% |
| Calidad del pitch y demostración | 5% |

### Entregables
Descripción ejecutiva; definición del problema y usuarios; descripción de la solución; inventario y documentación de fuentes de datos; justificación de selección de datos; procesos de obtención, limpieza, integración, transformación y análisis; arquitectura o metodología; prototipo funcional; evidencia de análisis y resultados; presentación y demo; documentación del modelo de IA (configuración, variables, resultados y limitaciones).

Premios objetivo: Gran Premio y Mejor solución basada en datos. Narrativa de impacto: turismo sostenible.

## Principio rector de arquitectura

**El modelo interpreta, el código decide.**

| LLM | Código determinista |
|---|---|
| Transcribir y entender intención | Decidir qué acción se ejecuta |
| Extraer datos a un esquema JSON fijo | Validar tipos, rangos y reglas de negocio |
| Redactar tono de la respuesta | Calcular toda cifra (totales, utilidad, pronóstico) |
| | Escribir en la base de datos |

El LLM nunca toca la base de datos, nunca controla identidad ni parámetros de acceso, y nunca inventa números.

## Arquitectura

### Arquitectura vigente (actualizado 2026-10-08)
- **Monolito modular** en **Java 21 con Spring Boot** (decidido): módulos horizontales y verticales. Cada módulo es dueño de sus datos, con su propio esquema de Postgres.
- **Reglas R1 a R6** (detalle y estado en `docs/BITACORA.md` y `docs/diseno/`):
  - R1: un módulo solo obtiene los datos de otro por su API pública (una interfaz Java en el mismo proceso) o por el bus de eventos.
  - R2: de vertical a horizontal, síncrono = llamar a la API pública; asíncrono = evento por el bus. Un evento anuncia un hecho y no devuelve respuesta.
  - R3: todo evento tiene un ID único (UUIDv7) y todo consumidor es idempotente.
  - R4: las dependencias van solo de vertical a horizontal.
  - R5: los eventos son contrato público y versionado; solo se agregan campos opcionales.
  - R6: entre horizontales rige lo mismo que R2.
- **Protocolos:** GraphQL sobre HTTPS para la app del turista, el panel del locatario y el tablero de gobierno, con suscripciones por WebSocket en el mismo endpoint. REST para las herramientas de la IA (`/ia/v1`), el acceso con código, la subida de archivos, los webhooks y el bot, la administración y la salud.
- **Eventos:** outbox transaccional, entrega al menos una vez y sin orden global; los consumidores deduplican.
- **Base de datos:** Postgres con seguridad por fila (`negocio_id`). Dónde se aloja la base de la demo está por decidir.

Lo que sigue de esta sección es el **diseño original**. Donde difiere del bloque anterior, manda el bloque anterior.

Dos canales sobre una misma base de datos:
- **Bot (Telegram/WhatsApp):** entrada de datos y consultas rápidas.
- **Panel web (PWA, mobile-first):** consulta y gestión (ventas, gastos, utilidad, corrección de movimientos, pronóstico, avance de formalización, perfil público).

Perfiles: **Prestador** (solo sus datos), **Turista** (directorio y mapa), **Gobierno** (tablero agregado y anónimo, con mínimo de negocios por celda para que nadie sea identificable).

Stack original (el backend ya está decidido en Java; el resto sigue por confirmar):
- Base de datos: Postgres multi-tenant con seguridad por fila. Supabase u otra opción para alojar la base de la demo está por decidir (D-16).
- Login: código por SMS o mensajería al número del dueño, sin contraseñas.
- Frontend: PWA. La tecnología (Next.js u otra) y si el turista y el locatario usan una sola app o dos están por decidir.
- Orquestación del bot: n8n como router determinista (no como agente autónomo). Solo aplica si el bot continúa (decisión abierta).
- Lógica de negocio: backend en **Java 21 con Spring Boot** (decidido). **n8n, si se usa, orquesta; no contiene reglas de negocio.**
- IA: transcripción de voz, extracción de transacciones, pronóstico, resolución de entidades, recomendador, rutas de visita, puntaje de formalización.

> Las secciones «Máquina de estados», «Flujo en n8n» y «Endpoints» describen el canal del bot del **diseño original**. Siguen como referencia si el bot continúa (decisión abierta). En el diseño vigente, la app y el panel usan GraphQL y el bot usa REST bajo `/bot`. Se mantienen el patrón preparar → confirmar y la idea de un caso de uso por operación.

### Máquina de estados de la conversación
Estado persistido por usuario. En cada estado solo se permiten ciertas intenciones y endpoints (lista blanca, mínimo privilegio).

```
IDLE ──registrar──▶ CAPTURANDO ──datos completos──▶ CONFIRMANDO ──sí──▶ GUARDADO ──▶ IDLE
  │                     │ faltan datos                  │ no / corregir
  │                     ▼                               ▼
  │                ACLARANDO ◀───────────────────── CORRIGIENDO
  ├──consultar──▶ CONSULTANDO ──▶ IDLE
  ├──formalizar──▶ GUIA_FORMALIZACION ──▶ IDLE
  └──nuevo usuario──▶ ONBOARDING (nombre → giro → ubicación → consentimiento) ──▶ IDLE
```

Reglas:
- Confirmación obligatoria antes de todo guardado.
- Transiciones globales de escape: "cancelar", "menú", "ayuda" desde cualquier estado.
- Tras 2 aclaraciones fallidas, ofrecer menú de botones.
- Deshacer disponible unos minutos, con auditoría.
- El texto del usuario es dato, nunca instrucción (resistencia a inyección).
- En CONSULTANDO solo existen herramientas de lectura.

### Flujo en n8n
```
Webhook ─▶ responder 200 ─▶ deduplicar message_id ─▶ resolver negocio por teléfono
 ─▶ cargar estado ─▶ transcribir si es voz ─▶ LLM: intención + extracción (JSON con esquema)
 ─▶ validar contra lista blanca del estado ─▶ Switch ─▶ HTTP a endpoint del caso de uso
 ─▶ plantilla con respuesta del servidor ─▶ enviar mensaje ─▶ guardar estado y auditoría
```

### Endpoints (un endpoint por caso de uso)
| Endpoint | Tipo | Estados permitidos |
|---|---|---|
| `POST /ventas/preparar` | Valida y devuelve resumen + `token_confirmacion` de un solo uso | CAPTURANDO |
| `POST /ventas/confirmar` | Escribe (requiere token) | CONFIRMANDO |
| `POST /ventas/anular` | Escribe (ventana de minutos) | IDLE, CORRIGIENDO |
| `GET /resumen?periodo=` | Lee | CONSULTANDO |
| `GET /pronostico` | Lee | CONSULTANDO |
| `GET /formalizacion/estado` | Lee | GUIA_FORMALIZACION |
| `POST /onboarding/paso` | Escribe | ONBOARDING |

Contrato de respuesta fijo: `{ ok, codigo, datos, mensaje_plantilla }`.

### Garantías de veracidad
1. `negocio_id` sale del número que llegó en el webhook, nunca del LLM. Reforzado con seguridad por fila en la BD.
2. El LLM solo ve el JSON del servidor, nunca la base de datos.
3. Respuestas finales = plantilla rellenada con `datos`. Si se reescribe con LLM, un verificador comprueba que toda cifra del texto exista en `datos`; si no, se envía la plantilla original.
4. Errores: se envía el texto asociado al `codigo`; el modelo no improvisa.
5. Respuesta vacía: "no tengo ese dato", nunca una estimación.
6. Consultas en lenguaje natural: el modelo elige entre consultas predefinidas y llena parámetros. **Nunca SQL libre generado por el modelo.**
7. Temas fiscales: solo desde base de conocimiento curada con fuente y fecha.

### Seguridad
- Secreto en encabezado en cada endpoint; validar firma del webhook de WhatsApp/Telegram.
- Solo el endpoint de entrada del bot es público.
- Idempotencia por `message_id` (los reintentos de la plataforma duplicarían ventas).
- Bloqueo o cola por usuario para evitar condiciones de carrera en el estado.
- Auditoría de cada llamada: negocio, estado, intención, endpoint, payload, respuesta, versión de regla aplicada.
- Nunca pedir ni almacenar credenciales del SAT.

## Reglas de negocio modificables

Viven como **datos versionados, no como código**: tabla `reglas` (o YAML/JSON) con condición, acción, vigencia, fuente y versión, evaluada por un motor simple (json-logic, OPA o Cerbos). Pantalla de administración con roles y registro de cambios.

- Globales: montos máximos sin doble confirmación, umbral de anomalía (por ejemplo, venta 10x el promedio dispara pregunta), categorías de gasto, plantillas de texto.
- Fiscales y formalización: requisitos por régimen, topes y plazos, con fecha de vigencia y fuente. **No cargar valores de memoria; verificar contra SAT y municipio.**
- Por negocio: categorías propias, horario, redondeo.

## Fuentes de datos candidatas

DENUE (INEGI), DataTur (SECTUR), ENOE (informalidad), OpenStreetMap, calendario de eventos y temporadas, ocupación hotelera, datos propios del piloto. Documentar por fuente: procedencia, licencia, calidad, actualidad, método de limpieza y trazabilidad. Todo dato simulado debe estar etiquetado como simulado.

## Componentes de IA y cómo se justifican

- Extracción de transacciones desde voz y texto a esquema fijo, con confirmación. Métrica: precisión de extracción y tasa de error sobre un conjunto de frases reales (acento poblano, "una docena", "tres cincuenta").
- Pronóstico de demanda (gradient boosting o Prophet) contra un baseline simple.
- Resolución de entidades entre DENUE, OSM y redes para estimar negocios "invisibles".
- Recomendador con restricción de equidad para repartir visitas hacia negocios menos expuestos.
- Rutas de visita con IA (Gemini o ChatGPT, por decidir): el modelo elige entre los negocios que devuelve la API mediante herramientas de solo lectura; el servidor valida identificadores y horarios y calcula duraciones y costos.
- Puntaje de formalización explicable (reglas + modelo, sin caja negra).
- Documentar limitaciones de cada componente.

## Alcance del MVP (quedan pocos días)

### Alcance vigente (actualizado 2026-10-08)
Detalle y estado de cada decisión en `docs/BITACORA.md`.
- **Inventario está dentro del MVP.** Prioridad 1: artículos, movimientos, saldo, kardex, conversión de unidades, costo promedio ponderado y recetas con descuento por platillo (las recetas viven en Restaurantes). Prioridad 2: lotes, caducidades y salida FEFO. Prioridad 3: números de serie. Si falta tiempo se recorta de abajo hacia arriba: primero series, luego lotes.
- **Giro real de la demo:** se propone que Restaurantes sea el único vertical que funcione de verdad y que Museos y Misceláneos se muestren como mockup. Es una decisión abierta.
- **Camino dorado de la demo:** por definir.
- **Funciones para el turista** (filtros, cuestionario, rutas con IA, pasaporte, transporte): su prioridad dentro del MVP está por definir.
- **Fase actual: diseño.** No se programa hasta que el equipo lo autorice por escrito (ver «Convenciones»).

### Alcance original (se conserva como referencia)

**Debe funcionar de verdad:**
1. Onboarding por voz y foto.
2. Libro de ventas hablado (preparar, confirmar, corregir, anular).
3. Consulta de resumen.
4. Pronóstico de demanda.
5. Panel mínimo: resumen día/semana, lista de movimientos editable, pronóstico, checklist de formalización con porcentaje, perfil público.
6. Vista de gobierno con mapa y embudo.

**Prototipo con datos simulados y etiquetados:** puntaje de formalización y precalificación de microcrédito.

**Piloto:** 5 a 10 negocios reales (mercado, Analco, Barrio del Artista, Cholula) usando el bot algunos días.

**Fuera de alcance (roadmap):** facturación, nómina, cuentas por cobrar, reportes exportables, WhatsApp Business API en producción. (El inventario salió de esta lista el 2026-10-08 y ahora está dentro del MVP; ver «Alcance vigente».)

## Riesgos conocidos

- Parecer "otro sistema de ventas" (ya existen Treinta, Alegra, Aspel, Square). Diferenciadores a destacar: ruta de formalización, pronóstico ligado a demanda turística, historial financiero alternativo, directorio que conecta turistas con el negocio.
- Alcance excesivo para el tiempo disponible.
- Desconfianza de quien opera en la informalidad: el gobierno solo ve datos agregados y anónimos; el negocio decide qué comparte.
- Latencia de voz + LLM + HTTP: enviar acuse inmediato en mensajes de voz.
- WhatsApp Business exige alta y plantillas: usar Telegram para la demo.

## Estructura del pitch

1. Historia real de una vendedora que no tiene cómo cobrar ni saber si gana.
2. El problema, con cifras de INEGI (verificar las más recientes).
3. Demo en vivo. El camino dorado vigente está por definir (ver Bitácora). Versión original: nota de voz → registro → resumen → mapa en el tablero.
4. Datos y metodología: fuentes, limpieza, calidad.
5. Resultados del piloto.
6. Escalamiento (Cholula, Atlixco, Cuetzalan, Zacatlán, Chignahuapan) y modelo sostenible (programas de gobierno, comisiones por referir microcrédito o cobros digitales).

## Convenciones para trabajar en este repo

- Idioma de la interfaz, plantillas y documentación: español de México.
- Toda cifra mostrada al usuario viene de código determinista, nunca del LLM.
- Toda escritura a la base que inicia una persona pasa por el patrón preparar → confirmar con token de un solo uso. (Excepción propuesta, D-22: los descuentos de inventario que son consecuencia de una venta ya confirmada; verifica su estado en la Bitácora.)
- Las reglas de negocio no se hardcodean: se leen de la tabla o archivo de reglas versionado.
- Workflows de n8n exportados a JSON y versionados en Git (solo si el bot se mantiene con n8n).
- Cada operación pública (endpoint REST, operación GraphQL o API de un módulo) tiene su contrato documentado (entrada, salida, códigos de error) y pruebas.
- Mantener un conjunto de pruebas con frases reales para medir la extracción.
- No introducir dependencias ni funciones fuera del alcance del MVP sin preguntar.
- **No programar hasta que el equipo lo autorice por escrito.** La autorización debe quedar anotada en la Bitácora. Mientras la fase sea de diseño, se producen documentos, diagramas y mockups, no código del producto.
- **Registrar el estado en `docs/BITACORA.md`.** Cada sesión que cambie algo cierra con una entrada al final; las decisiones llevan un ID (`D-xx`) y un estado (aceptada, propuesta, abierta). No se da por aceptado lo que el equipo no confirmó.
- **Respetar las reglas R1 a R6** (ver «Arquitectura vigente»). Los eventos llevan sobre común con `id` UUIDv7, `negocio_id` y `version`; los consumidores son idempotentes.
- **El diseño de cada módulo** va en `docs/diseno/<modulo>.md` y `<modulo>-diagramas.md`, con reglas de dominio identificadas (`U-01`, `C-02`...) y una prueba de aceptación por regla.
- **Skills y regla permanente para agentes de Gemini en Antigravity** en `.agent/skills/` (la base es `negocio360-reglas`) y `.agent/rules/negocio360.md` (se carga en cada turno). Si cambia una regla, se cambia en las skills y en los documentos donde aparezca.

## Siguientes pasos pendientes

> Los pendientes actualizados y las decisiones abiertas están en [`docs/BITACORA.md`](docs/BITACORA.md). La lista de abajo es la original, con notas de avance.

- [ ] Definir esquema de base de datos y políticas de seguridad por fila. (Inventario ya está diseñado en `docs/diseno/inventario-diagramas.md`; faltan los demás módulos.)
- [ ] Contratos JSON de los endpoints y códigos de error. (Inventario: `docs/diseno/inventario.md`; faltan los demás módulos.)
- [ ] Esquema JSON de extracción del LLM.
- [ ] Tabla de estados e intenciones permitidas.
- [ ] Formato del archivo de reglas.
- [ ] Inventario de fuentes de datos.
- [ ] Descripción ejecutiva (primer entregable).
- [ ] Reclutar negocios para el piloto.
- [ ] Nuevos (2026-10-08): aprobar el diseño de Inventario y resolver las decisiones propuestas; decidir el modelo de IA, si el bot continúa, el giro real de la demo, PWA única o dos apps y dónde vive la base de la demo; definir el camino dorado de la demo; probar las skills en Antigravity.
