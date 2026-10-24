# Evidencia TDD — Persistencia en PostgreSQL y outbox de Ventas, Caja e Inventario (F5 y F6, 2026-10-16)

**Plan de origen:** `docs/planes/integracion-ventas-caja.md`, secciones F5 y F6. **Autorización:** entrada de la Bitácora «Autorización: persistencia en PostgreSQL y outbox de Ventas, Caja e Inventario (F5 y F6)».
**Entorno:** `mvn -B verify` en `backend/`, JDK 21, PostgreSQL 17 en Testcontainers (rol de la aplicación sin superusuario), Maven 3.9.12.

## Historias de usuario

- Como dueño de un negocio, quiero que mis ventas, mi caja y mi inventario sobrevivan a un reinicio y que ningún otro negocio los vea.
- Como cajero, quiero que cobrar sea todo o nada: la venta, su folio y su aviso a Caja se confirman juntos o ninguno, y el folio nunca deja huecos.
- Como encargado, quiero que la caja refleje cada venta aunque Caja estuviera ocupada o se reiniciara, sin sumarla dos veces y sin que una anulación que llegó primero se pierda.
- Como quien despliega, quiero que la aplicación **no arranque** en producción si falta el almacén de Ventas, Caja o Inventario, en vez de arrancar sin ellos o con datos que se pierden.

## Qué se hizo, con pruebas primero

| Pieza | RED (la prueba antes que el código) | GREEN |
|---|---|---|
| Contrato de preparaciones (`RepositorioPreparaciones` con negocio) | `ContratoRepositorioPreparacionesTest` (7) contra memoria y doble: no compilaba con `porHash(negocio, hash)` | Puerto, `PreparacionesEnMemoria`, doble de pruebas y `ServicioPreparacion` con negocio; `RepositorioPreparacionesJdbc` (11 contra `ventas.preparacion` y 11 contra `caja.preparacion`) |
| Paginación por consulta | `CursoresTest` +3 | `Cursores.pagina(Consulta, …)`: el mismo cursor que la lista en memoria |
| Ventas | `ContratoRepositorioVentasTest` (11): **2 fallas reales en memoria** | `RepositorioVentasJdbc`: 19 contra PostgreSQL (11 del contrato + 8 propias) |
| Caja | `ContratoAlmacenDeCajaTest` (13): **1 falla real en memoria** | `RepositorioCajaJdbc` y `LibroDeCajaJdbc`: 19 contra PostgreSQL (13 + 6) |
| Inventario | `ContratoAlmacenDeInventarioTest` (17): **2 fallas reales en memoria** | `InventarioJdbc` y `PreparacionesJdbc`: 23 + 4 contra PostgreSQL |
| Eventos por outbox | `EscuchasDeVentasTest` (6) y `VentasACajaPorOutboxPostgresTest` (7): la prueba del candado por venta falló antes de agregarlo | `EscuchaDeVentasEnOutbox` (`@ApplicationModuleListener`), `PublicadorEventosEnBus` ×3, candado `pg_advisory_xact_lock` en `ventaAplicada` |
| Transacción por llamada | `VentasTransaccionPostgresTest` (4) | `FronteraTransaccional` sobre `VentasApi`, `CajaApi` e `InventarioApi` |
| Aislamiento por fila | `AislamientoVentasCajaInventarioPostgresTest` (47) | V040, V041, V042, V070 y V071 |
| Guarda de producción | `AlmacenesObligatoriosEnProduccionTest` (8) | `AlmacenesObligatoriosEnProduccion` |
| La misma batería en los dos almacenes | `GraphQlVentasCajaContratoTest` (9) y `GraphQlInventarioContratoTest` (6) | Subclases `…MemoriaTest` y `…PostgresTest` que solo cambian la propiedad |

## Hallazgos que cambiaron el código (los encontraron las pruebas)

1. **Los tres almacenes en memoria dejaban que un negocio pisara datos de otro.** `VentasEnMemoria.actualizar`, `CajaEnMemoria.actualizarTurno` e `InventarioEnMemoria.actualizar` (artículo y saldo) buscaban por id sin comprobar el negocio: con un id igual pero otro negocio, la actualización reemplazaba la fila ajena. Con UUIDv7 no ocurre por azar, pero el contrato lo exige y ahora lo cumplen (la base ya lo hacía por la política de seguridad por fila).
2. **`porTurno` de Ventas en memoria no devolvía las ventas en orden de folio**, como dice el puerto; devolvía el de inserción.
3. **La seguridad por fila exige el negocio en cada operación**: las preparaciones de Ventas, Caja e Inventario no lo recibían en `porHash`, `marcarUsada` ni `vaciar`. Se agregó, igual que en Clientes.
4. **`marcarUsada` en memoria no vaciaba el comando** y en la base sí (SC-10 lo impone con una restricción); el contrato exige lo mismo en los dos.
5. **El comando preparado se guarda como texto, no como `jsonb`**: el hash del comando se calcula sobre el texto exacto, y `jsonb` lo reordena (Clientes tuvo que hacer el hash canónico por eso).
6. **Una venta y su anulación pueden procesarse a la vez en hilos distintos** (el outbox es asíncrono y sin orden): sin un candado por venta, las dos verían la venta como desconocida y quedaría aplicada una venta anulada. `ventaAplicada` toma `pg_advisory_xact_lock` por negocio y venta.
7. **Un evento que Caja todavía no puede aplicar no se puede perder**: con el bus en proceso bastaba un aviso; con el outbox, la escucha lanza para que quede pendiente y se reenvíe al reiniciar. Un evento que no cumple el contrato solo se reporta (reintentarlo no lo arregla).

## Garantías (qué prueba cada una)

| Garantía | Prueba |
|---|---|
| Cada tabla con `negocio_id` tiene la seguridad activada y forzada, con su política; la lista de tablas es la real | `AislamientoVentasCajaInventarioPostgresTest#laListaEsCompleta`, `#seguridadForzada` |
| Cada negocio ve solo lo suyo y sin negocio no se ve nada; no se escribe en otro negocio; el negocio no se filtra entre transacciones | `#lecturaAislada`, `#escrituraAislada`, `#negocioNoSeFiltra` |
| El folio no se repite ni deja huecos (8 hilos; deshacer la venta deshace el folio) | `RepositorioVentasJdbcPostgresTest#foliosSimultaneos`, `#folioSeDeshaceConLaVenta` |
| Una venta no se edita: líneas y pagos solo se insertan, la venta solo se anula, ni el dueño de la base lo evita | `#inmutable`, `#laAplicacionSoloAnula` |
| Un turno sin cerrar por caja; un corte por turno; movimientos y cortes de solo inserción; el movimiento no se cuelga de un turno de otro negocio | `AlmacenDeCajaJdbcPostgresTest` |
| El nombre de un artículo activo es único aunque la carrera pase la revisión del servicio; el kardex es de solo inserción; el signo de la cantidad corresponde al tipo | `AlmacenDeInventarioJdbcPostgresTest` |
| Venta + folio + evento son una sola transacción; si el evento no se publica no queda nada; el token se gasta aunque falle | `VentasTransaccionPostgresTest` |
| El evento llega a Caja por el outbox y se completa; repetido o con otro id deja un efecto; la anulación previa no suma; sin turno queda pendiente; si la transacción se deshace no hay evento; venta y anulación simultáneas se serializan | `VentasACajaPorOutboxPostgresTest` |
| La guarda de producción: falta o valor distinto de `postgres` (incluido `memoria`) → no arranca, con el mensaje de qué definir | `AlmacenesObligatoriosEnProduccionTest` |
| Ventas, Caja e Inventario se comportan igual con memoria y con PostgreSQL de punta a punta por HTTP | `GraphQlVentasCaja{Memoria,Postgres}Test`, `GraphQlInventario{Memoria,Postgres}Test` |

## Verificación

`mvn -B verify` (JDK 21, PostgreSQL 17 en Testcontainers): **2548 pruebas, 0 fallas**, `All coverage checks have been met`. La primera corrida falló una prueba: `EsquemaPostgresTest#esquemas_soloLosDelDiseno` enumera los esquemas de la base y no conocía `ventas`, `caja` ni `inventario`; se actualizó la lista.

**Mutaciones** (cada una se aplicó, se corrió la clase de pruebas indicada y se restauró el archivo): quitar `AND version = :esperada` de `RepositorioVentasJdbc.actualizar` → falla `RepositorioVentasJdbcPostgresTest`; quitar `caja.almacen` de la guarda → 2 fallas en `AlmacenesObligatoriosEnProduccionTest`; que la escucha del outbox nunca lance → falla `EscuchasDeVentasTest`; cambiar el índice de turno sin cerrar de `<> 'CERRADO'` a `= 'ABIERTO'` en la consulta → falla `AlmacenDeCajaJdbcPostgresTest`; quitar el filtro de activos de `InventarioJdbc.buscar` → falla `AlmacenDeInventarioJdbcPostgresTest`.

## Huecos conocidos

Sin revisiones independientes (`java-reviewer`, `security-reviewer`); sin arranque manual contra PostgreSQL con `spring-boot:run`; sin prueba de reentrega al reiniciar (`republish-outstanding-events-on-restart`); la carrera cerrar turno / aplicar venta (A2) sigue abierta; ver la Bitácora.
