---
name: generar-mockup-html
description: "Genera mockups visuales del proyecto como una página HTML autocontenida: pantallas de la app del turista y del panel del locatario con sus estados, mapas de arquitectura, flujos, secuencias, eventos y modelo de datos, con tema claro y oscuro, interactividad ligera y datos inventados. Úsala cuando pidan un mockup, una maqueta, un prototipo visual, 'muéstrame cómo se vería', 'haz un modelo visual' de un diseño, una página para presentar al equipo, o cuando haya que ver un diseño en vez de leerlo."
---

# Generar un mockup en HTML

> Complementa `negocio360-reglas`. Si chocan, gana esa skill.

## Qué es y qué no es
- Un mockup es un **documento de diseño visual**: sirve para ver y discutir, no para ejecutar el producto.
- **Se permite en fase de diseño** aunque la Bitácora no autorice código, porque no es código del producto. Aun así: no crees proyecto Maven, ni `src`, ni uses frameworks, ni toques el backend.
- Es un solo archivo `.html` que se abre en el navegador. Sin servidor, sin instalar nada.
- No se conecta a ninguna API real. Todo lo que muestra es de ejemplo y está rotulado.

## Tipos de mockup

| Tipo | Contiene | Ejemplo en el proyecto |
|---|---|---|
| **Pantallas** | Teléfonos con el flujo de la app del turista o del locatario, cada pantalla con sus estados | Cuestionario, inicio, filtros, ficha del negocio |
| **Panel** | Vista de escritorio del panel del locatario o del tablero de gobierno | Ventas, mesas, inventario, alertas |
| **Arquitectura y flujos** | Mapa de módulos, protocolos, eventos y secuencias | Quién habla con quién, GraphQL, WebSocket, REST |
| **Datos** | Tablas, relaciones y catálogo de eventos explorables | Mapa de Inventario |
| **Plataforma** | Mezcla de los anteriores con menú fijo | El mockup de la plataforma |

Pregunta cuál quiere el usuario si no lo dijo. Si pide «todo», propón las secciones y confirma antes de construir.

## Proceso

1. **Aclara qué elementos quiere ver.** Escríbelos en una lista de una línea cada uno y confirma. Evita construir de más: un mockup consume horas, y quedan pocos días.
2. **Reúne el contenido real** de la Bitácora, el diseño del módulo y los contratos de API y eventos. **No inventes operaciones ni campos.** Si algo no está definido, ponlo como «por definir» en la página.
3. **Planifica las secciones**: menú, orden, y una idea por figura.
4. **Parte de la plantilla** `assets/esqueleto-mockup.html`: cópiala a `docs/mockups/<nombre>.html` y adáptala.
5. **Construye sección por sección.** Los datos largos (tablas, listas) van en arreglos de JavaScript y se pintan con una función que escapa el HTML.
6. **Verifica** (sección «Verificación»).
7. **Registra** el archivo en la Bitácora (`registrar-en-bitacora`).
8. **Reporta con honestidad** qué elementos incluye y qué no pudiste verificar.

Pon un **límite de tiempo** a cada mockup. Si el usuario no pidió pulido, entrega algo completo y claro antes que algo bonito a medias.

## Reglas de contenido

- **Todo dato es inventado y está rotulado.** Una etiqueta visible: «Datos de ejemplo inventados». Negocios ficticios (Fonda Doña Chela, Museo Talavera Viva, Sushi Volcán, Cocina Santa Clara, Dulces Doña Lupe). Nunca nombres de personas, teléfonos, correos o negocios reales.
- **Las cifras cuadran.** Calcula antes de escribir y repite el mismo número en todas las pantallas donde aparece (un subtotal menos el descuento da el total; la suma de las filas es el total). Un jurado atento revisa las cuentas.
- **Cada pantalla dice qué operación usa**: pie de figura con `API: nombre` y, si aplica, `WS: suscripción`, tomada del diseño. Si la operación aún no existe, escribe «propuesta».
- **Los cinco estados** en las pantallas clave: cargando, vacío, con error, con éxito y sin conexión.
- **Español de México**, lenguaje llano. Sin lorem ipsum.
- **Lo no confirmado se rotula.** Por ejemplo, los medios de pago de los autobuses «por confirmar» y los datos de transporte «ilustrativos / estimados».
- **Fuentes al pie:** qué documentos respaldan el mockup y que es un diseño en revisión.
- **Si el mockup contradice el diseño**, no lo hagas en silencio: o ajustas el mockup, o lo registras como hallazgo para cambiar el diseño.
- **Identidad visual:** la de `disenar-interfaz` (azul talavera, dorado cempasúchil, Bricolage Grotesque, Figtree, IBM Plex Mono) salvo que el usuario defina otra.

## Reglas técnicas

- **Un solo archivo**, CSS y JavaScript dentro, sin build ni frameworks. Lo único externo permitido: Google Fonts y, si hay diagramas Mermaid, Mermaid desde un CDN con **versión exacta** (verifica que exista). Para una demo sin internet, descarga esos archivos y guárdalos junto al mockup.
- **Tokens de color** en `:root` (claro), redefinidos para oscuro con `@media (prefers-color-scheme: dark)` protegido por `:root:not([data-theme="light"])`, y otra vez en `:root[data-theme="dark"]`. El `body` lleva fondo de un token. Ningún color suelto que solo se lea en un tema.
- **Responsivo:** funciona a 400 px, con márgenes laterales de al menos 16 px y sin desplazamiento horizontal de la página. Las tablas, el código y los diagramas anchos van dentro de un contenedor con `overflow-x: auto`. Los hijos de `grid` o `flex` con texto largo llevan `min-width: 0`.
- **Tipografía:** de dos a tres familias con respaldo; `font-variant-numeric: tabular-nums` en cifras.
- **Accesibilidad:** `lang="es-MX"`, foco visible, botones reales (`<button>`) con `aria-pressed` cuando alternan, contraste suficiente, `role="img"` y `aria-label` en los SVG, `prefers-reduced-motion` respetado.
- **Sin imágenes externas.** Los íconos son SVG en línea.
- **Sin `alert`, `confirm` ni `prompt`.** Sin red real. Sin formularios que envíen.
- **JavaScript mínimo y sin dependencias.**

## Estructura de la página

1. Encabezado: chips de estado («Borrador, solo diseño», «Datos de ejemplo inventados»), título y una línea que diga qué es.
2. Menú fijo con anclas a las secciones.
3. Secciones: una frase de introducción, la figura y un pie «Qué mirar».
4. Pie de página: documentos de origen y la nota de que es un diseño en revisión.

## Pantallas de teléfono

- Marco de unos 360 px de ancho con barra de estado de ejemplo.
- Un selector de estados (cargando, vacío, error, éxito, sin conexión) que cambia la pantalla.
- Debajo, el pie con la operación de API.
- Contenido real del diseño: ficha del negocio, filtros, cuestionario, pasaporte, etc.
- Para un flujo de varias pantallas, ponlas en fila con flechas, o con un «paso a paso» si el espacio es corto.
- La plantilla trae este componente ya resuelto.

## Diagramas

- **Mermaid** para secuencias, estados, entidad-relación y flujos. Sigue `diagramas-mermaid`.
  - En HTML van en `<pre class="mermaid">` y **sin** `<br/>`.
  - **Nunca dentro de un contenedor oculto** (pestañas, `display: none`, `<details>` cerrado): se dibuja con tamaño cero. Apílalos uno debajo de otro.
- **SVG a mano** para mapas de arquitectura:
  - `viewBox` fijo y clases que usen los tokens de color, no colores sueltos.
  - Marcadores para las puntas de flecha.
  - Etiquetas como «pastillas» centradas sobre la línea. Revisa que ninguna etiqueta pise otra línea ni otra etiqueta.
  - Convención del proyecto: **línea punteada azul = síncrono, línea continua dorada = asíncrono**, con leyenda dentro del dibujo.
- Todo diagrama lleva su pie «Qué mirar».

## Interactividad permitida
Filtros y búsqueda en tablas · selector de estados de una pantalla · paso a paso de un flujo con datos inventados (por ejemplo, de un mensaje a la pantalla del panel) · alternar tema. Nada que parezca un producto real sin rotularse como ejemplo.

## Verificación

No des por bueno un mockup que no viste. En orden:

1. Ábrelo en el navegador (el del IDE si lo hay) desde el archivo o con un servidor estático local.
2. Revísalo a **400 px y a 1280 px**, en tema **claro y oscuro**.
3. **Consola sin errores.** Ningún diagrama Mermaid muestra el mensaje de error de sintaxis.
4. Sin texto cortado y sin desplazamiento horizontal de la página.
5. Recorre con el teclado: foco visible y botones operables.
6. Vuelve a calcular las cifras.
7. Si puedes, toma una captura de lo esencial.
8. Si **no pudiste abrirlo**, dilo en el reporte y en la Bitácora. No digas «se ve bien».

## Entrega y registro

- Archivo en `docs/mockups/<nombre>.html`. Se ve abriéndolo en el navegador.
- Agrega la fila en «Documentos relacionados» y una entrada en la Bitácora: qué elementos muestra, qué documentos usa, qué no verificaste.
- **Compartirlo fuera del repositorio** (subirlo a un servicio, publicarlo o enviarlo) es una acción hacia afuera: pide confirmación al usuario antes.

## Errores comunes

- Pantallas sin estados de error o vacío.
- Cifras que no cuadran entre pantallas.
- Datos de ejemplo que parecen reales (nombres, teléfonos).
- Mermaid dentro de una pestaña oculta.
- Colores fijos que desaparecen en tema oscuro.
- Un mockup que inventa operaciones de API que no están en el diseño.
- Aspecto genérico: todo en tarjetas iguales con esquinas redondeadas, degradados morados, emojis como íconos.
- Gastar días en pulir un mockup cuando falta el camino dorado de la demo.

## Plantilla
`assets/esqueleto-mockup.html`: encabezado, menú, tokens claro y oscuro, componentes (chips, figuras, filtro, teléfono con estados, mapa SVG con convención de flechas, Mermaid) y scripts. Cópiala y reemplaza el contenido de ejemplo.
