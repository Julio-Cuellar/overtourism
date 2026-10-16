package mx.negocio360.caja.contratos;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.Objects;
import mx.negocio360.caja.dominio.MetodoPago;

/**
 * Un pago dentro de {@code venta.registrada} o {@code venta.anulada}: metodo como texto y monto como cadena decimal.
 *
 * @param metodo EFECTIVO, TARJETA o TRANSFERENCIA
 * @param monto  escala 2
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PagoV1(String metodo, BigDecimal monto) {

    /** Exige metodo y monto: un pago sin ellos es un evento mal formado. */
    public PagoV1 {
        Objects.requireNonNull(metodo, "metodo");
        Objects.requireNonNull(monto, "monto");
    }

    /**
     * Metodo como tipo de Caja.
     *
     * @return el metodo
     * @throws IllegalArgumentException si Ventas manda un metodo que Caja no conoce
     */
    public MetodoPago metodoDeCaja() {
        return MetodoPago.valueOf(metodo);
    }
}
