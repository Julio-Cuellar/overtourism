# Plantillas de plan

Copia la que corresponda al nivel, llena todo y borra lo que no aplique **solo** si en el nivel dice «opcional». Un campo vacío se llena con «Por definir» y una razón, no se omite.

---

## Plantilla estándar

```markdown
# Plan: <nombre de la funcionalidad> (v1)

**Fecha:** AAAA-MM-DD · **Nivel:** Estándar · **Estado:** borrador, esperando confirmación

## Qué entendí
<3 a 5 líneas: qué se construye, para quién, por qué ahora>

**Queda fuera:** <lista corta>
**Supongo:** <lista corta>
**No sé:** <lista corta>

## Decisiones que aplican
| ID | Decisión | Estado |
|---|---|---|

## Alcance
| Prioridad | Qué | Camino dorado |
|---|---|---|
| 1 | | Sí / No |
| 2 | | |
| 3 | | |

**Fuera del MVP:** <lista>
**Orden de recorte (primero lo que se quita primero):** 1. ... 2. ... 3. ...

## Fases
| Fase | Entregable | Criterio de salida | Estimado (rango) | Responsable | Depende de |
|---|---|---|---|---|---|
| F0 | | | | | |

## Riesgos
| Riesgo | Prob. | Impacto | Mitigación | Dueño |
|---|---|---|---|---|

## Calendario
Hoy es AAAA-MM-DD. Faltan <n> días para el 15 de octubre de 2026.
Demanda: <n> (con colchón). Capacidad: <n>. Veredicto: **cabe / cabe justo / no cabe**.

## Preguntas
1. <pregunta> — recomiendo: <opción>. Si A: <efecto>. Si B: <efecto>.

**Esperando confirmación** antes de continuar. Respóndeme con «aprobado», «aprobado salvo N» o «modifica: ...».
```

---

## Plantilla mayor

```markdown
# Plan: <nombre> (v1)

**Fecha:** AAAA-MM-DD · **Nivel:** Mayor · **Estado:** borrador, esperando confirmación
**Documentos base:** <rutas de diseño y Bitácora leídos>

## 1. Resumen ejecutivo
<5 líneas máximo: qué, cuánto, cuándo, mayor riesgo, qué se necesita decidir>

## 2. Qué entendí
<3 a 5 líneas>
**Queda fuera:** ... · **Supongo:** ... · **No sé:** ...

## 3. Decisiones que aplican
| ID | Decisión | Estado | Efecto en este plan |
|---|---|---|---|

## 4. Requisitos y criterios de aceptación
| ID | Requisito | Criterio comprobable | Regla del diseño | Criterio de la rúbrica |
|---|---|---|---|---|
| RQ-01 | | Dado ..., cuando ..., entonces ... | | |

**No funcionales:** aislamiento entre negocios · idempotencia · seguridad · datos simulados etiquetados · <otros>

## 5. Alcance
| Prioridad | Funcionalidad | Camino dorado |
|---|---|---|
**Camino dorado de la demo:** <el flujo de punta a punta en una frase>
**Fuera del MVP:** ...
**Orden de recorte:** 1. ... 2. ... 3. ...

## 6. Fases y tareas
| Fase | Entregable | Criterio de salida | Estimado (O / P / Pe → E) | Responsable | Depende de |
|---|---|---|---|---|---|
| F0 | | | | | |

### Tareas de la fase F0
| ID | Tarea (≤ medio día) | Hecho cuando | Responsable |
|---|---|---|---|
| F0.1 | | | |

## 7. Dependencias
- Orden: F0 → F1 → (F2 ∥ F3) → F4
- **Contratos a congelar primero:** <lista y con quién>
- **Decisiones abiertas que bloquean:** <ID, quién responde, para cuándo>
- **Terceros y entorno:** <lista>

## 8. Reparto del equipo
| Persona | Módulo o rebanada | Fases | Revisa a |
|---|---|---|---|
| A | | | |
| B | | | |
| C | | | |

**Puntos de integración:** <fecha y qué se junta>

## 9. Estimación
| Concepto | Días-persona |
|---|---|
| Suma de tareas (E) | |
| Pruebas | |
| Documentación | |
| Integración y corrección | |
| Preparación de la demo | |
| **Subtotal** | |
| Colchón (<n>%) | |
| **Total** | |

**Supuestos:** <horas efectivas por persona y día, disponibilidad, etc.>
**Ruta crítica:** <n> días calendario.

## 10. Calendario
| Hito | Fecha | Condición |
|---|---|---|
| Contratos congelados | | |
| Integración 1 | | |
| Congelamiento de funciones | | |
| Ensayo de la demo | | |
| Evento | 2026-10-15 al 17 | |

Capacidad: <n>. Demanda: <n>. Veredicto: **cabe / cabe justo / no cabe**.
Si no cabe: Opción A <recorte> · Opción B <más horas> · Opción C <mover hito>. Recomiendo: <...>

## 11. Riesgos
| Riesgo | Prob. | Impacto | Señal de alerta | Mitigación | Dueño |
|---|---|---|---|---|---|

## 12. Validación por fase
| Fase | Cómo se valida |
|---|---|

## 13. Preguntas
1. <pregunta> — recomiendo: <opción>. Si A: <efecto>. Si B: <efecto>.

**Esperando confirmación** antes de continuar. Respóndeme con «aprobado», «aprobado salvo N» o «modifica: ...».
```

---

## Cómo presentar el plan al usuario
- Entrega el plan completo en el formato de arriba, pero abre con el resumen: qué, cuánto, si cabe, qué decidir.
- No pegues el plan en la Bitácora completo. Registra un resumen y enlaza el documento si lo guardaste.
- Si el plan es largo, guárdalo en `docs/planes/<nombre>-v1.md` y avisa la ruta. Registra el archivo en «Documentos relacionados».
- Pide la confirmación en una sola frase al final.
