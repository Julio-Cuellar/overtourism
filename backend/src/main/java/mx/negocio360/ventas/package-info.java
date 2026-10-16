/**
 * Modulo horizontal de Ventas ({@code docs/diseno/ventas.md}). Registra que se vendio, a que precio, con que
 * descuento y como se pago.
 *
 * <p>Solo depende de {@code compartido} (D-76): no conoce a Caja, Clientes, Inventario ni a ningun vertical. Catalogo
 * y Pasaporte se usan a traves de puertos propios ({@code aplicacion.puertos}) porque esos modulos todavia no
 * existen; cuando existan, sus adaptadores implementaran esos puertos llamando a sus API publicas (R2, R6).
 */
@ApplicationModule(displayName = "Ventas", allowedDependencies = "compartido")
package mx.negocio360.ventas;

import org.springframework.modulith.ApplicationModule;
