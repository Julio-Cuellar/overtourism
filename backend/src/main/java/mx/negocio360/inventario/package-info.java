/**
 * Modulo horizontal de Inventario (docs/diseno/inventario.md). Solo puede depender de {@code compartido}: no conoce a
 * ningun vertical (R4) y los demas modulos lo usan por {@link mx.negocio360.inventario.InventarioApi} o por sus
 * eventos (R1, R2 y R6).
 *
 * <p>Este primer tramo es el nucleo (prioridad 1): articulos, unidades y conversion, movimientos, saldo y costo
 * promedio ponderado. Lotes, series y consumo por venta quedan fuera.
 */
@ApplicationModule(displayName = "Inventario", allowedDependencies = "compartido")
package mx.negocio360.inventario;

import org.springframework.modulith.ApplicationModule;
