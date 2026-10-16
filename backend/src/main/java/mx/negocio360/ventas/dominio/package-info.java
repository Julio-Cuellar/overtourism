/**
 * Dominio de Ventas: la venta, sus lineas y pagos, y los codigos de error. Se expone como interfaz con nombre
 * {@code dominio} porque {@code VentasApi} devuelve estos tipos y los adaptadores reportan fallos con
 * {@link mx.negocio360.ventas.dominio.VentasException}.
 */
@NamedInterface("dominio")
package mx.negocio360.ventas.dominio;

import org.springframework.modulith.NamedInterface;
