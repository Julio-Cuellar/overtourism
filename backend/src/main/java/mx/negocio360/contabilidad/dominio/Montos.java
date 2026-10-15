package mx.negocio360.contabilidad.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Reglas del dinero en Contabilidad: centavos exactos, redondeo HALF_UP (D-20) y el rango de {@code numeric(14,2)}.
 * Nunca se usa punto flotante (SK-05).
 */
public final class Montos {

    /** Escala del dinero: dos decimales. */
    public static final int ESCALA = 2;

    private static final BigDecimal MAXIMO = new BigDecimal("999999999999.99");

    private Montos() {
    }

    /**
     * Valida un monto capturado y lo deja en centavos. No redondea: un monto con mas de dos decimales es un error.
     *
     * @param monto monto a validar; nulo cuenta como cero
     * @return el monto con escala 2
     * @throws ContabilidadException {@code MONTO_INVALIDO} si tiene mas de dos decimales o no cabe en
     *                               {@code numeric(14,2)}
     */
    public static BigDecimal exacto(BigDecimal monto) {
        if (monto == null) {
            return BigDecimal.ZERO.setScale(ESCALA);
        }
        if (monto.stripTrailingZeros().scale() > ESCALA || monto.abs().compareTo(MAXIMO) > 0) {
            throw new ContabilidadException(CodigoError.MONTO_INVALIDO);
        }
        return monto.setScale(ESCALA, RoundingMode.UNNECESSARY);
    }

    /**
     * Redondea un resultado de calculo a centavos con HALF_UP (D-20).
     *
     * @param valor valor calculado, no nulo
     * @return el valor con escala 2
     */
    public static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }
}
