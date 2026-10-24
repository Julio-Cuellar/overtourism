# Evidencia TDD: Fase 0 (consumo de Inventario en Postgres) y HZ-22, HZ-23 y HZ-28

**Fecha:** 2026-10-24 · **Plan:** [`docs/planes/horizontales-para-restaurantes.md`](../planes/horizontales-para-restaurantes.md) (v2) · **Autorización:** Bitácora, «Autorización: Fase 0, Fase 1 y HZ-22, HZ-23 y HZ-28».
**Entorno:** JDK 21, Maven, **sin Docker**. Las pruebas que levantan PostgreSQL o un contexto completo de Spring no se pudieron ejecutar aquí.

## 1. Resultado de la suite completa

`mvn -B test` sobre el árbol de trabajo: **2,881 pruebas, 0 fallas, 744 errores, 2 omitidas**. Los 744 errores son 717 de «no se pudo cargar el contexto de Spring» y 27 de «no hay entorno Docker»; ninguno es una aserción. El tronco compila (main y test).

| Módulo | Pruebas | Errores |
|---|---|---|
| Restaurantes | 212 | 0 |
| Compartido | 266 | 0 |
| Caja | 98 | 0 |
| Inventario | 199 | 12 (contextos que piden Docker) |
| Ventas | 135 | 18 (contextos que piden Docker) |

## 2. Tareas, con su rojo y su verde

| Tarea | RED (por la razón correcta) | GREEN | Pruebas nuevas |
|---|---|---|---|
| **HZ-01** consumo de Inventario en Postgres | El árbol no compilaba: `Articulo` ganó `rastreo` e `InventarioServicio` ganó `ConsumosServicio`; el contrato nuevo exigía `InventarioJdbc` como `RepositorioConsumos` | `V072`, `InventarioJdbc` (rastreo y `ON CONFLICT DO NOTHING`), `InventarioPostgresConfiguracion` | 8 del contrato (corren en memoria: 25 en total) y 5 de `ConsumoInventarioPostgresTest` (compilan, **sin ejecutar**) |
| **HZ-28** kit de contrato | No compilaba: faltaba `modoInventario` / `ModoInventario` | `ModoInventario` y el método por omisión de `FuenteDeProductos` | 9 del kit; la fuente simulada los pasa (2 opcionales omitidas) |
| **HZ-23** Restaurantes es `FuenteDeProductos` | `PlatillosParaVenta` no era una `FuenteDeProductos` | Implementa la interfaz, declara el modo, se retira `ProductoDePlatillo` | 10 (9 del kit + 1 del modo) y una aserción de que Spring la ve como `FuenteDeProductos` |
| **HZ-22** `ComandosDeRestaurantes` | No compilaba (la clase no existía) y el esquema no tenía las variantes | Módulo de comandos, SDL, declaración en la tabla de operaciones y `@Bean` | 7 de `ComandosDeRestaurantesTest`, 1 de esquema y 1 de permisos de comandos |

## 3. Lo que las pruebas descubrieron (y se corrigió)

1. **D-126 no se cumplía en Restaurantes.** `ManejadorGuardarReceta` y `ManejadorDarDeBajaPlatillo` revisaban el permiso por dentro, con el token ya gastado, pero no declaraban `permisos()`. Tres pruebas de `ComandosDeRestaurantesTest` fallaron («Expecting code to raise a throwable») hasta que los dos manejadores declararon `RECETA_EDITAR` y `MENU_EDITAR`. Ahora, quitar el permiso entre preparar y confirmar responde `PERMISO_DENEGADO` y el token sigue vivo.
2. **La tabla de operaciones (SG-21) protegió el cambio:** agregar las variantes al esquema hizo fallar `EsquemaGraphqlRestaurantesTest.tablaDeOperaciones` («falta declarar la variante …») hasta declararlas.
3. **Pruebas que dependían de la firma vieja** (`PreparacionesEnMemoriaTest`, `InventarioEnMemoriaTest`, `InventarioConfiguracionTest`, `OperacionesDeRestaurantesTest`, `PlatillosParaVentaTest`) se ajustaron. `OperacionesDeRestaurantesTest` afirmaba «ninguna variante de comando todavía».
4. **`AislamientoVentasCajaInventarioPostgresTest` compara la lista de tablas con la base:** la nueva `inventario.consumo` se agregó a la lista y se siembra una fila por negocio.

## 4. Mutaciones (4 de 4 detectadas)

| Mutación | Prueba que la detecta |
|---|---|
| `guardarReceta` deja de declarar su permiso | `ComandosDeRestaurantesTest` (2 fallas) |
| El modo de inventario del platillo se ignora | `PlatillosParaVentaContratoTest` (1 falla) |
| El modo por omisión deja de ser `NINGUNO` | contratos de la fuente simulada y de Restaurantes (1 falla) |
| `buscar` ofrece platillos dados de baja | `PlatillosParaVentaContratoTest` (2 fallas) |

Cada archivo se restauró y una corrida posterior dejó 26 pruebas en verde.

## 5. Límites y lo que NO se verificó

- **PostgreSQL:** la migración `V072`, `InventarioJdbc` (rastreo y consumos) y `ConsumoInventarioPostgresTest` están escritos y compilan, pero **nunca se ejecutaron contra una base**. El SQL se revisó a mano. Hay que correr `mvn -B verify` con Docker (HZ-02).
- **K-02 (atomicidad)** descansa en `FronteraTransaccional` y en que `ON CONFLICT DO NOTHING` no aborta la transacción; la prueba que lo demuestra (`atomico`, `carrera`) es una de las que no corrió.
- **La aplicación completa nunca se arrancó** con Restaurantes, Inventario y Ventas juntos; por eso «Ventas ve los platillos» está probado a nivel de contrato y de bean, no de punta a punta.
- **`ComandosDeRestaurantes`** se probó sin Spring, con Inventario en memoria real; el cableado de `ComandosGraphQlController` y el esquema completo con `@oneOf` no se ejecutaron.
- Las vigencias de los tokens de comandos de Restaurantes viven en memoria (`PreparacionesEnMemoria`) hasta R7.
