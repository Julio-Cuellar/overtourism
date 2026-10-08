# Skills del proyecto Negocio 360 para agentes de Antigravity

Este paquete le da a los agentes de Google Gemini el contexto, las reglas y los procedimientos del proyecto. Cada carpeta contiene un `SKILL.md`. Antigravity lee la `description` de cada skill y carga la que coincide con la tarea.

**La skill base es `negocio360-reglas`.** Todas las demás la complementan y, si chocan con ella, gana ella.

## Índice

| Skill | Úsala cuando... | Produce |
|---|---|---|
| `negocio360-reglas` | Siempre que se trabaje en este repositorio | Orden de lectura, compuerta de código, principio rector, reglas R1 a R6, prohibiciones |
| `planificar-funcionalidad` | Piden un plan, una estimación, un cronograma, repartir trabajo, recortar alcance o replanificar | Plan por nivel (rápido, estándar o mayor) con requisitos y criterios, fases, reparto, estimación con rango, veredicto de calendario y riesgos; espera confirmación. Incluye plantillas, método de estimación, replanificación y un ejemplo real |
| `disenar-modulo` | Hay que diseñar o ampliar un módulo | `docs/diseno/<modulo>.md` y `<modulo>-diagramas.md` |
| `contratos-eventos` | Se crea o cambia un evento, o se conecta un listener | Contrato de evento versionado y lista para consumidores |
| `contratos-api` | Se define GraphQL, REST, WebSocket o la API pública de un módulo | Operaciones, errores y reglas por protocolo |
| `modelar-base-de-datos` | Se crea o cambia una tabla, un índice o una migración | Tablas con tipos, restricciones, índices y retención |
| `diagramas-mermaid` | Piden un diagrama, o uno no se dibuja | Secuencias, entidad-relación, estados y flujos sin errores de sintaxis |
| `revisar-diseno` | Piden revisar o auditar un diseño | Hallazgos con escenario y severidad, y un veredicto |
| `revisar-codigo` | Hay código autorizado y piden revisarlo | Hallazgos por archivo y línea, y un veredicto |
| `escribir-pruebas` | Hay código autorizado y se escribe o mejora una prueba | Pruebas primero (TDD), una por regla del diseño |
| `revisar-seguridad` | Se toca autenticación, entradas, bot, IA, ubicación o secretos | Hallazgos de seguridad y un veredicto |
| `integrar-ia` | Se diseña algo con un LLM, o se documenta la IA | Reglas de uso, evaluación y ficha de componente |
| `documentar-fuentes-de-datos` | Se usa, limpia o cita un dataset o una cifra | Ficha por fuente y proceso de limpieza |
| `registrar-en-bitacora` | Termina una sesión, o se retoma una | Entrada en `docs/BITACORA.md` o resumen para retomar |
| `disenar-interfaz` | Piden una pantalla, un flujo de usuario o los textos de la interfaz | Flujo, pantallas con estados, textos y operaciones de API |
| `generar-mockup-html` | Piden un mockup, una maqueta o un modelo visual de un diseño | Una página HTML en `docs/mockups/`, con plantilla, tema claro y oscuro y verificación |
| `preparar-demo-hackathon` | Se habla de la demo, el pitch o la rúbrica | Guion, plan B, recorte y lista previa a la entrega |
| `diagnosticar-fallo` | Algo falla, no cuadra o no funciona | Diagnóstico con causa raíz y prueba de regresión |

## Cómo se combinan (flujos típicos)

- **Módulo nuevo:** `planificar-funcionalidad` → `disenar-modulo` (usa `modelar-base-de-datos`, `contratos-api`, `contratos-eventos`, `diagramas-mermaid`) → `revisar-diseno` → `registrar-en-bitacora`.
- **Cambiar un contrato:** `contratos-eventos` o `contratos-api` → `revisar-diseno` → `registrar-en-bitacora`.
- **Con código autorizado:** `escribir-pruebas` → implementar → `revisar-codigo` → `revisar-seguridad` → `registrar-en-bitacora`.
- **Algo no cuadra:** `diagnosticar-fallo` → `escribir-pruebas` (regresión) → `registrar-en-bitacora`.
- **Cerca de la entrega:** `preparar-demo-hackathon` + `integrar-ia` + `documentar-fuentes-de-datos`.

## Instalación

Las skills viven en el repositorio, en `.agent/skills/`, y Antigravity las detecta desde el proyecto. La documentación actual usa `.agents/skills/` como ruta por defecto y mantiene compatibilidad con `.agent/skills/`; si la versión del equipo solo lee `.agents/`, renombra la carpeta `.agent` a `.agents` y corrige las rutas en los documentos.

Para tenerlas en todos tus proyectos (normalmente no conviene, son de este proyecto), la ruta global según la documentación es `~/.gemini/config/skills/` en Antigravity 2.0 y en el IDE; el IDE acepta también `~/.gemini/antigravity/skills/`, y la CLI usa `~/.gemini/antigravity-cli/skills/`. Verifica la ruta en la documentación de tu versión.

Junto a las skills hay una **regla permanente** en `.agent/rules/negocio360.md` (se carga en cada turno y remite a `negocio360-reglas`).

## Cómo mantenerlas

- Cuando una decisión pase de propuesta a aceptada, actualiza la sección «Instantánea del estado» de `negocio360-reglas`.
- Si cambias una regla de arquitectura, cámbiala en **todas** las skills y documentos donde aparezca. Búscala con el texto de la regla.
- Cada skill nueva: una carpeta con `SKILL.md`, `name` en minúsculas con guiones, y una `description` que diga **cuándo usarla** con palabras que el usuario realmente escribiría.
- Agrega la skill a este índice y regístrala en `docs/BITACORA.md`.

## Lo que estas skills no hacen
No autorizan escribir código. Mientras la Bitácora diga «fase de diseño», ninguna skill debe usarse para crear proyecto, clases, migraciones o pruebas.
