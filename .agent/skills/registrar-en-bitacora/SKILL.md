---
name: registrar-en-bitacora
description: "Cierra una sesión de trabajo dejando el contexto listo para el siguiente agente o persona: registra decisiones, pendientes y archivos en docs/BITACORA.md, y retoma el trabajo desde ahí. Úsala al terminar cualquier tarea que cambie algo, cuando pidan un resumen del estado, un traspaso (handoff), actualizar la bitácora, 'en qué quedamos' o retomar el proyecto en una sesión nueva."
---

# Registrar y retomar con la Bitácora

> Complementa `negocio360-reglas`. Si chocan, gana esa skill.

La Bitácora (`docs/BITACORA.md`) es la memoria del proyecto. Un agente nuevo no recuerda nada: solo sabe lo que está escrito ahí. Lo que no se registra se pierde.

## Cerrar una sesión

1. **Relee** lo que hiciste. Lista archivos creados y modificados.
2. **Separa** lo que el usuario **dijo** de lo que tú **propusiste**. Lo primero es `ACEPTADA`; lo segundo, `PROPUESTA`; lo que no tiene respuesta, `ABIERTA`.
3. **Calcula el ID siguiente:** busca el mayor `D-xx` en la tabla de decisiones y suma uno. No reutilices ni saltes números.
4. **Agrega las decisiones** a la tabla de la sección 4 (ID, decisión, estado, fecha, notas).
5. **Agrega una fila** a «Documentos relacionados» por cada archivo nuevo.
6. **Escribe la entrada** al **final** del registro, con la plantilla de abajo. Nunca en medio, nunca reescribiendo las anteriores.
7. **Anota lo que no verificaste:** no se ejecutó, no se renderizó, no se probó, no se leyó.
8. **Revisa contradicciones** que tu cambio pudo crear: conteos (número de eventos), nombres, estados de decisiones, enlaces. Si encuentras una en un documento que no tocaste, regístrala como pendiente.
9. **Sincroniza la instantánea** de la sección 10 de la skill `negocio360-reglas` si una decisión cambió de estado o cambió la fase.

## Plantilla de entrada

```markdown
### AAAA-MM-DD — Título corto de lo que se hizo
**Pedido del usuario:** una línea, con sus palabras.

**Qué se vio:**
1. Lo relevante, en pocas líneas. Incluye hallazgos y por qué se decidió así.

**Decisiones:** D-xx (aceptada / propuesta).

**Observaciones para quien continúe:**
- Trampas, archivos dañados, cosas que no se tocaron y por qué.

**Pendiente:**
- Qué sigue abierto y quién debe responder.

**Archivos creados:** lista.
**Archivos modificados:** lista.
```

## Cambiar una decisión
No borres la anterior. Marca la vieja como `REEMPLAZADA por D-xx`, agrega la nueva con su motivo y anótalo en la entrada del día.

## Retomar el trabajo en una sesión nueva

1. Lee la Bitácora: secciones 1 a 6 y las **dos últimas** entradas.
2. Resume al usuario en **máximo diez líneas**:
   - Fase actual y qué está autorizado.
   - Qué se hizo en la última sesión.
   - Decisiones aceptadas que importan para hoy.
   - Pendientes abiertos, con quién debe responder.
3. Pregunta **cuál retomar**, con tu recomendación.
4. No empieces a construir hasta que el usuario elija, y recuerda la compuerta de código.

## Traspaso a otro agente o persona
Un buen traspaso cabe en una pantalla e incluye: objetivo, estado, lo hecho, lo que sigue, trampas conocidas y archivos clave. Si no cabe, el problema es la Bitácora, no el traspaso.

## Errores comunes
- Escribir «se decidió X» cuando el usuario no respondió.
- Dejar los pendientes sin dueño.
- Olvidar un archivo nuevo en «Documentos relacionados».
- Resumir el éxito y omitir lo que no se verificó.
- Pegar el contenido completo de los documentos en la Bitácora en vez de enlazarlos.
