package mx.negocio360.inventario.dominio;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Resultado de llevar una cantidad capturada a la unidad base del articulo. Se guarda completo en el movimiento para
 * que la conversion sea auditable (U-05).
 *
 * @param cantidadCapturada lo que escribio la persona, tal cual
 * @param unidadCapturada   la unidad que uso, en forma canonica
 * @param factorAplicado    cuantas unidades base vale una unidad capturada (8 decimales)
 * @param cantidadBase      la cantidad en unidad base (4 decimales)
 */
public record Conversion(BigDecimal cantidadCapturada, String unidadCapturada, BigDecimal factorAplicado,
                         BigDecimal cantidadBase) {

    public Conversion {
        Objects.requireNonNull(cantidadCapturada, "cantidadCapturada");
        Objects.requireNonNull(unidadCapturada, "unidadCapturada");
        Objects.requireNonNull(factorAplicado, "factorAplicado");
        Objects.requireNonNull(cantidadBase, "cantidadBase");
    }
}
