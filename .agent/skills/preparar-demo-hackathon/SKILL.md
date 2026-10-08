---
name: preparar-demo-hackathon
description: "Prepara la entrega del hackathon Puebla Turística 360° (15 al 17 de octubre de 2026): guion de demo, estructura del pitch, mapeo a la rúbrica, datos de ejemplo, plan B, preguntas del jurado y recorte de alcance por falta de tiempo. Úsala cuando se hable de la demo, el pitch, la presentación, la rúbrica, el premio, qué mostrar, qué recortar, cuánto tiempo queda o si el prototipo está listo para presentar."
---

# Preparar la demo y el pitch

> Complementa `negocio360-reglas`. Si chocan, gana esa skill.

## Hechos del evento (de `CLAUDE.md`)
- **Fechas:** 15 al 17 de octubre de 2026, de 11:00 a 17:00, en el Centro Expositor.
- **Equipo:** 3 estudiantes y un profesor tutor. La solución y el desarrollo los hacen principalmente los estudiantes.
- **Etapa final:** versión funcional o demostrable, pitch ejecutivo, demo y ronda de preguntas. Se califica el resultado final, no el proceso.
- **Premios objetivo:** Gran Premio y Mejor solución basada en datos. Narrativa: turismo sostenible.
- Un proyecto puede ganar el Gran Premio y una categoría especial, nunca dos categorías especiales.

## Rúbrica y qué mostrar para cada criterio

| Criterio | Peso | Qué debe verse en la entrega |
|---|---|---|
| Datos: identificación, pertinencia, calidad, aprovechamiento | 20% | Fichas de fuentes, justificación, limpieza, trazabilidad (`documentar-fuentes-de-datos`) |
| Relevancia y comprensión del problema | 15% | Historia real, cifras con fuente, a quién ayuda |
| Innovación y creatividad | 15% | Lo que no hace «otro sistema de ventas»: rutas con IA, pasaporte con cupones, desconcentrar flujos, historial para formalizar |
| Solidez técnica y funcionamiento del prototipo | 15% | Un flujo que **funciona de verdad**, pruebas, arquitectura clara |
| Impacto turístico, económico y social | 15% | Derrama más pareja, formalización, ejemplos del piloto |
| Viabilidad, implementación y escalabilidad | 10% | Modelo sostenible, siguientes municipios (Cholula, Atlixco, Cuetzalan, Zacatlán, Chignahuapan) |
| Experiencia de usuario y utilidad | 5% | Interfaz clara, probada con usuarios reales |
| Calidad del pitch y demostración | 5% | Ensayo, tiempos, plan B |

Entregables escritos: descripción ejecutiva, problema y usuarios, solución, inventario y justificación de datos, procesos de datos, arquitectura, prototipo, evidencia de resultados, presentación y demo, y **documentación del modelo de IA** (con `integrar-ia`).

## Estructura del pitch (propuesta del `CLAUDE.md`)
1. Historia real de una persona prestadora de servicios que no tiene cómo cobrar ni saber si gana.
2. El problema, con cifras de INEGI (verificar las más recientes).
3. Demo en vivo.
4. Datos y metodología: fuentes, limpieza, calidad.
5. Resultados del piloto.
6. Escalamiento y modelo sostenible.

## Diseñar la demo
1. **Elige un camino dorado**: un solo flujo de punta a punta, que funcione de verdad y se cuente en 3 a 5 minutos. Es preferible un flujo completo a diez a medias.
2. **Candidatos** (propuestas; dependen de las decisiones abiertas de producto, que debe tomar el equipo):
   - Locatario: una venta entra → se descuentan los insumos por receta → se actualiza el costo y salta una alerta de reposición → se ve en el panel.
   - Turista: cuestionario → filtros → ruta generada con IA → cupón del pasaporte con QR → el locatario lo canjea.
3. **Datos de ejemplo** listos y **rotulados como simulados**. Si hay negocios del piloto, con su consentimiento.
4. **Guion** minuto a minuto, con quién habla y quién maneja la pantalla.
5. **Plan B** para cada punto de falla: sin internet, base de datos caída, modelo de IA lento o sin cuota. Capturas o un video corto grabado de antemano. Prueba el plan B.
6. **Estado limpio:** una forma de reiniciar los datos de la demo en un minuto.

## Recorte de alcance
Cuando falte tiempo se recorta **desde abajo**, nunca a la mitad de algo, y se presenta lo recortado como hoja de ruta, no como si existiera.
1. Pantallas y datos extra de lo que no está en el camino dorado.
2. Verticales sin implementar (se muestran como mockup).
3. Funciones de prioridad 3 y luego 2 de cada módulo (por ejemplo, números de serie y lotes en Inventario).
4. Pulido visual.
Lo último que se recorta: el camino dorado, los datos y la documentación del modelo de IA.

## Preguntas que probablemente harán
- ¿Cuál es el valor real de la IA aquí y qué haría un sistema sin IA? ¿Qué pasa si el modelo se equivoca?
- ¿De dónde salen los datos y cómo sabes que son confiables?
- ¿Cómo evitas que el modelo invente información?
- ¿Cómo protegen la privacidad de los negocios y de los turistas?
- ¿Qué los diferencia de las herramientas de ventas que ya existen?
- ¿Cómo escala y cómo se sostiene económicamente?
- ¿Qué está funcionando hoy y qué es todavía mockup? (Responder con honestidad.)

## Reglas
- **No prometas lo que no funciona.** Si algo es mockup, di «mockup».
- Todo dato simulado, rotulado.
- Ninguna cifra sin fuente.
- Respeta la honestidad sobre limitaciones; el jurado lo valora más que una demo que se rompe.
- Nada de claves ni datos reales en pantalla.

## Lista 48 horas antes
- [ ] Camino dorado funcionando en el equipo con el que se presentará.
- [ ] Docker o la base de datos arriba, y datos cargados.
- [ ] Claves de IA en variables de entorno, con cuota suficiente, y modelo alterno si falla.
- [ ] Plan B probado.
- [ ] Guion ensayado al menos dos veces con cronómetro.
- [ ] Documentos entregables completos y sin contradicciones (conteos, nombres, fechas).
- [ ] Respuestas a las preguntas de arriba repartidas entre las tres personas.
- [ ] Carga y batería, adaptadores y respaldo de todo en un USB.
