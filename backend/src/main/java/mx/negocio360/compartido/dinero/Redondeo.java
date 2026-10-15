package mx.negocio360.compartido.dinero;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Unica utilidad de redondeo de la plataforma (D-20): modo HALF_UP, dinero a 2 decimales, cantidades y precios
 * unitarios a 4. Ningun calculo de dinero usa {@code double} ni {@code float}.
 */
public final class Redondeo {

    /** Decimales del dinero (totales, importes, pagos). */
    public static final int ESCALA_DINERO = 2;
    /** Decimales de cantidades y precios unitarios. */
    public static final int ESCALA_CANTIDAD = 4;
    /** Modo de redondeo de toda la plataforma. */
    public static final RoundingMode MODO = RoundingMode.HALF_UP;

    private Redondeo() {
    }

    /**
     * Redondea un monto de dinero.
     *
     * @param valor monto, no nulo
     * @return el monto con 2 decimales, HALF_UP
     */
    public static BigDecimal dinero(BigDecimal valor) {
        return Objects.requireNonNull(valor, "valor").setScale(ESCALA_DINERO, MODO);
    }

    /**
     * Redondea una cantidad.
     *
     * @param valor cantidad, no nula
     * @return la cantidad con 4 decimales, HALF_UP
     */
    public static BigDecimal cantidad(BigDecimal valor) {
        return Objects.requireNonNull(valor, "valor").setScale(ESCALA_CANTIDAD, MODO);
    }

    /**
     * Redondea un precio unitario.
     *
     * @param valor precio, no nulo
     * @return el precio con 4 decimales, HALF_UP
     */
    public static BigDecimal precio(BigDecimal valor) {
        return Objects.requireNonNull(valor, "valor").setScale(ESCALA_CANTIDAD, MODO);
    }

    /**
     * Escribe una cantidad como decimal plano sin ceros sobrantes, como viaja en los eventos ({@code "2"},
     * {@code "0.5"}).
     *
     * @param valor cantidad, no nula
     * @return texto plano, sin notacion cientifica
     */
    public static String cantidadComoTexto(BigDecimal valor) {
        var limpio = Objects.requireNonNull(valor, "valor").stripTrailingZeros();
        return DecimalPlano.escribir(limpio.scale() < 0 ? limpio.setScale(0, MODO) : limpio);
    }
}
