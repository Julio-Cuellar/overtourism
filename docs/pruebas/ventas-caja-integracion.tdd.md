# Evidencia — Integración de Ventas, Caja, Clientes e Inventario con Identidad (2026-10-16)

**Plan de origen:** `docs/planes/integracion-ventas-caja.md` (Etapa 1, F0 a F4) más la ampliación a Clientes e Inventario. **Entorno:** `mvn -B verify` en `backend/`, JDK 21, PostgreSQL 17 en Docker (puerto 5433), Maven 3.9.12.

## Punto de partida
Suite completa tras la mezcla de `uriel` (según el plan): 2249 pruebas, 3 fallas y 542 errores; una sola causa en los errores (la tabla de operaciones SG-21 no conocía las operaciones del canal).

## Ciclos

| Ciclo | RED | GREEN |
|---|---|---|
| F1 permisos y roles | Pruebas de `PermisosDeVentas`, `PermisosDeCaja` y la matriz de la sección 8.2 escritas antes que el código (no compilaban) | `RolesBaseTest` 17, `PermisosDe*Test`, `EsquemaEquipoPostgresTest` 16 contra PostgreSQL real (migración = `RolesBase`) |
| F2 `Autorizacion` | Las ~114 pruebas de Ventas y Caja pasaron a las firmas nuevas sin cambiar una aserción de dominio; las de permiso se escribieron nuevas | Ventas + Caja: 160 pruebas en verde (antes 142, +18 de consultas, paginación, tope de cajas y revocación entre pasos) |
| F3 despachador y GraphQL | `GraphQlVentasCajaTest` escrita contra el esquema final | 9 de 9 a la primera corrida de la clase, con sesiones reales de `RolesBase` |
| F4 SPI de productos | `CatalogoCompuestoTest` (6), `CuponesNoDisponiblesTest`, `FuenteDeProductosSimuladaTest` (3) | en verde |
| Clientes | `GraphQlClientesTest` (6) | en verde tras ajustar una aserción mía (el teléfono se enmascara con «••••», no con «*») |
| Inventario | `PermisosDeInventarioTest`, matriz en `RolesBaseTest`, `GraphQlInventarioTest` (6) | en verde |

## Qué garantizan las pruebas de punta a punta
| Garantía | Prueba |
|---|---|
| El dueño crea caja, abre turno, cobra, ve saldo y ventas, anula y corta | `GraphQlVentasCajaTest.caminoDelDueno` |
| El cajero cobra pero no anula, no retira, no crea cajas ni audita cortes | `cajero_cobraPeroNoAnula` |
| Mesero, cocinero y guía no cobran ni tocan la caja | `roles_sinCaja` |
| Sin sesión o sin negocio nada responde, ni siquiera confirmar | `anonimoYSinNegocio_negados` |
| Dos negocios no se ven; un token ajeno no confirma | `dosNegocios_noSeVen` (Ventas/Caja, Clientes, Inventario) |
| Token con prefijo desconocido o de otro módulo: `TOKEN_INVALIDO` sin revelar módulos | `token_prefijoDesconocido` |
| Permiso revocado entre preparar y confirmar: `PERMISO_DENEGADO` y el token sigue vivo | `permisoRevocadoEntrePasos` (los tres módulos) |
| Un cupón es inválido mientras no exista Pasaporte | `erroresDeDominio` |
| Lo no programado de Clientes responde `OPERACION_NO_DISPONIBLE` / `COMANDO_NO_DISPONIBLE` | `GraphQlClientesTest.loPendiente_noDisponible` |
| El cocinero ve existencias y registra salidas, pero no crea artículos ni ajusta | `GraphQlInventarioTest.cocinero_soloOperaLaCocina` |
| Las reseñas anónimas de Negocios siguen funcionando con el despachador | `GraphQlEntradaTest.resenas_*` (20, sin cambiar una aserción) |

## Resultado de la verificación completa
`mvn -B verify -Dmaven.test.failure.ignore=true`: **2313 pruebas, 1 falla, `All coverage checks have been met`**. La falla fue `ResenasTest.ocultarYRestituir` (Negocios), intermitente por un reloj fijo y un `ORDER BY` sin desempate; se corrigió su aserción y la clase pasó sola. La suite completa **no se repitió** después de ese cambio.

## Arranque real
`mvn spring-boot:run -Dspring-boot.run.profiles=desarrollo` (puerto 8091): registro → código de desarrollo → sesión → crear caja → abrir turno → cobrar 3 tacos y medio kilo de camote simulados (total 66.00, cambio 34.00, folio 1) → anular → saldo de caja → artículo y entrada de Inventario → `miNegocio` → una petición sin token responde `NO_AUTENTICADO`.

## Límites conocidos
- Ventas, Caja e Inventario viven en memoria (F6 pendiente); con el perfil `produccion` no se encienden.
- Eventos por el bus de Spring, sin outbox (F5 pendiente).
- Sin ronda de mutaciones ni revisión de `ecc:java-reviewer` y `ecc:security-reviewer` (F7 pendiente).
- Ventas → Clientes y Ventas → Inventario por eventos no están conectados.
- CORS para Flutter web: pendiente de decisión.
- No hay commits: la convención del repo es no commitear hasta que el usuario lo pida.
