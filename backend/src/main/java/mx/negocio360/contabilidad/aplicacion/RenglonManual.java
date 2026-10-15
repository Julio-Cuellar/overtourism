package mx.negocio360.contabilidad.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Renglon tal como lo captura una persona. Se valida al preparar, no al construirse, para que el error salga con su
 * codigo en el paso correcto.
 *
 * @param cuentaId cuenta afectada
 * @param cargo    monto del cargo, o nulo
 * @param abono    monto del abono, o nulo
 * @param concepto texto opcional
 */
public record RenglonManual(UUID cuentaId, BigDecimal cargo, BigDecimal abono, String concepto) {
}
