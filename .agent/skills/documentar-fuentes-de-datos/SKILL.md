---
name: documentar-fuentes-de-datos
description: "Identifica, documenta, limpia e integra las fuentes de datos del proyecto (DENUE de INEGI, DataTur de SECTUR, ENOE, OpenStreetMap, calendario de eventos, ocupación hotelera, datos del piloto) con procedencia, licencia, calidad y trazabilidad, y etiqueta los datos simulados. Úsala cuando se busque o use un dataset, se limpie o integre información, se simulen datos, se cite una cifra, o se prepare el entregable de datos del hackathon."
---

# Documentar fuentes de datos

> Complementa `negocio360-reglas`. Si chocan, gana esa skill.

## Por qué importa
La convocatoria **no da un dataset**. El equipo debe localizar, justificar, documentar y limpiar sus datos, y eso pesa **20% de la rúbrica**, el criterio más alto. Todo dato debe poder explicarse: de dónde viene, por qué se eligió y cómo se transformó.

## Reglas
1. **Nunca inventes una cifra real.** Estadísticas de INEGI, ocupación hotelera, tarifas, horarios del transporte: o las citas con fuente y fecha, o dices que no las tienes. Verifica las más recientes antes de citarlas.
2. **Todo dato simulado va etiquetado como simulado**, en la base, en los documentos y en la pantalla.
3. **No cargues de memoria** valores fiscales ni de transporte. Se verifican contra la fuente oficial.
4. **Respeta la licencia.** Antes de usar una fuente, anota bajo qué términos se puede usar y si hay que citar.
5. **Privacidad primero.** Los datos de negocios que participan en el piloto se tratan con consentimiento. El tablero de gobierno es agregado y anónimo, con un mínimo de negocios por celda.
6. **Trazabilidad.** Toda transformación se puede repetir con un script y una versión de los datos de entrada.
7. **Transporte:** no hay datos GTFS públicos conocidos. Lo que se construya con OpenStreetMap y captura propia se rotula como **estimado**, y los medios de pago de los autobuses no se afirman si no están confirmados.

## Inventario de fuentes (una ficha por fuente)

```markdown
### <Nombre de la fuente>
- **Qué contiene:** campos relevantes y cobertura geográfica y temporal.
- **Procedencia:** institución, enlace, fecha de descarga o consulta.
- **Licencia y condiciones de uso:**
- **Actualidad:** fecha de los datos y cada cuánto se actualiza.
- **Calidad:** completitud, duplicados, errores conocidos, sesgos. Medidos, no supuestos.
- **Para qué la usamos:** qué parte del producto depende de ella.
- **Por qué esta y no otra:** justificación de la selección.
- **Limpieza:** pasos, en orden, con el criterio de cada uno.
- **Integración:** con qué otras fuentes se cruza y con qué llave.
- **Trazabilidad:** dónde están el script y la versión de entrada y salida.
- **Riesgos y limitaciones:**
```

## Fuentes candidatas (del `CLAUDE.md`)
DENUE (INEGI), DataTur (SECTUR), ENOE (informalidad), OpenStreetMap, calendario de eventos y temporadas, ocupación hotelera y datos propios del piloto. Cualquier fuente nueva se agrega con su ficha.

## Proceso de limpieza
1. Guarda una copia **intacta** de lo descargado.
2. Perfila los datos: cuántas filas, nulos, duplicados, rangos extraños.
3. Define las reglas de limpieza **antes** de aplicarlas y anótalas.
4. Aplica en un script que se pueda volver a ejecutar. No edites a mano.
5. Mide el antes y el después (cuántas filas se descartaron y por qué).
6. Para cruzar fuentes (DENUE con OSM con redes sociales), registra el criterio de coincidencia y la confianza.
7. Documenta lo que quedó mal. Una limitación explicada suma; una oculta resta.

## Datos simulados
- Etiqueta visible: «Dato simulado».
- Genéralos con un script y una semilla, para poder repetirlos.
- Que se parezcan a la realidad solo en la forma, no en nombres reales de personas o negocios.
- Dilo siempre que se presenten resultados que dependan de ellos.

## Entregables del hackathon que esto alimenta
Inventario y documentación de fuentes · justificación de la selección · procesos de obtención, limpieza, integración, transformación y análisis · evidencia de análisis y resultados.

## Antes de entregar
- ¿Cada cifra citada tiene fuente y fecha?
- ¿Cada fuente tiene su ficha completa?
- ¿Lo simulado está etiquetado en todos lados?
- ¿El proceso se puede repetir desde cero?
