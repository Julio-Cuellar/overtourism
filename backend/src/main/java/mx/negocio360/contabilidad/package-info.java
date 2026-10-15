/**
 * Modulo horizontal de Contabilidad (D-76). Lleva el registro contable del negocio en partida doble a partir de los
 * eventos {@code venta.registrada}, {@code cupon.canjeado} y {@code caja.cerrada} (D-77). Solo puede depender de
 * {@code compartido}: no llama a la API de ningun otro modulo ni lee sus tablas (R1, R2, R6, E-07).
 */
@ApplicationModule(displayName = "Contabilidad", allowedDependencies = "compartido")
package mx.negocio360.contabilidad;

import org.springframework.modulith.ApplicationModule;
