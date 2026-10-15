package mx.negocio360.ventas.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Una forma de pago de la venta (VT-04). Todos los montos van en escala 2.
 *
 * @param metodo     efectivo, tarjeta o transferencia
 * @param monto      parte del total que cubre este pago
 * @param recibido   efectivo entregado; nulo fuera del efectivo
 * @param cambio     {@code recibido − monto}; nulo fuera del efectivo
 * @param referencia autorizacion de la terminal o de la transferencia, o nula
 */
public record PagoVenta(MetodoPago metodo, BigDecimal monto, BigDecimal recibido, BigDecimal cambio,
                        String referencia) {

    /** Exige metodo y monto. */
    public PagoVenta {
        Objects.requireNonNull(metodo, "metodo");
        Objects.requireNonNull(monto, "monto");
    }
}
