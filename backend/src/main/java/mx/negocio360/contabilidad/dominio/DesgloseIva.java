package mx.negocio360.contabilidad.dominio;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Desglose de un monto con IVA incluido en la parte de ventas y la de IVA (A-07). El IVA se calcula sobre la base
 * redondeada y la diferencia de redondeo queda en ventas, para que {@code ventas + iva} sea siempre el total y la
 * poliza cuadre.
 *
 * @param ventas parte que va a la cuenta de ventas
 * @param iva    parte que va a la cuenta de IVA trasladado
 */
public record DesgloseIva(BigDecimal ventas, BigDecimal iva) {

    private static final int ESCALA_TASA = 4;
    private static final int ESCALA_INTERMEDIA = 10;

    /**
     * Desglosa un total con IVA incluido.
     *
     * @param total total cobrado, en centavos
     * @param tasa  tasa de IVA entre 0 (incluido) y 1 (excluido), con hasta 4 decimales; nula si es exento
     * @return el desglose; con tasa nula o cero, todo es ventas
     * @throws ContabilidadException {@code TASA_INVALIDA} si la tasa esta fuera de rango o tiene mas de 4 decimales;
     *                               {@code MONTO_INVALIDO} si el total no es un monto valido
     */
    public static DesgloseIva de(BigDecimal total, BigDecimal tasa) {
        var exacto = Montos.exacto(total);
        validarTasa(tasa);
        if (tasa == null || tasa.signum() == 0) {
            return new DesgloseIva(exacto, Montos.exacto(BigDecimal.ZERO));
        }
        var base = Montos.redondear(exacto.divide(BigDecimal.ONE.add(tasa), ESCALA_INTERMEDIA, RoundingMode.HALF_UP));
        var iva = Montos.redondear(base.multiply(tasa));
        return new DesgloseIva(exacto.subtract(iva), iva);
    }

    /**
     * Valida una tasa de IVA.
     *
     * @param tasa tasa a validar; nula significa exento y es valida
     * @throws ContabilidadException {@code TASA_INVALIDA} si es negativa, de 1 o mas, o tiene mas de 4 decimales
     */
    public static void validarTasa(BigDecimal tasa) {
        if (tasa != null && (tasa.signum() < 0 || tasa.compareTo(BigDecimal.ONE) >= 0
                || tasa.stripTrailingZeros().scale() > ESCALA_TASA)) {
            throw new ContabilidadException(CodigoError.TASA_INVALIDA);
        }
    }
}
