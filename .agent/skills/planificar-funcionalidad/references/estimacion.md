# Cómo estimar

Estas son **heurísticas, no hechos**. Su valor está en que obligan a mostrar los supuestos y a calibrar con datos propios. Cuando el equipo tenga números reales de sus fases, esos números mandan.

## 1. La unidad
- **Día-persona**: una persona trabajando una jornada efectiva.
- Una jornada efectiva no son las horas de reloj: descuenta clases, juntas, esperas y cambios de contexto.
- **Pregunta al equipo** cuántas horas efectivas al día tiene cada quien. Como supuesto inicial, para estudiantes con clases, usa de 4 a 5 horas, **márcalo como supuesto** y corrígelo con lo que respondan.
- Si el equipo trabaja con jornadas parciales, estima y compara en **horas**, no en días. Los planes anteriores del proyecto usaron «día-persona» sin fijar las horas; con disponibilidad parcial pueden ser optimistas.

## 2. Descompón antes de estimar
- Divide hasta tareas de **medio día o menos**. Estimar «el módulo» no sirve; estimar «la conversión de unidades con sus pruebas» sí.
- Estima **tareas**, suma, y luego agrega lo que no está en las tareas (sección 4).

## 3. Rango por tarea (tres puntos)
Para cada tarea con incertidumbre:
- **O** optimista (todo sale bien), **P** probable, **Pe** pesimista (sale lo que suele salir mal).
- **E = (O + 4·P + Pe) / 6**.
- Guarda el rango O–Pe. Un rango ancho es una señal: la tarea no se entiende bien. Divídela o investiga antes.
- Para tareas que ya se han hecho antes con el equipo, un número basta.

## 4. Costos que casi siempre se olvidan
Agrégalos como tareas o como líneas aparte, no los des por incluidos:

| Concepto | Qué incluye |
|---|---|
| Preparar el entorno | Instalar, configurar, base de datos, Docker, claves |
| Pruebas | Escribirlas primero, casos raros, datos de prueba |
| Documentación | Contratos, bitácora, diagramas, fichas de IA y de datos |
| Integración | Juntar el trabajo de varias personas y resolver choques |
| Corrección de errores | Lo que aparece al integrar y al probar con datos reales |
| Revisión | Revisión de diseño, de código y de seguridad |
| Demo | Datos de ejemplo, guion, plan B, ensayo |
| Esperas | Respuestas pendientes de decisiones o de terceros |

Si el equipo no tiene datos propios, anota qué porción dedicaste a cada concepto y por qué, para poder corregirla.

## 5. Paralelismo: tres personas no son tres veces más rápido
- El trabajo en paralelo solo existe donde **no hay dependencia**. Congelar contratos temprano es lo que la crea.
- Cada persona extra agrega coordinación (integración, revisión, dudas).
- La **ruta crítica** es el camino más largo de tareas dependientes. El plan no puede terminar antes que ella, sin importar cuánta gente haya.
- Para calcularla: dibuja las dependencias, suma la duración de cada camino considerando que cada persona hace una cosa a la vez, y toma el más largo.

## 6. Colchón
- Agrega un colchón al total, como punto de partida de **20% a 30%**. Es una heurística; ajústala con datos del equipo.
- En un evento con fecha fija, el colchón protege la **integración y la demo**. No lo gastes en agregar alcance.
- Muestra el colchón como línea aparte para que se vea y no se "absorba" en silencio.

## 7. Capacidad contra demanda
- **Capacidad** = suma, por persona, de (días disponibles × horas efectivas por día).
- **Demanda** = estimado total con colchón, en las mismas unidades.
- Veredicto:
  - **Cabe:** la demanda es claramente menor que la capacidad.
  - **Cabe justo:** muy cerca. Di que no hay margen para imprevistos y qué se recorta primero.
  - **No cabe:** la demanda supera la capacidad. Ofrece opciones: recortar (por el orden de recorte), más horas o más gente, o mover un hito. Nunca comprimas estimaciones para que "salga".
- Cuenta los días reales: descuenta los días sin trabajo y el tiempo del evento mismo. Si hay duda, pregunta el calendario del equipo.

## 8. Ejemplo numérico (inventado, solo para ilustrar el cálculo)

Tres personas, seis días hasta el congelamiento de funciones, cinco horas efectivas por día, un cuarto de día perdido por persona en juntas:
- Capacidad = 3 personas × (6 días × 5 h − 1.25 h) ≈ 3 × 28.75 ≈ **86 h**.
- Tareas sumadas (E) = 48 h. Pruebas 12 h, documentación 6 h, integración y corrección 10 h, demo 8 h. Subtotal = 84 h.
- Colchón 25% = 21 h. Demanda total = **105 h**.
- 105 h > 86 h → **no cabe**. Se ofrecen opciones: quitar lo de prioridad 3 (≈ 18 h) deja 87 h, **cabe justo**; o sumar horas.

Las cifras son ilustrativas. No las uses como referencia de productividad.

## 9. Calibrar
- Al cerrar cada fase, anota en la Bitácora: estimado, real y la razón de la diferencia.
- Calcula el factor (real ÷ estimado) y úsalo para estimar la siguiente fase.
- Una diferencia grande y repetida suele ser un costo fijo olvidado (sección 4) o una tarea mal partida.

## 10. Qué no hacer
- Dar un solo número sin rango ni supuestos.
- Estimar por capas sin incluir la integración.
- Contar el colchón como capacidad.
- Estimar siempre para el mejor caso porque el calendario aprieta: eso mueve el problema a la última semana.
- Aceptar una estimación ajena sin descomponerla.
