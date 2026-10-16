package mx.negocio360.inventario.dominio;

import java.math.BigDecimal;

/**
 * Rangos de las columnas del esquema (5): cantidades y costos {@code NUMERIC(14,4)} y factores
 * {@code NUMERIC(18,8)}. Validar aqui evita que un valor llegue a la base y falle alla con un error tecnico.
 */
public final class Limites {

    /** Mayor valor absoluto de una cantidad o costo con 4 decimales: 10 digitos enteros. */
    public static final BigDecimal MAX_CANTIDAD = new BigDecimal("9999999999.9999");
    /** Mayor factor de conversion con 8 decimales: 10 digitos enteros. */
    public static final BigDecimal MAX_FACTOR = new BigDecimal("9999999999.99999999");
    /** Mayor valor absoluto de dinero con 2 decimales: {@code NUMERIC(14,2)}. */
    public static final BigDecimal MAX_DINERO = new BigDecimal("999999999999.99");
    public static final int MAX_DECIMALES_CAPTURA = 4;
    public static final int MAX_DECIMALES_FACTOR = 8;

    private Limites() {
    }

    /** Comparar magnitudes es barato aunque el exponente sea enorme; nunca se opera antes de comprobar el rango. */
    public static boolean cantidadEnRango(BigDecimal valor) {
        return valor.compareTo(MAX_CANTIDAD) <= 0 && valor.compareTo(MAX_CANTIDAD.negate()) >= 0;
    }

    public static boolean dineroEnRango(BigDecimal valor) {
        return valor.compareTo(MAX_DINERO) <= 0 && valor.compareTo(MAX_DINERO.negate()) >= 0;
    }
}
