package mx.negocio360.ventas.aplicacion.puertos;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cupon del Pasaporte con el descuento que aplica a un subtotal.
 *
 * @param cuponId   id opaco del cupon en Pasaporte
 * @param descuento monto a descontar, con 2 decimales
 */
public record CuponCotizado(UUID cuponId, BigDecimal descuento) {
}
