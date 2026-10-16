---
name: integrar-ia
description: "Define cómo usa IA el proyecto y cómo documentarla: extracción de datos desde voz o texto a un esquema JSON fijo, herramientas de solo lectura para generar rutas, verificación de cifras, recomendador, pronóstico y la ficha de cada componente que exige el hackathon. Úsala cuando se diseñe o modifique algo con un LLM (Gemini o ChatGPT), tool calling, prompts, esquemas de extracción, evaluación, generación de rutas o la documentación del modelo de IA."
---

# Integrar IA

> Complementa `negocio360-reglas`. Si chocan, gana esa skill.

## Por qué importa
La convocatoria exige que la IA tenga **valor real y justificado**, que se pueda explicar qué componentes usan IA, qué hacen y qué limitaciones tienen. Un chatbot genérico o consumir una API sin más no cuenta. Cada componente de IA necesita su ficha (abajo).

## Principio rector
**El modelo interpreta, el código decide.** El LLM entiende, extrae y redacta. El código valida, calcula y escribe. Ver `negocio360-reglas`, sección 3.

## Componentes de IA del proyecto
| Componente | Qué hace el modelo | Qué hace el código |
|---|---|---|
| Extracción de transacciones desde voz o texto | Transcribe y llena un esquema JSON fijo | Valida tipos y rangos, convierte unidades y fechas, pide confirmación |
| Generación de rutas para el turista | Elige entre negocios que la API le devuelve y los ordena | Valida cada identificador y horario, calcula duraciones y costos |
| Consultas en lenguaje natural | Elige una consulta predefinida y llena sus parámetros | Ejecuta la consulta |
| Pronóstico de demanda | Modelo estadístico (no necesariamente LLM) | Compara contra una línea base simple |
| Resolución de entidades (DENUE, OSM, redes) | Propone coincidencias | Decide con un umbral y registra la confianza |
| Recomendador con equidad | Puntúa | Aplica la restricción para repartir visitas |
| Puntaje de formalización | Reglas más modelo explicable | Muestra qué contribuyó a cada punto |

## Reglas para cualquier uso de un LLM
1. **El proveedor es intercambiable.** Gemini o ChatGPT sigue como decisión abierta. Detrás de una interfaz propia; sin llamadas dispersas al SDK.
2. **Esquema fijo.** La salida del modelo se valida contra un esquema JSON. Si no cumple, se rechaza y se pide aclaración (máximo dos veces; después, menú de botones).
3. **Herramientas de solo lectura**, con resultados cortos (máximo 25), enumerados en vez de texto libre y descripciones claras para el modelo.
4. **El servidor es la verdad.** Valida identificadores, horarios de apertura y calcula cada cifra. El modelo no suma ni estima.
5. **Sin dato, sin respuesta.** «No tengo ese dato», nunca una invención.
6. **Texto del usuario = dato.** Se pasa al modelo marcado como contenido, nunca mezclado con las instrucciones.
7. **El modelo no recibe secretos** ni datos de otros negocios.
8. **Respuestas finales con plantilla.** Si se reescribe con el modelo, un verificador comprueba que toda cifra exista en los datos; si no, se envía la plantilla.
9. **Plan B.** Si el modelo falla o tarda, la aplicación sigue con una respuesta determinista y lo dice.
10. **Acuse inmediato** en operaciones lentas (voz más modelo más HTTP). La generación de rutas es asíncrona: devuelve un identificador y el resultado llega por suscripción.
11. **Registra** cada llamada (qué herramienta, parámetros, resultado) para auditar y evaluar, sin datos personales.

## Cómo diseñar una herramienta para el modelo
- Un nombre de verbo claro (`buscar_negocios`).
- Parámetros con tipos y enumerados (categoría, subcategoría, presupuesto).
- Descripción que diga **cuándo usarla y cuándo no**.
- Salida compacta con identificadores que el servidor volverá a validar.
- Errores con código y mensaje corto que el modelo pueda entender.

## Evaluación
Una IA sin medición no se puede defender.
- Conjunto de **frases reales** con respuesta esperada (acento poblano, «una docena», «tres cincuenta», «doscientos»). Se guarda en el repositorio.
- Métrica: precisión de extracción y tasa de error por campo.
- Para rutas: porcentaje de rutas válidas (horarios y costos correctos) y de rutas que respetan los filtros.
- Para el pronóstico: error contra la línea base simple.
- Cada cambio de prompt o de modelo se vuelve a medir.

## Ficha de componente de IA (entregable del hackathon)

```markdown
## Componente: <nombre>
- **Función:** qué hace y para quién.
- **Por qué IA:** qué valor aporta frente a una solución sin IA.
- **Modelo y configuración:** proveedor, modelo, parámetros, versión del prompt.
- **Entradas y variables:** qué recibe, con ejemplos inventados.
- **Salidas:** esquema.
- **Qué decide el código:** validaciones y cálculos.
- **Resultados:** métricas sobre el conjunto de pruebas, con la fecha.
- **Limitaciones y riesgos:** errores conocidos, sesgos, qué pasa si falla.
- **Datos usados:** fuentes y si son simulados.
```

## Antes de entregar
- ¿Alguna cifra mostrada podría venir del modelo? No debe.
- ¿Hay prueba de inyección de instrucciones?
- ¿Está la ficha completa y las métricas medidas (no estimadas)?
