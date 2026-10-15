package mx.negocio360.contabilidad.dominio;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Renglon de una poliza: una cuenta con un cargo o un abono (tabla {@code movimiento}).
 *
 * @param cuentaId cuenta afectada
 * @param cargo    monto del cargo
 * @param abono    monto del abono
 * @param concepto texto opcional del renglon
 */
public record Renglon(UUID cuentaId, BigDecimal cargo, BigDecimal abono, String concepto) {
}
