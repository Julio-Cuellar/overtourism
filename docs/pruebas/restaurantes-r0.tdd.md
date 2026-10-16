# Restaurantes R0 — evidencia TDD

**Fecha:** 2026-10-16 · **Plan:** [`docs/diseno/restaurantes.md`](../diseno/restaurantes.md) §16, fase R0 · **Autorización:** Bitácora, «Autorización: programar Restaurantes P1a, empezando por R0» · **JDK:** 21 (`JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64`), Maven sin conexión (`mvn -o`).

## Historias de usuario
- Como **plataforma**, quiero que Restaurantes aparezca como módulo vertical que solo depende de `compartido` e `inventario` y del que nadie depende (R4, D-160).
- Como **dueño de un restaurante**, quiero que los permisos de Restaurantes estén declarados como datos y repartidos entre los roles según la matriz de la sección 11.
- Como **dueño**, quiero poder darle a alguien el rol «Mesero con caja» (D-171), que aparece en la plantilla del giro RESTAURANTE justo después de MESERO.

## RED
| Paso | Comando | Resultado |
|---|---|---|
| 1. Pruebas escritas, sin código | `mvn -o -Dtest=PermisosDeRestaurantesTest,RolesBaseTest,EstructuraModulosTest,RolesYPermisosTest,PermisosDeRestaurantesMigracionPostgresTest test` | **RED de compilación:** `cannot find symbol: class PermisosDeRestaurantes`. Es la señal buscada: la prueba instancia código que no existe |
| 2. Solo el catálogo y el paquete del módulo | mismo comando | **RED en ejecución:** 34 pruebas, 12 fallas y 1 error. `RolesBaseTest` (8 fallas y 1 error: nueve roles, matriz, plantilla), `RolesYPermisosTest` (1) y `PermisosDeRestaurantesMigracionPostgresTest` (3: sin permisos ni rol sembrados). Todas por la razón correcta: faltaban `RolesBase` y `V037` |

## GREEN
| Paso | Resultado |
|---|---|
| `RolesBase` con la matriz y `MESERO_CAJA`, y `V037__identidad_permisos_de_restaurantes.sql` | `RolesBaseTest` 17/17, `PermisosDeRestaurantesTest` 5/5, `PermisosDeRestaurantesMigracionPostgresTest` 3/3, `EstructuraModulosTest` 4/4 |
| Los catálogos copiados en `Escenario` y `PerfilProduccionTest` (un módulo no ve el catálogo del otro) y los conteos 30 → 48 de `RolesYPermisosTest`, `NegocioActivoTest` y `NegociosControllerTest` | `RolesYPermisosTest` 5/5, `NegocioActivoTest` 18/18 |
| Suite completa `mvn -o test` | 2262 pruebas, **3 fallas y 542 errores: exactamente los mismos que antes de empezar** (2249 pruebas, 3 y 542 en la Bitácora del 2026-10-16). Todos vienen de la integración pendiente de Ventas y Caja: `EsquemaGraphqlClientesTest` (3) y los contextos de Spring que no arrancan por «Operaciones de GraphQL mal declaradas». Ninguna prueba nueva falla |

## Especificación de pruebas
| # | Qué se garantiza | Prueba | Tipo | Resultado |
|---|---|---|---|---|
| 1 | El módulo declara exactamente los 18 permisos de P1, sin repetir, todos de `restaurantes`, ninguno sensible y con descripción | `PermisosDeRestaurantesTest` (5) | unidad | PASS |
| 2 | Hay nueve roles base y la plantilla de RESTAURANTE ofrece `MESERO_CAJA` entre `MESERO` y `COCINERO` | `RolesBaseTest.todos_sonNueveRoles`, `plantilla_porGiro` | unidad | PASS |
| 3 | Cada rol tiene exactamente los permisos de Restaurantes de la matriz de la sección 11 (18 permisos × 9 roles) | `RolesBaseTest.restaurantes_matrizDeLaSeccion11` | unidad | PASS |
| 4 | El gerente tiene 17 (todos menos configurar el salón); taquillero y guía, ninguno | `restaurantes_gerenteTaquilleroYGuia` | unidad | PASS |
| 5 | El dueño tiene 48 permisos; cajero, mesero y cocinero los suyos exactos; `MESERO_CAJA` tiene hoy los del mesero | `dueno_tieneTodos`, `cajero_…`, `mesero_…`, `cocineroTaquilleroYGuia`, `meseroCaja_hoyTieneLosDelMesero` | unidad | PASS |
| 6 | R4: ningún módulo depende de `restaurantes`; D-160: solo depende de `compartido` e `inventario`; `verify()` sin violaciones | `EstructuraModulosTest` (4) | estructura | PASS |
| 7 | Con Postgres real, `V037` siembra los 18 permisos con su descripción, el rol `MESERO_CAJA`, los permisos de cada rol y el orden de la plantilla, iguales a `RolesBase` | `PermisosDeRestaurantesMigracionPostgresTest` (3) | integración (Flyway sobre una base nueva, sin Spring) | PASS |
| 8 | Los 48 permisos de la plataforma y los 4 módulos con su conteo; los roles base solo citan permisos declarados | `CatalogoDePermisosPlataformaTest`, `EsquemaEquipoPostgresTest` (actualizadas) | integración con Spring | **No se pudo ejecutar**: el contexto no arranca por la integración pendiente de la Pista A (causa ajena). Quedan actualizadas y se ejecutan cuando esa pista cierre |

## Mutaciones (4 de 4 detectadas)
| Mutación | Quién la detecta |
|---|---|
| El gerente recibe `salon.configurar` | `RolesBaseTest` (3 fallas) |
| El mesero pierde `linea.anular` | `RolesBaseTest` (3 fallas) |
| `V037` no mueve a `COCINERO` al orden 7 | `PermisosDeRestaurantesMigracionPostgresTest` (error de llave única) |
| `V037` quita un permiso a `MESERO_CAJA` | `PermisosDeRestaurantesMigracionPostgresTest` (1 falla) |

Los archivos se restauraron y se comprobó con `cmp` contra las copias.

## Cobertura
JaCoCo sobre las clases nuevas y la modificada: `PermisosDeRestaurantes` 3/3 líneas, `RolesBase` 44/44. El paquete `restaurantes` no tiene más código en R0.

## Brechas conocidas
1. Las dos pruebas con contexto de Spring (punto 8) no se ejecutaron.
2. **`MESERO_CAJA` aún no puede cobrar.** `ventas.venta.ver`, `ventas.venta.registrar` y `caja.turno.ver` no existen hasta que la Pista A los declare (F1). Van en una migración posterior (propuesta: la `V039`, reservada) porque en una base nueva la `V035` corre antes que la `V037`. La prueba `meseroCaja_hoyTieneLosDelMesero` documenta el estado y se cambia entonces.
3. **Flyway sin `out-of-order`.** Una base de desarrollo que ya tenga `V037` aplicada rechazará una `V035` o `V036` que llegue después («migración resuelta sin aplicar»). En una base nueva no pasa. Hay que acordarlo con la Pista A (repair, `outOfOrder` en desarrollo o recrear la base).
4. Sin commits: la skill pide puntos de control por etapa, pero no se hace commit sin que lo pidan.
