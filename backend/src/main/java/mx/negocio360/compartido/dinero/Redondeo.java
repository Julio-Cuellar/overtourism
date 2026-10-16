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

    /** Mayor monto que cabe en {@code NUMERIC(14, 2)}, la columna de dinero de la base. */
    public static final BigDecimal MAXIMO_DINERO = new BigDecimal("999999999999.99");

    private Redondeo() {
    }

    /**
     * Dice si un monto que llega de fuera es dinero valido: no nulo, con valor absoluto hasta {@link #MAXIMO_DINERO} y
     * a lo mas 2 decimales. El signo lo revisa quien llama. Solo compara: no redondea, asi que un valor como
     * {@code 1E+999999999} se rechaza al instante en lugar de expandirse en memoria (revision M1).
     *
     * @param valor monto recibido, puede ser nulo
     * @return verdadero si se puede redondear y guardar sin riesgo
     */
    public static boolean dineroValido(BigDecimal valor) {
        return valor != null && valor.abs().compareTo(MAXIMO_DINERO) <= 0
                && valor.stripTrailingZeros().scale() <= ESCALA_DINERO;
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
