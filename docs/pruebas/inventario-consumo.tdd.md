# Evidencia TDD: consumo por venta de Inventario y conexión con Restaurantes (R-I1 a R-I4)

**Fecha:** 2026-10-16. **Rama:** `inventario-consumo` (desde `julio`). **Plan:** el entregado en la sesión (sin archivo `*.plan.md`), aceptado por el usuario. **Entorno:** JDK 21, `mvn -o`.

## Recorridos de usuario
- Como **Restaurantes**, quiero conocer en una llamada el CPP, la unidad base, si está activo y cómo se rastrea cada insumo, para armar y costear recetas y rechazar series (S-05).
- Como **Restaurantes**, quiero descontar los insumos de una venta y reponerlos si se anula, sin doble descuento aunque el evento llegue repetido o en desorden (K-01 a K-04).
- Como **plataforma**, quiero que `ApplicationModules.verify()` deje a `restaurantes` usar los tipos de `InventarioApi` (R-I4).

## RED y GREEN por tarea
| Tarea | RED | GREEN |
|---|---|---|
| T1 `articulosPorId` y `Rastreo` (R-I1, R-I3) | `test-compile` falló: faltaban `Rastreo`, `ArticuloBase`, `articulosPorId` y `RASTREO_NO_SOPORTADO` | `mvn -o test -Dtest='mx.negocio360.inventario.**.*Test'`: 120 pruebas, 0 fallas |
| T2 consumo (R-I2) | `test-compile` falló: faltaban `ConsumosServicio`, `Consumo`, `RepositorioConsumos`, `ConsumoResultado`, `EstadoDeConsumo`, `LineaDeConsumo` | 140 pruebas de Inventario, 0 fallas (20 nuevas en `ConsumoPorVentaTest`) |
| T3 `@NamedInterface` (R-I4) | **RED de ejecución:** `EstructuraModulosTest.verify` falló con nueve violaciones («depends on module 'inventario' via InventarioApiComoPuerto -> …ArticuloBase… Allowed targets: compartido, inventario») | Con `@NamedInterface("api")` falló distinto (hacía falta declarar `inventario::api`); con eso, 5 pruebas en verde |
| T4 adaptador y cableado | `test-compile` falló (`InventarioApiComoPuerto`, `InventarioEnMemoria`, `PreparacionesDeInventarioEnMemoria`); `RestaurantesConfiguracionTest.conInventarioApi_usaElAdaptador` falló en ejecución (seguía usando `InventarioNoDisponible`) | 163 pruebas del conjunto relacionado, 0 fallas |

Dos pruebas mías fallaron por errores de la propia prueba y se corrigieron sin tocar producción: una usaba `Instant.now()` contra el reloj fijo del escenario (daba `FECHA_FUTURA`) y otra generaba UUIDv7 del mismo milisegundo (sin orden definido).

## Suite completa
`mvn -o test`: **2486 pruebas, 3 fallas, 542 errores.** Antes de empezar eran 2438 con las mismas 3 y 542 (integración pendiente de Ventas y Caja: contextos de Spring que no arrancan). Las 48 pruebas nuevas pasan; ninguna falla es de Inventario ni de Restaurantes.

## Mutaciones (15 de 15 detectadas)
Cada una se aplicó a una copia, se corrieron las pruebas nuevas y el archivo se restauró (script en la carpeta temporal de la sesión; **después hubo que hacer `mvn clean`**, porque la clase compilada de la última mutación quedó obsoleta en `target/`).

| Mutación | Detectada por |
|---|---|
| M1 sin idempotencia al registrar | `ConsumoPorVentaTest` |
| M2 la reversa usa el CPP vigente y no el costo original | `ConsumoPorVentaTest` |
| M3 la marca `ANULADO_PREVIO` no omite el consumo | `ConsumoPorVentaTest` |
| M4 el consumo se guarda antes de escribir (rompe la atomicidad ante una carrera) | `ConsumoPorVentaTest` |
| M5 `articulosPorId` omite inactivos | `ArticulosPorIdTest` |
| M6 el adaptador nunca marca reintentable | `InventarioApiComoPuertoTest` |
| M7 crear acepta `LOTE` y `SERIE` | `ArticulosPorIdTest` |
| M8 `revertirConsumo` no valida el consumidor | `ConsumoPorVentaTest` |
| M9 costo total con signo | `ConsumoPorVentaTest` |
| M10 tandas de 1000 en el adaptador | `InventarioApiComoPuertoTest` |
| M11 el adaptador pierde el rastreo | `InventarioApiComoPuertoTest` |
| M12 consumo sin validar fecha futura | `ConsumoPorVentaTest` |
| M13 dos líneas del mismo artículo no se encadenan | `ConsumoPorVentaTest` |
| M14 la reversa se permite en artículos inactivos | `ConsumoPorVentaTest` |
| M15 `marcarUsada` sin control de uso único | `InventarioEnMemoriaTest` |

## Especificación de pruebas
| Regla o garantía | Prueba | Tipo |
|---|---|---|
| R-I1, R-I3: varios artículos en una llamada con unidad base, CPP, `activo` y `rastreo`; los ausentes y los de otro negocio no aparecen; más de 100 ids falla | `ArticulosPorIdTest` | unidad |
| D-191: crear sin rastreo o con `NINGUNO` funciona; `LOTE` y `SERIE` se rechazan; el dominio conserva el rastreo | `ArticulosPorIdTest` | unidad |
| K-01 idempotencia, llave con consumidor y negocio, fecha del hecho, alertas por cruce (A-02), saldo negativo (A-01), eventos con `causa_id` | `ConsumoPorVentaTest` | unidad |
| K-02 una línea mala no descuenta ninguna; carrera por el saldo no deja consumo guardado | `ConsumoPorVentaTest` | unidad |
| K-03 reversa al costo original, idempotente; K-04 marca y omisión posterior | `ConsumoPorVentaTest` | unidad |
| D-22 no usa preparar → confirmar | `ConsumoPorVentaTest` | unidad |
| R-I4 `verify()` con `restaurantes` dependiendo de `inventario::api` | `EstructuraModulosTest` | estructura |
| Adaptador: traducción de tipos y rastreo, errores permanentes y reintentables, tandas, ciclo de consumo y reversa contra el Inventario real | `InventarioApiComoPuertoTest` | integración en memoria |
| Almacenes en memoria: aislamiento por negocio, versión, orden del kardex, uso único del token con 8 hilos | `InventarioEnMemoriaTest` | unidad |
| Encendido por propiedad, publicación en el bus de Spring y uso del adaptador | `InventarioConfiguracionTest`, `RestaurantesConfiguracionTest` | contexto reducido |

## Cobertura (JaCoCo, solo las pruebas relacionadas)
`inventario.aplicacion` 96.9 % de instrucciones y 83.4 % de ramas; `inventario.dominio` 97.4 % y 89.6 %; `inventario.infraestructura` 98.5 % y 88.1 %; `InventarioApiComoPuerto` 100 % y 100 %; `ConsumosServicio` 85.5 % y 66.7 % (ramas sin cubrir: `requireNonNull` de `causaId` y `lineas`).

## Brechas conocidas
1. **K-02 no es atómico con repositorios en memoria** más allá de las validaciones previas (ver la Bitácora).
2. **Sin lotes, FEFO ni series.** El costo es el CPP.
3. **Nada corrió con la aplicación arrancada** (misma causa que el resto de la suite).
4. **JavaDoc:** `javadoc -Xdoclint:all` sobre los tipos nuevos no marca nada faltante; quedan las advertencias de `@param` de `Articulo` y `CrearArticuloComando`, de estilo anterior.
5. La decisión D-192 (reversa en un artículo dado de baja) y D-190 son propuestas mías.
