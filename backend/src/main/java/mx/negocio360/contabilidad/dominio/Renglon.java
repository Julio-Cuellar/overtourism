package mx.negocio360.contabilidad.dominio;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Renglon de una poliza: una cuenta con un cargo o un abono (tabla {@code movimiento}). Exactamente uno de los dos es
 * mayor que cero (A-02); los montos quedan en centavos.
 *
 * @param cuentaId cuenta afectada; obligatoria
 * @param cargo    monto del cargo; nulo cuenta como cero
 * @param abono    monto del abono; nulo cuenta como cero
 * @param concepto texto opcional del renglon
 * @throws ContabilidadException {@code MOVIMIENTO_INVALIDO} si falta la cuenta, si hay cargo y abono a la vez, si no
 *                               hay ninguno o si alguno es negativo; {@code MONTO_INVALIDO} si un monto tiene mas de
 *                               dos decimales o no cabe en {@code numeric(14,2)}
 */
public record Renglon(UUID cuentaId, BigDecimal cargo, BigDecimal abono, String concepto) {

    public Renglon {
        if (cuentaId == null) {
            throw new ContabilidadException(CodigoError.MOVIMIENTO_INVALIDO);
        }
        cargo = Montos.exacto(cargo);
        abono = Montos.exacto(abono);
        var hayCargo = cargo.signum() > 0;
        var hayAbono = abono.signum() > 0;
        if (cargo.signum() < 0 || abono.signum() < 0 || hayCargo == hayAbono) {
            throw new ContabilidadException(CodigoError.MOVIMIENTO_INVALIDO);
        }
    }
}
