---
name: revisar-codigo
description: "Revisa código Java 21 / Spring Boot del proyecto cuando ya hay código autorizado: límites entre módulos, transacciones, idempotencia, dinero, concurrencia, seguridad, manejo de errores, pruebas y estilo. Úsala cuando pidan revisar un cambio, un diff, un pull request, un commit, o antes de integrar código a la rama principal."
---

# Revisar código

> Complementa `negocio360-reglas`. Si chocan, gana esa skill.

## Condición previa
Solo aplica si **la Bitácora autoriza código** y hay cambios que revisar. Si el repositorio aún no tiene código, dilo y propón usar `revisar-diseno`.

## Procedimiento

1. Identifica **qué cambió**: lista los archivos y el propósito del cambio. Si hay git, revisa el diff; si no, lee los archivos modificados.
2. Lee las reglas del diseño que el cambio implementa (`U-01`, `C-02`, `K-01`...).
3. Si el entorno lo permite, ejecuta las pruebas y la verificación de módulos. **Dilo** si no las ejecutaste.
4. Recorre la lista de abajo.
5. Entrega los hallazgos con el formato final. No reescribas el código del autor salvo que te lo pidan.

## Lista de revisión

**Límites de módulo**
- [ ] No importa clases internas de otro módulo ni usa su repositorio.
- [ ] Ningún horizontal depende de un vertical.
- [ ] Solo se conversa por API pública o evento.
- [ ] La prueba de módulos (`ApplicationModules.verify()`) pasa.

**Transacciones y eventos**
- [ ] La escritura y la publicación del evento van en la misma transacción.
- [ ] Los listeners son idempotentes y no confían en el `negocio_id` de la carga.
- [ ] Se distinguen los errores reintentables de los permanentes.
- [ ] No hay llamadas externas lentas dentro de una transacción abierta.

**Dinero y cantidades**
- [ ] `BigDecimal` con escala explícita. Ningún `double` ni `float`.
- [ ] Un solo punto de redondeo, con el modo del diseño.
- [ ] Las conversiones de unidad pasan por la función única.

**Concurrencia**
- [ ] Bloqueo optimista con reintento acotado donde hay contención.
- [ ] No hay «leer, calcular, escribir» sin protección.

**Persistencia**
- [ ] Consultas parametrizadas. Ninguna concatenación de texto.
- [ ] Sin N+1; las listas paginan con límite.
- [ ] El contexto de negocio (RLS) se fija también en listeners y tareas programadas, que no tienen petición HTTP.
- [ ] Los registros históricos no se modifican ni se borran.

**API**
- [ ] El `negocio_id` sale del contexto, no de un argumento.
- [ ] Entradas validadas en la frontera.
- [ ] Códigos de error del catálogo; mensajes que no filtran datos internos.
- [ ] Respuesta con el contrato `{ ok, codigo, datos, mensaje_plantilla }`.

**Seguridad** (profundiza con `revisar-seguridad` si toca autenticación, entradas o el LLM)
- [ ] Sin secretos en el código.
- [ ] Sin datos personales en eventos ni en registros.

**Estilo**
- [ ] Objetos inmutables (`record`) donde se pueda.
- [ ] Funciones de menos de unas 50 líneas; archivos de menos de 800.
- [ ] Retornos tempranos en vez de anidación profunda.
- [ ] Nombres del diseño, en español, sin sinónimos nuevos.
- [ ] Sin código muerto, sin `System.out`, sin comentarios que repiten el código.

**Pruebas**
- [ ] Cada regla que implementa tiene una prueba que la nombra.
- [ ] Hay pruebas de evento duplicado, fuera de orden, concurrencia y aislamiento entre negocios cuando aplican.
- [ ] Las pruebas fallaban antes del cambio (TDD) o se explica por qué no.
- [ ] Cobertura mínima de 80% en lo que se tocó.

## Severidad y veredicto

| Nivel | Acción |
|---|---|
| CRÍTICO (seguridad, fuga entre negocios, pérdida de datos) | Bloquea |
| ALTO (bug probable, regla incumplida, sin pruebas de una regla) | Corregir antes de integrar |
| MEDIO (mantenimiento, ambigüedad) | Corregir pronto |
| BAJO (estilo) | Opcional |

## Formato de salida

```markdown
# Revisión de <cambio> (<fecha>)
**Veredicto:** Aprobar / Con reservas / Bloquear
**Ejecuté:** <pruebas, verify, linters> / **No ejecuté:** <lo que no>

| Sev. | Archivo:línea | Hallazgo | Escenario | Sugerencia |
|---|---|---|---|---|
```

Cada hallazgo lleva un escenario con entrada concreta y resultado incorrecto. Si no encuentras problemas, dilo y menciona qué revisaste.
