---
name: disenar-interfaz
description: "Diseña pantallas y flujos de la app del turista y del panel del locatario (PWA móvil primero): estructura, estados, textos en español de México, accesibilidad, filtros e identidad visual, y los mockups correspondientes. Úsala cuando pidan una pantalla, un flujo de usuario, un wireframe, un mockup, textos de interfaz, mejoras de UX, el cuestionario de intereses, el pasaporte turístico o el tablero de gobierno."
---

# Diseñar la interfaz

> Complementa `negocio360-reglas`. Si chocan, gana esa skill.

## Para quién se diseña

| Persona | Contexto | Qué necesita |
|---|---|---|
| **Turista** (de fin de semana, de negocios) | En la calle, con el teléfono, poco tiempo | Encontrar qué hacer cerca, con filtros, rutas y cupones |
| **Local** | Conoce la ciudad, busca opciones y beneficios | Reseñas, horarios, disponibilidad, pasaporte |
| **Locatario** (fonda, artesano, museo) | Atiende un negocio, poca experiencia digital, a menudo con una mano ocupada | Registrar y consultar rápido, sin errores. Bot por mensaje más un panel sencillo |
| **Gobierno** (Secretaría de Desarrollo Turístico) | Escritorio | Tablero agregado y anónimo |

## Principios
1. **Móvil primero** (PWA). Todo funciona a 400 px de ancho sin desplazamiento horizontal.
2. **Muestra el estado real** de todo: cargando, vacío, con error, con éxito, sin conexión. Cada pantalla tiene los cinco, diseñados.
3. **Confirmar antes de guardar.** Toda escritura muestra un resumen y pide confirmación (preparar → confirmar). Un deshacer corto cuando aplique.
4. **Ninguna cifra se inventa en la interfaz.** Todo número viene de la API. Si no hay dato: «No tengo ese dato».
5. **Lenguaje llano** del usuario, no del sistema: «Entrada de mercancía», no «movimiento tipo ENTRADA». Sin tecnicismos ni siglas sin explicar (CPP → «costo promedio»).
6. **Español de México**, tono cercano y directo. Mensajes de error que dicen qué pasó y cómo seguir, sin disculpas ni vaguedad.
7. **Accesibilidad (WCAG 2.2 AA):** contraste suficiente, objetivos táctiles de al menos 44 px, foco visible, no depender solo del color, etiquetas en todos los campos, textos alternativos.
8. **Privacidad visible:** la ubicación solo se usa al consultar y no se guarda el recorrido; dilo en la pantalla donde se pide el permiso.
9. **Menos pasos.** Si algo se puede inferir (el negocio, el idioma), no lo preguntes.
10. **Un solo propósito por pantalla.**

## El turista: piezas del producto
- **Cuestionario posterior al registro:** tipo de visitante (local, turista de fin de semana, viaje de negocios), 3 a 5 intereses, estilo de viaje (familia, pareja, solo, amigos). Corto, con progreso visible y opción de omitir.
- **Filtros:** cultura y arte, gastronomía local, festividades, noche o día, familiar, presupuesto.
- **Categorías y subcategorías:** Restaurante (gluten free, verano, mixto, gastronomía típica, comida corrida, extranjera), Museos (historia, cultura, arte).
- **Ficha del negocio:** reseñas, menú, información general, horas pico, disponibilidad de mesas o atención.
- **Rutas con IA:** varias opciones por estancia, ajustables, con duración y costo calculados por el servidor.
- **Cómo llegar:** transporte público y RUTA. Los datos aproximados se rotulan como estimados.
- **Pasaporte con cupones:** sellos, QR de un solo uso con vencimiento corto.

## El locatario: piezas del producto
Registro por voz o texto (bot) y panel: ventas, inventario, alertas, mapa de mesas, cupones, estadísticas del pasaporte. La confirmación siempre es explícita y legible: «Entrada de 5 kg de carne a $180 el kilo. ¿Confirmas?».

## Qué entregar
Para cada flujo:
1. **Diagrama del flujo** (Mermaid, con `diagramas-mermaid`).
2. **Pantallas**, con sus cinco estados.
3. **Textos** exactos de interfaz y errores.
4. **Operaciones de la API** que usa cada pantalla (nombre de la consulta o mutación).
5. **Notas de accesibilidad.**

## Mockups
Si se pide un mockup visual, usa la skill `generar-mockup-html`, que trae la plantilla, el proceso y la verificación. Reglas mínimas:
- Un archivo HTML autocontenido, con tema claro y oscuro y funcionando a 400 px.
- **Datos inventados y rotulados** como ejemplo. Negocios ficticios (Fonda Doña Chela, Museo Talavera Viva, Sushi Volcán...). Nunca nombres de negocios ni de personas reales.
- Identidad usada hasta ahora en los mockups: azul talavera como acento y dorado cempasúchil como secundario, con Bricolage Grotesque, Figtree e IBM Plex Mono. Es la línea de los mockups, no una identidad final aprobada; si el usuario define otra, gana la suya.
- Evita lo genérico: nada de plantilla de «tarjetas redondeadas iguales» sin jerarquía.
- Dilo si no revisaste cómo se ve.

## Lista de verificación
- [ ] ¿Funciona a 400 px y en tema oscuro?
- [ ] ¿Están los cinco estados?
- [ ] ¿Cada número viene de la API?
- [ ] ¿Los textos están en español de México y en lenguaje llano?
- [ ] ¿Se puede usar con una sola mano y sin leer mucho?
- [ ] ¿Hay confirmación antes de guardar?
- [ ] ¿Cumple contraste y tamaño táctil?
