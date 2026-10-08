---
name: modelar-base-de-datos
description: "Modela o revisa tablas, columnas, restricciones, índices, seguridad por fila y migraciones de Postgres del proyecto: esquema por módulo, llaves UUIDv7, tipos numéricos, movimientos inmutables, Flyway. Úsala cuando se cree o cambie una tabla, un diagrama entidad-relación, una migración o una consulta pesada, o cuando haya dudas de tipos, índices, llaves foráneas o aislamiento entre negocios."
---

# Modelar la base de datos

> Complementa `negocio360-reglas`. Si chocan, gana esa skill. En fase de diseño produce **tablas descritas en documentos**, no migraciones.

## Convenciones del proyecto

| Tema | Regla |
|---|---|
| Esquemas | Un esquema de Postgres por módulo (`inventario`, `restaurantes`...). Sin joins, llaves foráneas ni repositorios entre esquemas de módulos distintos |
| Referencias entre módulos | **Referencia lógica**: un UUID opaco sin llave foránea. Documenta de qué módulo es |
| Llaves primarias | UUIDv7 |
| Multi-negocio | `negocio_id uuid NOT NULL` en toda tabla de negocio, más seguridad por fila (RLS) con la variable de sesión `app.negocio_id`, que fija la infraestructura al abrir la transacción |
| Cantidades | `numeric(14,4)` |
| Dinero | `numeric(14,2)` (costos unitarios `numeric(14,4)`) |
| Factores de conversión | `numeric(18,8)` |
| Fechas y horas | `timestamptz` en UTC. Una fecha de calendario local (una caducidad) es `date` |
| Enumerados | `text` con `CHECK`, no tipos enumerados de Postgres (cambiarlos es doloroso) |
| Nombres | `snake_case` en español, igual que el diseño |
| Concurrencia | Columna `version bigint` para bloqueo optimista donde haya contención |
| Registros históricos | **Solo inserción**: sin UPDATE ni DELETE (revócalos y agrega un disparador de respaldo). Un error se corrige con un movimiento nuevo |
| Borrado | Lógico (`activo`), no físico, salvo purgas de tablas técnicas |
| Redondeo | Una sola utilidad en el código (HALF_UP). La base solo guarda |

## Restricciones que implementan reglas
Usa `UNIQUE`, `CHECK` y llaves foráneas para que la base **impida** lo que las reglas prohíben, no solo el código:
- Idempotencia → `UNIQUE (negocio_id, causa_id, consumidor)`.
- Unicidad dentro del negocio → `UNIQUE (negocio_id, ...)`.
- Rangos → `CHECK (merma_pct BETWEEN 0 AND 100)`.
- Dependencia entre columnas → `CHECK (rastreo <> 'SERIE' OR unidad_base = 'pieza')`.
- Vigencias que no se traslapan → restricción de exclusión sobre el rango de fechas.
- Refuerzo recomendado: llaves foráneas **compuestas con `negocio_id`** para que ni un error de código enlace datos de dos negocios.

## Índices
Un índice por consulta real, nombrada en el diseño. Para cada uno escribe para qué sirve (kardex, asignación FEFO, revisión diaria...). Sin índices "por si acaso". Los índices parciales (`WHERE cantidad_actual > 0`) son bienvenidos cuando la consulta solo mira filas activas.

## Retención
Toda tabla técnica (`evento_procesado`, tokens, marcas) dice cuánto vive y qué tarea la purga.

## Migraciones (cuando haya código autorizado)
- Flyway, una carpeta por módulo, versionadas.
- **Nunca editar una migración ya aplicada.** Se agrega una nueva.
- Sin datos reales en migraciones. Los datos iniciales son solo catálogos (por ejemplo, las unidades).
- Cada migración se prueba contra Postgres real.

## Cómo documentar una tabla

```markdown
**`nombre_tabla`** (solo inserción / con RLS / global)
| Columna | Tipo | Nota |
|---|---|---|
```

Y un diagrama entidad-relación en Mermaid (ver `diagramas-mermaid`), con las referencias lógicas en línea punteada.

## Lista de verificación por tabla
- [ ] ¿Quién es el módulo dueño?
- [ ] ¿Tiene `negocio_id` y RLS (o dice por qué es global)?
- [ ] ¿La llave primaria es UUIDv7?
- [ ] ¿Los tipos son los de la tabla de arriba? Ningún `float` ni `double`.
- [ ] ¿Qué restricciones hacen cumplir las reglas del diseño?
- [ ] ¿Cada consulta del diseño tiene su índice?
- [ ] ¿Es de solo inserción? ¿Cómo se corrige un error?
- [ ] ¿Cuánto vive y quién la purga?
- [ ] ¿Las referencias a otros módulos son lógicas?
