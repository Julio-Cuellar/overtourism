---
name: planificar-funcionalidad
description: "Planifica una funcionalidad, módulo o cambio del proyecto Negocio 360 antes de construirlo, de principio a fin: reformula el pedido, lo contrasta con las decisiones vigentes, define requisitos con criterios de aceptación, alcance por prioridad y orden de recorte, descompone en fases y tareas, ordena dependencias, reparte el trabajo entre el equipo, estima con rango, calcula si cabe en el calendario, lista riesgos y espera aprobación. También replanifica cuando cambia el alcance o falta tiempo. Úsala cuando pidan un plan, una estimación, 'cómo lo hacemos', priorizar, recortar, repartir trabajo, un cronograma, o antes de tocar más de un archivo."
---

# Planificar una funcionalidad

> Complementa `negocio360-reglas`. Si chocan, gana esa skill.

Esta skill cubre el ciclo completo del plan. Los detalles largos están en `references/`: plantillas (`plantilla-plan.md`), cómo estimar (`estimacion.md`), cómo replanificar (`replanificacion.md`) y un ejemplo real del proyecto (`ejemplo-plan-inventario.md`). Léelos cuando el paso lo pida.

## Regla de oro
- Un plan **no es código ni diseño detallado**. El plan dice qué se hace, en qué orden, cuánto cuesta, quién y qué puede salir mal. El *cómo* detallado lo resuelve `disenar-modulo`.
- Termina **siempre** pidiendo confirmación y **no ejecutes nada** hasta recibirla.
- Aprobar un plan no autoriza escribir código por sí solo. Mira la sección «Después de la confirmación».

## 1. Elige el nivel del plan

| Nivel | Cuándo | Qué contiene |
|---|---|---|
| **Rápido** | Hasta un día-persona, uno o dos archivos, sin cambiar contratos | Qué entendí · pasos · cómo se valida · riesgos (1 a 3) · pregunta si hay |
| **Estándar** | De uno a cinco días-persona, una funcionalidad | Plantilla estándar completa |
| **Mayor** | Un módulo nuevo, un cambio de arquitectura, más de cinco días-persona, o algo que cambia contratos | Plantilla mayor: además requisitos con criterios de aceptación, grafo de dependencias, reparto y calendario por hitos |

Ante la duda, sube un nivel. Un plan corto de algo grande sale caro.

## 2. Procedimiento

**Paso 0. Clasifica el pedido.** ¿Plan nuevo, ampliación de alcance, replanificación (algo cambió) o recorte (falta tiempo)? Si es replanificación o recorte, sigue `references/replanificacion.md` en vez de empezar de cero.

**Paso 1. Reúne contexto.**
- Lee `docs/BITACORA.md` (fase, decisiones, pendientes, hechos del entorno), el diseño del módulo y `CLAUDE.md`.
- Fecha de hoy: del sistema o de la última entrada de la Bitácora. Días que faltan para el 15 de octubre de 2026.
- Equipo: tres estudiantes. Pregunta disponibilidad real (clases, otros compromisos) y quién domina qué, si no está registrado.
- Entorno: lo que diga la Bitácora (Java, Maven, Docker, base de datos). Puede haber cambiado; no lo supongas.

**Paso 2. Reformula.** Tres a cinco líneas: qué se construye, para quién y por qué ahora. Después, tres listas cortas: **qué queda fuera**, **qué supones** y **qué no sabes**.

**Paso 3. Contrasta con las decisiones.** Tabla con las `D-xx` que aplican y su estado real (aceptada, propuesta, abierta). Si lo pedido contradice una decisión aceptada, **detente** y señálalo antes de seguir. Si depende de una decisión abierta, dilo y marca qué se bloquea.

**Paso 4. Requisitos y criterios de aceptación** (nivel Mayor; en Estándar, resumidos).
- Requisitos funcionales numerados (`RQ-01`...), cada uno en una frase.
- Requisitos no funcionales que importan en este proyecto: aislamiento entre negocios, idempotencia, seguridad, rendimiento razonable, datos simulados etiquetados.
- Cada requisito lleva un **criterio comprobable**: «dado..., cuando..., entonces...» o una medida. Si no se puede comprobar, reescríbelo.
- Si hay reglas del diseño (`U-01`, `C-02`...), cítalas.
- Señala qué criterios de la **rúbrica del hackathon** mueve cada requisito (datos 20%, problema 15%, innovación 15%, técnica 15%, impacto 15%, viabilidad 10%, UX 5%, pitch 5%).

**Paso 5. Alcance y prioridades.**
- **Camino dorado:** el flujo de punta a punta que la demo necesita. Lo que está en él es prioridad 1.
- Prioridad 1 (imprescindible), 2 (importante), 3 (deseable). Y **fuera del MVP** (hoja de ruta).
- **Orden de recorte:** de abajo hacia arriba y nunca a la mitad de algo. Escríbelo explícito.

**Paso 6. Descompón en fases y tareas.**
- Prefiere **rebanadas verticales** (algo que funciona de punta a punta y se puede demostrar) a capas horizontales (todo el modelo, luego toda la API...). Excepción: una fase 0 de cimientos mínimos.
- Cada fase tiene: entregable, **criterio de salida comprobable**, estimado, responsable y de qué depende.
- Tareas de **medio día o menos**. Si una tarea es mayor, pártela.
- Incluye siempre como tareas explícitas: pruebas, documentación, datos de prueba, revisión y preparación de la demo. Se olvidan primero y cuestan más.
- Si hay código autorizado, cada tarea sigue pruebas primero. Si no, la fase es de diseño y se valida con `revisar-diseno`.

**Paso 7. Dependencias.**
- Qué bloquea qué (lista o diagrama de texto).
- **Contratos que hay que congelar primero**: congelarlos es lo que permite trabajar en paralelo.
- Decisiones abiertas que bloquean, con quién debe responder y para cuándo.
- Terceros: otros módulos, quién es dueño de Catálogo o Ventas, claves de IA, base de datos.

**Paso 8. Reparte el trabajo.**
- Una persona por módulo o rebanada, para no editar los mismos archivos.
- Fechas de **integración** (cuándo se juntan las piezas) y quién revisa a quién.
- Si no sabes quién es quién, usa A, B y C y pregunta nombres y fortalezas.
- Una persona nunca es el único punto de falla de la ruta crítica sin decirlo.

**Paso 9. Estima.** Sigue `references/estimacion.md`: tareas pequeñas, rango (optimista, probable, pesimista), supuestos a la vista y colchón. Nunca des un número sin su base.

**Paso 10. Calendario.**
- **Demanda** (estimado con colchón) contra **capacidad** (personas × días × horas efectivas).
- Ruta crítica en días calendario y los hitos con fecha: contratos congelados, integración, **congelamiento de funciones** unos días antes de la demo, ensayo.
- Veredicto claro: **cabe**, **cabe justo** o **no cabe**.
- Si no cabe, ofrece opciones con su costo: (a) recortar según el orden de recorte, (b) más horas o más gente, (c) mover un hito. Nunca comprimas estimaciones en silencio.

**Paso 11. Riesgos.** Tabla con probabilidad, impacto, mitigación, señal de alerta y dueño. Casi siempre aplican: calendario, entorno (Docker, base de datos), dependencia entre personas, contrato sin congelar, cuota o fallo de la IA, datos no disponibles, alcance que crece.

**Paso 12. Cómo se valida cada fase.** Un criterio observable por fase. Con código: comandos de prueba y cobertura. Sin código: revisión del documento y aprobación del equipo.

**Paso 13. Preguntas abiertas.** Máximo cuatro. Cada una con tu recomendación y lo que pasa con cada opción. Solo las que cambian el plan.

**Paso 14. Entrega y espera.** Presenta el plan en el formato de `references/plantilla-plan.md`. Cierra con una línea que diga **qué necesitas que responda el usuario**. No ejecutes nada.

## 3. Cómo interpretar la respuesta del usuario

| El usuario dice... | Significa | Qué haces |
|---|---|---|
| «sí», «aprobado», «adelante» | Aprueba el plan | Registra lo aprobado como `ACEPTADA`. Si el plan incluye programar, pregunta **una vez**: «¿Esto autoriza empezar a programar la fase X?» |
| «aprobado salvo 3 y 7» | Aprueba todo menos esas preguntas | Acepta lo demás, deja 3 y 7 como `ABIERTA` y no avances en lo que dependa de ellas |
| «modifica: ...» | Pide cambios | Replanifica (`references/replanificacion.md`) y vuelve a pedir confirmación |
| «otra opción: ...» | Rechaza el enfoque | Presenta la alternativa con sus costos. No defiendas el anterior sin razones |
| «salta la fase 2 y haz la 3» | Cambia el orden | Verifica dependencias, avisa si algo se rompe y confirma |
| «ok» o algo ambiguo | No es una aprobación clara | Pregunta una vez: «¿Apruebo el plan tal cual y empiezo por la fase X?» |

## 4. Después de la confirmación

1. Registra en la Bitácora (con `registrar-en-bitacora`): el plan resumido, las decisiones aceptadas y las que quedaron abiertas.
2. Pasa a la primera fase **autorizada**. Si es de diseño, usa `disenar-modulo`. Si es de código, solo con autorización explícita anotada en la Bitácora.
3. No amplíes el alcance aprobado. Cualquier cosa nueva es un cambio y se replanifica.
4. Al terminar cada fase, compara estimado y real y anótalo (calibra las siguientes estimaciones).

## 5. Autoevaluación antes de entregar

- [ ] ¿Reformulé y dije qué queda fuera, qué supongo y qué no sé?
- [ ] ¿Cada decisión citada tiene su estado correcto?
- [ ] ¿Cada requisito tiene un criterio comprobable?
- [ ] ¿Identifiqué el camino dorado de la demo?
- [ ] ¿Escribí el orden de recorte?
- [ ] ¿Cada fase tiene entregable, criterio de salida, estimado, responsable y dependencias?
- [ ] ¿Marqué qué va en paralelo y cuál es la ruta crítica?
- [ ] ¿La estimación tiene rango, supuestos y colchón, e incluye pruebas, documentación y demo?
- [ ] ¿Comparé demanda contra capacidad y di «cabe», «cabe justo» o «no cabe»?
- [ ] ¿Los riesgos tienen mitigación y dueño?
- [ ] ¿Las preguntas son cuatro o menos y llevan recomendación?
- [ ] ¿Terminé esperando confirmación, sin haber creado nada?

## 6. Errores comunes
- Estimar solo escribir código y olvidar pruebas, migraciones, documentación, integración y demo.
- Repartir el trabajo como si tres personas fueran tres veces más rápidas (la integración cuesta).
- Planear por capas y descubrir en la última semana que nada funciona de punta a punta.
- No identificar el camino dorado y dejarlo para el final.
- Dar por aceptada una decisión que sigue como propuesta.
- Meter funciones que el usuario no pidió.
- Plan largo que no dice qué se recorta primero.
- Cambiar el plan en silencio en lugar de mostrar qué cambió.
- Suponer disponibilidad completa de estudiantes con clases.
- No incluir una fase de **revisión del diseño**: al dibujar los flujos de Inventario aparecieron siete huecos que el plan no preveía.

## 7. Referencias
- `references/plantilla-plan.md`: plantillas estándar y mayor, listas para llenar.
- `references/estimacion.md`: cómo estimar, comparar con la capacidad y calibrar.
- `references/replanificacion.md`: qué hacer cuando cambia el alcance o falta tiempo.
- `references/ejemplo-plan-inventario.md`: el plan real de Inventario, de la versión 1 a la 2, con sus lecciones.
