---
name: revisar-diseno
description: "Revisa un documento de diseño, plan, modelo de datos o contrato de eventos o API del proyecto, y encuentra huecos, contradicciones y violaciones de las reglas R1 a R6 antes de aprobarlo. Úsala cuando pidan revisar, auditar, validar, criticar o dar una segunda opinión sobre un diseño, o antes de congelar un contrato."
---

# Revisar un diseño

> Complementa `negocio360-reglas`. Si chocan, gana esa skill. Revisas; **no edites** el documento a menos que te lo pidan.

## Procedimiento

1. Lee la Bitácora, el documento a revisar y los que lo complementan.
2. Recorre las cinco pasadas de abajo, en orden.
3. Para cada hallazgo, escribe un **escenario concreto** que lo demuestre. Un hallazgo sin escenario es una opinión.
4. Clasifica por severidad y da un veredicto.
5. Registra las propuestas como `PROPUESTA` en la Bitácora (con la skill `registrar-en-bitacora`).

## Pasada 1: reglas de arquitectura
- R1: ¿algún módulo lee tablas de otro, o hay llaves foráneas entre esquemas?
- R2: ¿hay algún evento que en realidad espera respuesta? ¿Alguna llamada síncrona que debería ser evento?
- R3: ¿todo evento tiene `id` y todo consumidor es idempotente?
- R4: ¿algún horizontal depende de un vertical, aunque sea de forma indirecta?
- R5: ¿se cambió o quitó un campo de un evento publicado?
- R6: ¿entre horizontales se respeta lo mismo que R2?

## Pasada 2: consistencia entre documentos
- Los mismos conteos y nombres en todos lados (número de eventos publicados, nombres de columnas, códigos de error).
- Cada operación citada existe en el contrato.
- Cada regla tiene prueba de aceptación.
- Lo que dice el mockup coincide con el diseño (o se declara desactualizado).

## Pasada 3: «¿qué pasa si...?» para cada flujo
- ¿Llega el evento **duplicado**?
- ¿Llega **fuera de orden** (la anulación antes que la venta)?
- ¿Falla **a la mitad**? ¿Qué queda escrito?
- ¿Dos hilos actúan **a la vez** sobre lo mismo?
- ¿El proceso **se reinicia** entre dos pasos?
- ¿El dato pertenece a **otro negocio**?
- ¿El error es **permanente** o **reintentable**, y dónde queda registrado?
- ¿Se puede **repetir** la operación sin duplicar?
- ¿Qué ve el usuario cuando algo falla?

## Pasada 4: propiedad y límites
- ¿Cada dato tiene un solo dueño?
- ¿Hay dos módulos que podrían actuar sobre lo mismo a la vez sin un árbitro neutral?
- ¿Algo que parece horizontal solo sirve a un giro?

## Pasada 5: viabilidad y verdad
- ¿El estimado cabe en los días que quedan? ¿Qué se recorta primero?
- ¿Hay datos presentados como hechos sin fuente (cifras, requisitos fiscales, transporte)?
- ¿Alguna decisión aparece como aceptada sin que el usuario la haya aceptado?
- ¿Se apoya en algo que no existe todavía (otro módulo, una herramienta)?

## Severidad

| Nivel | Significa | Acción |
|---|---|---|
| CRÍTICO | Pérdida de datos, fuga entre negocios o incumplimiento de seguridad | Bloquea la aprobación |
| ALTO | Descuadre, duplicado o contradicción que se manifestará | Corregir antes de aprobar |
| MEDIO | Hueco o ambigüedad que obliga a adivinar | Corregir pronto |
| BAJO | Estilo, claridad o mejora opcional | A criterio |

## Formato de salida

```markdown
# Revisión de <documento> (<fecha>)

**Veredicto:** Aprobar / Aprobar con reservas / Bloquear
**Resumen:** dos líneas.

| # | Sev. | Dónde | Hallazgo | Escenario | Propuesta |
|---|---|---|---|---|---|

## Lo que está bien
Breve. Sirve para saber qué no tocar.

## Qué no pude verificar
```

Si no encuentras nada, dilo con claridad y di qué pasadas hiciste. No inventes hallazgos para parecer útil.
