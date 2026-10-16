# Ejemplo real: el plan de Inventario (v1 y v2)

Es el plan que se hizo el 2026-10-08 para el módulo de Inventario. Sirve para ver el formato en uso y las lecciones. Las estimaciones eran las de ese momento: no son vinculantes ni una referencia de productividad.

## Contexto
- Stack fijado: Java 21 y Spring Boot (D-03). Tres reglas de arquitectura dadas por el usuario; el asistente adoptó las tres, corrigió la regla 2 (síncrono = API pública, asíncrono = evento) y propuso R4, R5 y R6.
- Fecha: 2026-10-08. Demo: 2026-10-15. Siete días.
- El `CLAUDE.md` original dejaba inventario fuera de alcance.

## Plan v1 (MVP mínimo)

| Prioridad | Funcionalidad |
|---|---|
| 1 | Artículos, movimientos, saldo, kardex |
| 1 | Escrituras con preparar → confirmar |
| 1 | Descuento automático por venta (vínculo 1:1) |
| 1 | Alertas de bajo mínimo y reposición |
| 2 | Consultas del panel por GraphQL y una suscripción de alertas |

| Fase | Entregable | Estimado |
|---|---|---|
| 0 | Esqueleto, módulo compartido, Postgres con Testcontainers, Flyway | 0.5 |
| 1 | Dominio y persistencia | 1 |
| 2 | API pública y preparar → confirmar | 1 |
| 3 | Consumo de ventas, idempotente | 0.5 |
| 4 | GraphQL y suscripción | 1 |
| 5 | (Opcional) costo promedio | 0.5 |
| **Total** | | **4 a 4.5** |

Riesgos de v1: el inventario le quita tiempo a lo que se presenta (alta); Ventas aún no existe y nadie publica `venta.registrada` (media); el equipo no conoce Spring Modulith (media); saldos inconsistentes por concurrencia (baja); eventos duplicados (media).

**Preguntas que hizo v1:** adoptar la regla 2 corregida y R4 a R6; stock negativo con alerta, ajuste por conteo y una sola unidad por artículo; recetas fuera del MVP; Gradle o Maven.

## Cambio: el usuario amplía el alcance
Pidió sumar lotes, caducidades, números de serie, conversión de unidades, costo promedio ponderado, recetas y descuento de insumos por platillo.

## Plan v2 (replanificado)

| Prioridad | Funcionalidad | Dueño |
|---|---|---|
| 1 | Artículos, movimientos, saldo, kardex | Inventario |
| 1 | Conversión de unidades | Inventario |
| 1 | Costo promedio ponderado | Inventario |
| 1 | Recetas y descuento por platillo | **Restaurantes** |
| 2 | Lotes y caducidades con salida FEFO | Inventario |
| 3 | Números de serie | Inventario |

| Fase | Entregable | Estimado | Quién |
|---|---|---|---|
| 0 | Esqueleto, verificación de módulos | 0.5 | A |
| 1 | Dominio base y conversión de unidades | 1.5 | A |
| 2 | Movimientos valuados con costo promedio | 1 | A |
| 3 | API pública, preparar → confirmar, eventos (contratos congelados aquí) | 1 | A |
| 4 | Recetas en Restaurantes, consumo idempotente, reversión | 2 | B |
| 5 | Lotes, FEFO, caducidades | 2 | C |
| 6 | Números de serie | 1.5 | C |
| 7 | GraphQL y suscripción | 1.5 | A |
| **Total** | | **≈ 11 días-persona** | |

- **Ruta crítica en paralelo:** unos 5 días.
- **Complejidad:** de media a alta.
- **Orden de recorte (aceptado):** primero series, luego lotes.
- **Veredicto de calendario:** el alcance pasó de unos 4.5 a unos 11 días-persona con siete días hasta la demo; el asistente avisó que casi no dejaba tiempo para ventas, la app del turista, las rutas con IA y el pasaporte.

**Delta v1 → v2 (cómo se debió presentar):**

| Elemento | v1 | v2 | Efecto |
|---|---|---|---|
| Alcance | MVP mínimo | + conversión, costo promedio, lotes, series, recetas | +6.5 días-persona |
| Fases | 0 a 5 | 0 a 7 | +2 fases |
| Dependencias | Ventas | Ventas + Catálogo + Restaurantes | +2 contratos por congelar |
| Riesgos | 5 | + doble descuento, errores de redondeo, cambio de contrato de venta | +3 |

**Preguntas de v2:** dónde viven las recetas (se recomendó Restaurantes); FEFO automático o manual; costo promedio sin recálculo retroactivo; orden de recorte.

## Lo que salió bien
- Identificó el riesgo de calendario de inmediato y lo dijo con números.
- Dejó un **orden de recorte** explícito antes de empezar.
- Separó prioridades, de modo que el recorte no rompía el camino dorado.
- Marcó las fases que podían ir en paralelo (B y C desde que se congelan los contratos).
- Hizo preguntas con recomendación; el usuario respondió con decisiones claras.

## Lo que faltó (lecciones)
1. **No incluyó una fase de revisión del diseño.** Al dibujar los flujos después aparecieron siete huecos (D-24 a D-30) que cambiaron contratos. Una fase de «diagramas y revisión» habría dado tiempo y dueño.
2. **No fijó las horas efectivas** detrás del «día-persona». Con estudiantes de jornada parcial, el total real puede ser mayor.
3. **No hubo tabla de capacidad contra demanda** con la disponibilidad real del equipo; el veredicto fue cualitativo.
4. **Los contratos con Ventas y Catálogo** quedaron «por congelar» sin responsable ni fecha.
5. **No se dibujó el camino dorado** de la demo: no estaba claro cuánto de Inventario debía verse en pantalla.
6. **El plan v2 no mostró el delta** en una tabla contra v1; se explicó en prosa.

Con esas lecciones, un plan nuevo debe incluir: fase de diseño y revisión con dueño, capacidad en horas efectivas, camino dorado, dueños y fechas para cada contrato por congelar, y el delta explícito cuando se replanifique.
