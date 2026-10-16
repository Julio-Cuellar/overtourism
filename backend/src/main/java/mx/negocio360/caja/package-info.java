/**
 * Modulo horizontal de Caja ({@code docs/diseno/caja.md}). Controla el dinero de un punto de cobro durante un turno:
 * fondo, entradas, retiros, lo que llega de las ventas y el corte.
 *
 * <p>Depende de {@code ventas} en un solo sentido (D-76): escucha {@code venta.registrada} y {@code venta.anulada}
 * con su propio lector tolerante y llama a {@code VentasApi.resumenTurno} al hacer el corte. Ventas no conoce a Caja.
 */
@ApplicationModule(displayName = "Caja", allowedDependencies = {"compartido", "ventas"})
package mx.negocio360.caja;

import org.springframework.modulith.ApplicationModule;
