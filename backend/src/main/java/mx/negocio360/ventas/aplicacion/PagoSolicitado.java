package mx.negocio360.ventas.aplicacion;

import java.math.BigDecimal;
import mx.negocio360.ventas.dominio.MetodoPago;

/**
 * Forma de pago que captura el cajero (VT-04).
 *
 * @param metodo     efectivo, tarjeta o transferencia
 * @param monto      parte del total, mayor que 0 y con hasta 2 decimales
 * @param recibido   solo en efectivo: lo que entrego la persona; si falta, se toma el monto exacto
 * @param referencia autorizacion de la terminal o de la transferencia, opcional
 */
public record PagoSolicitado(MetodoPago metodo, BigDecimal monto, BigDecimal recibido, String referencia) {
}
