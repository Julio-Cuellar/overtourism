# Restaurantes P1a (R1 a R3) — evidencia TDD

**Fecha:** 2026-10-16 · **Plan:** [`docs/diseno/restaurantes.md`](../diseno/restaurantes.md) §16, fases R1, R2 y R3 · **Autorización:** Bitácora, «Autorización: programar Restaurantes P1a, empezando por R0» · **R0:** [`restaurantes-r0.tdd.md`](restaurantes-r0.tdd.md) · **JDK:** 21, Maven sin conexión (`mvn -o`).

## Historias de usuario
- **R1.** Como dueño, quiero armar mi menú (platillos, categorías, etiquetas, temporada, agotado, publicar y dar de baja) y que el turista vea solo lo publicado y vigente; y como Ventas, quiero precios vigentes de los platillos de mi negocio.
- **R2.** Como dueño, quiero guardar la receta de un platillo con versiones y ver su costo teórico y margen, sin inventar cifras cuando falta un costo.
- **R3.** Como dueño, quiero que al vender un platillo con receta se descuenten solos los insumos con la receta de ese momento, que al anular se devuelvan y que un error de inventario no bloquee la venta.

## RED y GREEN por tarea

| Tarea | RED | GREEN |
|---|---|---|
| R1 dominio (`Temporada`, `Platillo`) | Compilación: `cannot find symbol` en `Temporada`, `Platillo`, `CodigoError`… | 19 pruebas |
| R1 `MenuServicio`, categorías, `MenuEnMemoria` | Las pruebas se escribieron antes de que existiera la clase (no se corrió la compilación fallida por separado) | 24 pruebas |
| R1 baja, fuente para Ventas, menú público, seguimiento de Negocios | Compilación: `cannot find symbol` en `AutorizacionDeRestaurantes`, `PlatillosParaVenta`, `MenuPublicoServicio`, `SeguimientoDeNegocios`, `RestaurantesEnMemoria` | 7 + 8 + 9 + 11 pruebas |
| R1 GraphQL (controlador, operaciones, esquema, errores) | Compilación: `cannot find symbol` en `RestaurantesGraphQlController`, `OperacionesDeRestaurantes`, `PlatilloInput`, `TraductorDeErroresDeRestaurantes`; luego un error de prueba (el esquema necesita el tipo `ComandoInput` de otro módulo) corregido en la prueba | 19 pruebas |
| R1 configuración, escucha, permisos de sesión | Compilación | 8 pruebas |
| R2 dominio (`Receta`) y servicios | Compilación: `cannot find symbol` en `Receta`, `InsumoDeReceta`, `InventarioPuerto`, `RecetasServicio`, `GuardarRecetaComando`… | `RecetaTest` 10, `RecetasServicioTest` 11, `ManejadorGuardarRecetaTest` 15 |
| R2 GraphQL | Compilación: falta `receta`, `costoDeReceta` y el constructor de 3 argumentos | +7 pruebas; un fallo de configuración (dos beans del mismo tipo) corregido |
| R3 consumo, fallos y contrato | Compilación: `cannot find symbol` en `ConsumidorDeVentas`, `ConsumosEnMemoria`, `ResultadoConsumo`, `ConsumosServicio`, `ConsumoFallido` | `ConsumidorDeVentasTest` 20, `ConsumosServicioTest` 3, `ContratoVentasConsumidoTest` 3 |
| R3 escucha y GraphQL | Compilación: `EscuchaDeVentas`; luego `consumosFallidos` y `mensaje` | 9 pruebas |

Una prueba mía de R3 falló al primer intento por una aserción demasiado estricta (contaba también las llamadas de la preparación); se corrigió la prueba, no el código.

## Resultado
| Comprobación | Resultado |
|---|---|
| `mvn -o test -Dtest='mx.negocio360.restaurantes.**.*Test,PermisosDeRestaurantesMigracionPostgresTest,RolesBaseTest,EstructuraModulosTest'` | **205 pruebas, 0 fallas, 0 errores** (185 del paquete `restaurantes`, más `RolesBaseTest`, la migración `V037` y la estructura de módulos) |
| `EstructuraModulosTest` (`ApplicationModules.verify()`) | Verde: `restaurantes` solo usa `compartido` y nadie lo usa |
| Suite completa `mvn -o test` | 2438 pruebas, **3 fallas y 542 errores, los mismos que antes de empezar**: la integración pendiente de Ventas y Caja («Operaciones de GraphQL mal declaradas» y `EsquemaGraphqlClientesTest`). Ninguna es de Restaurantes |
| JaCoCo del paquete `restaurantes` | **95.3 % de líneas (1010 de 1060) y 85.2 % de ramas (410 de 481)**. Lo que queda sin cubrir son accesores y rutas de los almacenes en memoria y de `InventarioNoDisponible` |

## Mutaciones: 16 de 16 detectadas
| Mutación | Quién la detecta |
|---|---|
| M1 editar un platillo sin comprobar la versión | `MenuServicioTest` |
| M2 la temporada no cuenta para ordenar | `PlatilloTest` |
| M3 agotado deja de ser vendible para Ventas (MN-14) | `PlatillosParaVentaTest` |
| M4 el menú público incluye lo no publicado | `MenuPublicoServicioTest` |
| M5 el menú público no revisa la visibilidad | `MenuPublicoServicioTest` |
| M6 el consumo usa la receta de hoy y no la del momento de la venta (RC-07) | `ConsumidorDeVentasTest` (2) |
| M7 el consumo ignora la merma | `ConsumidorDeVentasTest` (2) |
| M8 el consumo no revisa el evento ya procesado (R3) | `ConsumidorDeVentasTest` (2) |
| M9 el costo no reparte entre las porciones | `RecetasServicioTest` (2) |
| M10 guardar otra versión no cierra la anterior | `RecetaTest`, `ManejadorGuardarRecetaTest` (3) |
| M11 un evento viejo de Negocios pisa al nuevo | `SeguimientoDeNegociosTest` |
| M12 guardar receta no revisa el permiso | `ManejadorGuardarRecetaTest` |
| M13 un fallo reintentable se marca como procesado (D-122) | `ConsumidorDeVentasTest` |
| M14 se permiten dos platillos con el mismo nombre | `MenuServicioTest` |
| M15 la baja ignora las cuentas abiertas | `DarDeBajaPlatilloTest` |
| M16 el modo RECETA no exige una receta | `MenuServicioTest` |

Cada mutación se aplicó a una copia, se corrió su prueba y el archivo se restauró; una corrida posterior en verde lo confirma.

## Especificación de pruebas (resumen por regla)
| Reglas | Prueba | Tipo |
|---|---|---|
| MN-01, MN-05, MN-06, MN-08, MN-09 | `PlatilloTest`, `TemporadaTest` | unidad |
| MN-01, MN-04, MN-07, MN-12, MN-13, AZ-04, SG-02 | `MenuServicioTest` | unidad |
| MN-10 | `DarDeBajaPlatilloTest` (preparar y confirmar, permiso en los dos pasos, `COMANDO_CAMBIO`, token de un solo uso) | unidad |
| MN-02, MN-03, MN-14 | `PlatillosParaVentaTest` (kit de contrato de la fuente de productos) | contrato |
| PB-01 a PB-05 | `MenuPublicoServicioTest` (los campos permitidos se comprueban por reflexión) | unidad |
| DM-01, PB-01, R3, R5 | `SeguimientoDeNegociosTest`, `EscuchaDeNegociosTest` | unidad |
| RC-01 a RC-04, RC-07 | `RecetaTest` | unidad |
| RC-02, RC-03, RC-05, RC-06, RC-08, RC-10 | `RecetasServicioTest` (cifras de la cemita: 43.25, 41.75, 49.12 %; mole de 40 porciones: 13.00; 100.00 entre 3: 33.33), `ManejadorGuardarRecetaTest` | unidad |
| CM-01 a CM-06, CM-08, RC-07 | `ConsumidorDeVentasTest` | unidad |
| Contrato real con Ventas | `ContratoVentasConsumidoTest` (usa `VentaRegistrada(...).datos()` y `VentaAnulada(...).datos()` reales) | contrato |
| SG-01, SG-02, SG-13, SG-21, RC-10 | `OperacionesDeRestaurantesTest`, `EsquemaGraphqlRestaurantesTest` (con la tabla de operaciones de la plataforma), `TraductorDeErroresDeRestaurantesTest`, `RestaurantesGraphQlControllerTest` | unidad |
| Encendido por propiedad | `RestaurantesConfiguracionTest`, `SesionDeRestaurantesTest`, `EscuchaDeVentasTest` | unidad |

## Brechas conocidas
1. **Nada de esto se ha visto correr en la aplicación completa**: los contextos de Spring no arrancan por la integración pendiente de la Pista A (misma causa que el resto de la suite). La configuración se probó con un contexto reducido y el esquema con la tabla de operaciones, pero falta arrancar `desarrollo` de punta a punta.
2. **`guardarReceta` y `darDeBajaPlatillo` no están en GraphQL todavía**: dependen del despachador de comandos (D-125). Los manejadores ya pasan por `ServicioPreparacion` en las pruebas.
3. **Inventario no está conectado**: `InventarioNoDisponible` responde un fallo reintentable. Faltan `rastreo` y `activo` en `aBase` (R-I1), `registrarConsumo` y `revertirConsumo` (R-I2) y `@NamedInterface` (R-I4). Las pruebas usan un doble que cumple el contrato del puerto.
4. **La suscripción `alertasRestaurante` y el reintento manual del consumo fallido** no se hicieron (P2 el reintento; la suscripción depende de `EventosPorNegocio`).
5. **Los repositorios son en memoria** (`restaurantes.almacen=memoria`, solo en el perfil de desarrollo). La persistencia, la seguridad por fila y las migraciones `V060` a `V065` son R7, que no está autorizada.
6. **`negocio.estado_cambiado`**: lo lee el seguimiento, pero Negocios todavía no lo publica.
7. Commit «feat(restaurantes): P1a (R0 a R3) con TDD…» en la rama `julio` a pedido del usuario; sin push.
