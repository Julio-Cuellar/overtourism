# overtourism

Proyecto **Negocio 360** para el hackathon **Puebla Turística 360°: Datos que transforman experiencias** (Tech-Capital Puebla 2026, del 15 al 17 de octubre de 2026).

Es una plataforma modular de turismo para Puebla: módulos **horizontales** que comparten todos los giros (inventario, caja, ventas, clientes, contabilidad, catálogo) y módulos **verticales** por giro (restaurantes, museos, misceláneos), con una app para turistas, un pasaporte con cupones, rutas generadas con IA y un panel para locatarios.

## Estado

> Al 2026-10-08 el proyecto está en **fase de diseño**. **Todavía no existe código** y no se debe programar hasta que el equipo lo autorice por escrito.
> La fuente de verdad del estado es [`docs/BITACORA.md`](docs/BITACORA.md). Si esta página dice otra cosa, manda la Bitácora.

Stack decidido: **Java 21 con Spring Boot**. El resto (Maven, base de datos de la demo, modelo de IA) sigue como propuesta o abierto; está en la Bitácora.

## Estructura del repositorio

```
.
├── README.md                    Esta página
├── CLAUDE.md                    Especificación del proyecto (actualizada el 2026-10-08; lo previo al cambio de rumbo está rotulado «original»)
├── docs/
│   ├── BITACORA.md              Memoria del proyecto: estado, decisiones, pendientes
│   ├── diseno/                  Diseño de módulos: reglas, contratos, diagramas y datos
│   └── mockups/                 Mockups en HTML (se crea al generar el primero)
├── .gitignore                   Lo que no se sube (secretos, compilados, archivos del IDE)
└── .agent/
    ├── rules/                   Regla permanente: se carga en cada turno
    └── skills/                  Skills para agentes de Gemini en Antigravity
```

## Por dónde empezar

1. Clona el repositorio:
   ```
   git clone https://github.com/Julio-Cuellar/overtourism.git
   ```
2. Lee [`docs/BITACORA.md`](docs/BITACORA.md): las secciones 1 a 6 y las dos últimas entradas del registro.
3. Si vas a trabajar en un módulo, lee su diseño en `docs/diseno/`. El de Inventario es el modelo a seguir.
4. Crea o cambia a tu rama de trabajo (ver «Ramas de trabajo»). No trabajes directo en `main`.
5. Abre la carpeta en Antigravity y sigue la guía de skills que está más abajo.

## Ramas de trabajo

| Rama | Para qué |
|---|---|
| `main` | Lo que está listo y aprobado. Nadie trabaja directo aquí. |
| `pre-main` | Última parada antes de `main`: aquí se junta todo y se prueba en conjunto. |
| `liz`, `uriel`, `julio` | La rama de trabajo de cada integrante. |
| `liz-testing`, `uriel-testing`, `julio-testing` | La rama de pruebas de cada integrante: ahí se verifica el trabajo antes de pasarlo a `pre-main`. |

Flujo propuesto (el equipo puede ajustarlo):

```
liz   ──▶ liz-testing   ──┐
uriel ──▶ uriel-testing ──┼──▶ pre-main ──▶ main
julio ──▶ julio-testing ──┘
```

1. Trabajas en tu rama.
2. Cuando esté listo, lo pasas a tu rama de testing y lo pruebas.
3. Si pasa, se integra en `pre-main` mediante un pull request que revise otra persona.
4. En `pre-main` se prueba todo junto.
5. De `pre-main` se pasa a `main`.

Notas:
- Los nombres llevan guion (`liz-testing`) y no barra (`liz/testing`) porque git no permite tener una rama `liz` y otra `liz/testing` al mismo tiempo.
- Las ramas se crearon y se subieron a GitHub el 2026-10-08, a partir del primer commit. **Hoy están un commit atrás de `main`:** no incluyen `docs/`, `.agent/`, `CLAUDE.md` ni `.gitignore`, que entraron a `main` en el commit `7de219d`. Antes de trabajar en una, actualízala con `main`: `git switch <tu-rama>` y `git merge main`.
- Se recomienda proteger `main` y `pre-main` en la configuración del repositorio en GitHub, para que solo se les pueda integrar por pull request.
- Cuando se integre algo a `pre-main` o a `main`, conviene que las demás ramas se actualicen con `git merge` desde la rama destino, para no trabajar sobre una versión vieja.

---

# Cómo usar las skills con Gemini en Antigravity

Las skills le dan a los agentes el contexto del proyecto, sus reglas y procedimientos para tareas concretas (planificar, diseñar, revisar, generar mockups...). Sirven para que un agente nuevo no empiece de cero, no pierda contexto y no haga lo que no debe, como programar antes de que se apruebe el diseño.

## Qué son y cómo se cargan

- Una skill es una carpeta dentro de `.agent/skills/` con un archivo `SKILL.md`. Algunas traen archivos de apoyo (`references/`, `assets/`).
- Cada `SKILL.md` empieza con un encabezado con `name` y `description`. **Antigravity compara tu pedido con la `description` de cada skill y carga las que coinciden.** El resto del archivo solo se lee cuando la skill se activa.
- No hay nada que instalar: viajan con el repositorio.
- Además hay una **regla permanente** en `.agent/rules/negocio360.md`, con `trigger: always_on`. Antigravity la inyecta en **cada turno** y manda al agente a leer la Bitácora y la skill `negocio360-reglas`. Las skills se cargan solo cuando coinciden con la tarea; la regla se carga siempre.
- **Rutas.** La documentación actual de Antigravity usa `.agents/` (en plural) como carpeta por defecto y mantiene compatibilidad con `.agent/`. Este repositorio usa `.agent/`, que sigue aceptada. Si la versión que usan solo lee `.agents/`, renombren la carpeta (`git mv .agent .agents`) y corrijan las rutas en los documentos.

## Preparación (una sola vez)

1. Abre en Antigravity la **carpeta raíz del repositorio** (la que contiene `.agent/`), no una subcarpeta. Si abres otra carpeta, las skills no se detectan.
2. Comprueba que existe `.agent/skills/` con 17 carpetas y un `README.md` de índice.
3. Haz una prueba rápida (sección «Comprobar que funcionan»).

## Las 17 skills

| Skill | Úsala cuando... |
|---|---|
| `negocio360-reglas` | **Siempre.** Es la base: qué leer primero, cuándo se puede programar, reglas de arquitectura y prohibiciones |
| `planificar-funcionalidad` | Quieres un plan, una estimación, un cronograma, repartir trabajo, recortar alcance o replanificar |
| `disenar-modulo` | Hay que diseñar o ampliar un módulo |
| `contratos-eventos` | Se crea o cambia un evento del bus |
| `contratos-api` | Se define GraphQL, REST, WebSocket o la API entre módulos |
| `modelar-base-de-datos` | Se crea o cambia una tabla, un índice o una migración |
| `diagramas-mermaid` | Quieres un diagrama de secuencia, de estados, entidad-relación o de flujo |
| `revisar-diseno` | Quieres que revisen un diseño antes de aprobarlo |
| `revisar-codigo` | Ya hay código autorizado y quieres revisarlo |
| `escribir-pruebas` | Ya hay código autorizado y quieres escribir pruebas primero (TDD) |
| `revisar-seguridad` | Se toca autenticación, entradas, bot, IA, ubicación o secretos |
| `integrar-ia` | Se diseña algo con un modelo de lenguaje o se documenta la IA |
| `documentar-fuentes-de-datos` | Se usa, limpia o cita un dataset o una cifra |
| `registrar-en-bitacora` | Termina una sesión o se retoma una |
| `disenar-interfaz` | Quieres pantallas, flujos de usuario o textos de interfaz |
| `generar-mockup-html` | Quieres un mockup visual en una página HTML |
| `preparar-demo-hackathon` | Preparas la demo, el pitch o la rúbrica |
| `diagnosticar-fallo` | Algo falla, no cuadra o no funciona |

El índice con más detalle está en [`.agent/skills/README.md`](.agent/skills/README.md).

## Cómo pedir las cosas

Escribe el pedido con claridad y **menciona la skill por su nombre**. Mencionarla ayuda a que se cargue, aunque no hay garantía.

| Quieres... | Ejemplo de mensaje |
|---|---|
| Empezar bien una sesión | «Antes de empezar, lee `docs/BITACORA.md` y usa `negocio360-reglas`. Dime en qué estado está el proyecto y qué está autorizado.» |
| Un plan | «Con `planificar-funcionalidad`, planifica el módulo de Ventas. Considera que somos tres personas y cuántos días faltan.» |
| Diseñar un módulo | «Con `disenar-modulo`, diseña el módulo de Caja. Usa el de Inventario como modelo.» |
| Un evento nuevo | «Con `contratos-eventos`, define el evento `caja.cerrada`: campos, quién lo publica y quién lo consume.» |
| Operaciones de API | «Con `contratos-api`, lista las consultas de GraphQL del panel de ventas y qué va por WebSocket.» |
| Tablas | «Con `modelar-base-de-datos`, modela las tablas de Caja con sus restricciones e índices.» |
| Un diagrama | «Con `diagramas-mermaid`, haz la secuencia de un corte de caja, con el caso de error.» |
| Revisar un diseño | «Con `revisar-diseno`, revisa `docs/diseno/inventario.md`. Dame hallazgos con escenario y severidad.» |
| Un mockup | «Con `generar-mockup-html`, genera un mockup de la ficha del negocio con sus cinco estados.» |
| Pantallas | «Con `disenar-interfaz`, diseña el flujo del cuestionario de intereses.» |
| Revisar seguridad | «Con `revisar-seguridad`, revisa el diseño del bot de Telegram.» |
| Documentar la IA | «Con `integrar-ia`, escribe la ficha del componente que genera rutas.» |
| Fuentes de datos | «Con `documentar-fuentes-de-datos`, haz la ficha de DENUE.» |
| Preparar la demo | «Con `preparar-demo-hackathon`, propón el guion de la demo y qué recortar si falta tiempo.» |
| Un error | «Con `diagnosticar-fallo`, el saldo no cuadra después de una anulación. Ayúdame a encontrar la causa.» |
| Cerrar la sesión | «Con `registrar-en-bitacora`, cierra la sesión: decisiones, pendientes y archivos.» |
| Retomar | «Con `registrar-en-bitacora`, retoma el proyecto: resume en qué quedamos y pregúntame qué sigue.» |

## Flujos típicos

- **Módulo nuevo:** `planificar-funcionalidad` → `disenar-modulo` (que usa `modelar-base-de-datos`, `contratos-api`, `contratos-eventos` y `diagramas-mermaid`) → `revisar-diseno` → `registrar-en-bitacora`.
- **Cambiar un contrato:** `contratos-eventos` o `contratos-api` → `revisar-diseno` → `registrar-en-bitacora`.
- **Cuando haya código autorizado:** `escribir-pruebas` → implementar → `revisar-codigo` → `revisar-seguridad` → `registrar-en-bitacora`.
- **Algo no cuadra:** `diagnosticar-fallo` → `escribir-pruebas` (prueba de regresión) → `registrar-en-bitacora`.
- **Cerca de la entrega:** `preparar-demo-hackathon`, `integrar-ia` y `documentar-fuentes-de-datos`.

## Comprobar que funcionan

No se ha probado el paquete dentro de Antigravity, así que haz esta comprobación antes de confiar en él:

1. Pregunta: «¿Qué skills de este proyecto tienes disponibles?». Debe nombrar varias de la tabla de arriba. Pregunta también: «¿Qué reglas permanentes tienes en este proyecto?». Debe mencionar las de `.agent/rules/negocio360.md`.
2. Pide una tarea sencilla, por ejemplo: «Con `negocio360-reglas`, dime qué puedo y qué no puedo hacer hoy en este proyecto».
3. Pregunta después: «¿Qué skill usaste y qué archivos leíste?».

**Buena señal:** el agente lee primero `docs/BITACORA.md`, dice que estamos en fase de diseño y se niega a crear carpetas de código.
**Mala señal:** empieza a crear un proyecto Maven, carpetas `src` o clases sin que lo autorizaras. Detenlo y repítele: «Lee `negocio360-reglas`: aún no está autorizado programar».

## Si una skill no se activa

1. Nómbrala en el mensaje, con su nombre exacto (por ejemplo `contratos-eventos`).
2. Confirma que abriste la carpeta raíz del repositorio y que `.agent/skills/<nombre>/SKILL.md` existe.
3. Pídele al agente que lea el archivo: «Lee `.agent/skills/contratos-eventos/SKILL.md` y síguelo».
4. Cierra y vuelve a abrir el proyecto, o abre una conversación nueva, por si no refrescó la lista de skills.
5. Si una skill se activa cuando no debe, o no cuando sí debe, ajusta su `description` (es la primera parte del `SKILL.md`): debe decir **cuándo usarla** con palabras que alguien realmente escribiría.
6. Consulta la documentación de tu versión de Antigravity: la ubicación o el comportamiento pueden cambiar entre versiones.

## Skills en todos tus proyectos (opcional)

Antigravity también lee skills de una carpeta global del usuario. Según su documentación: en Antigravity 2.0 y en el IDE, `~/.gemini/config/skills/`; el IDE acepta además la ruta anterior `~/.gemini/antigravity/skills/`; la CLI usa `~/.gemini/antigravity-cli/skills/`. En Windows, `~` es `%USERPROFILE%`. Verifica la ruta en la documentación de tu versión.

Estas skills están escritas para este proyecto. Si las copias a la carpeta global, se aplicarán también a tus otros proyectos. Lo normal es **dejarlas solo en el repositorio**.

## Reglas del equipo al trabajar con agentes

1. **El agente propone y ustedes deciden.** Lean lo que escribe antes de aceptarlo.
2. **Nada de código hasta que esté autorizado.** La Bitácora dice la fase. Si un agente empieza a programar en fase de diseño, deténganlo.
3. **Una tarea clara por agente** y, mientras sea posible, **un solo agente por archivo**. Dos agentes editando el mismo archivo se pisan.
4. **Cierren cada sesión en la Bitácora** con `registrar-en-bitacora`: decisiones, pendientes y archivos. Lo que no se escribe, el siguiente agente no lo sabe.
5. **Distingan lo propuesto de lo aceptado.** Un agente no puede dar por aceptada una decisión que ustedes no confirmaron.
6. **Pidan siempre el reporte honesto**: qué hizo, qué verificó y qué no pudo verificar.
7. **No pongan secretos** (claves de IA, tokens, contraseñas) en archivos ni en mensajes al agente. Usen variables de entorno.
8. **Revisen los cambios antes de subirlos a GitHub** (`git status` y `git diff`): que no suban claves ni archivos que no esperaban. El `.gitignore` ya excluye `.env`, claves, `target/` y archivos del IDE, pero no sustituye la revisión ni afecta archivos que ya se hayan confirmado.
9. **Si cambian una regla de arquitectura**, cámbienla en todas las skills y documentos donde aparezca.

## Mantener las skills

- Cuando una decisión pase de propuesta a aceptada, actualiza la «Instantánea del estado» (sección 10) de `.agent/skills/negocio360-reglas/SKILL.md`.
- Para agregar una skill: una carpeta con `SKILL.md`, `name` en minúsculas con guiones y una `description` que diga cuándo usarla. Agrégala a `.agent/skills/README.md` y regístrala en la Bitácora.
- Los cambios a las skills se revisan igual que el resto del repositorio.
- Si cambia una regla de arquitectura o una prohibición, actualiza también `.agent/rules/negocio360.md`, que resume lo esencial y se carga en cada turno. Mantenla corta: Antigravity limita el tamaño de las reglas y comparte un presupuesto entre todas las que se cargan siempre.

## Límites de las skills

- **No se probaron dentro de Antigravity** ni las skills ni la regla permanente. Es posible que alguna `description` no active la skill en el momento correcto, o que la regla se descarte si su encabezado no es válido (Antigravity descarta en silencio las reglas con un `trigger` inválido).
- **Un agente puede equivocarse o ignorar una regla.** Las skills reducen ese riesgo, no lo eliminan. Revisen el trabajo.
- **No autorizan escribir código.** Ninguna skill reemplaza la aprobación del equipo.
- **Los nombres de herramientas** de pruebas y versiones de librerías que mencionan salen de conocimiento general; hay que verificar versiones compatibles antes de usarlas.

---

## Si usas Claude Code

`CLAUDE.md` se lee automáticamente al abrir el proyecto en Claude Code. Las skills de `.agent/skills/` están pensadas para Antigravity; Claude Code no las carga solo, pero puedes pedirle que lea `.agent/skills/negocio360-reglas/SKILL.md` y la Bitácora. Las reglas son las mismas.

## Documentos clave

- [`docs/BITACORA.md`](docs/BITACORA.md): estado, decisiones y pendientes.
- [`docs/diseno/inventario.md`](docs/diseno/inventario.md): diseño del módulo de Inventario (reglas, contratos de API y eventos).
- [`docs/diseno/inventario-diagramas.md`](docs/diseno/inventario-diagramas.md): secuencias, eventos y modelo de datos de Inventario.
- [`CLAUDE.md`](CLAUDE.md): especificación original del proyecto.
- [`.agent/skills/README.md`](.agent/skills/README.md): índice de las skills.
