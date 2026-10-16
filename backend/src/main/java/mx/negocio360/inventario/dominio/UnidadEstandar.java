package mx.negocio360.inventario.dominio;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Catalogo global de unidades estandar (U-02, tabla {@code unidad}). El factor lleva cada unidad a la mas pequena de
 * su dimension: g para masa, ml para volumen y pieza para conteo.
 */
public enum UnidadEstandar {
    G("g", Dimension.MASA, "1"),
    KG("kg", Dimension.MASA, "1000"),
    ML("ml", Dimension.VOLUMEN, "1"),
    L("l", Dimension.VOLUMEN, "1000"),
    PIEZA("pieza", Dimension.CONTEO, "1");

    private final String codigo;
    private final Dimension dimension;
    private final BigDecimal factorADimension;

    UnidadEstandar(String codigo, Dimension dimension, String factorADimension) {
        this.codigo = codigo;
        this.dimension = dimension;
        this.factorADimension = new BigDecimal(factorADimension);
    }

    public String codigo() {
        return codigo;
    }

    public Dimension dimension() {
        return dimension;
    }

    public BigDecimal factorADimension() {
        return factorADimension;
    }

    /** La unidad con ese codigo, sin importar mayusculas ni espacios de los extremos. */
    public static Optional<UnidadEstandar> deCodigo(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        var buscado = normalizar(texto);
        return Arrays.stream(values()).filter(u -> u.codigo.equals(buscado)).findFirst();
    }

    /** Forma canonica del nombre de una unidad: sin espacios de los extremos y en minusculas. */
    public static String normalizar(String texto) {
        return texto.strip().toLowerCase(Locale.ROOT);
    }
}
