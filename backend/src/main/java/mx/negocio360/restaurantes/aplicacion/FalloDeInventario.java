package mx.negocio360.restaurantes.aplicacion;

import java.util.Objects;

/**
 * Un fallo de Inventario: su codigo (los de la seccion 6.5 de inventario.md) y si vale la pena reintentar. Los
 * reintentables son {@code CONCURRENCIA} y {@code ERROR_TEMPORAL}; los demas son permanentes (K-02).
 */
public final class FalloDeInventario extends RuntimeException {

    private final String codigo;
    private final boolean reintentable;

    /**
     * @param codigo       codigo de Inventario, por ejemplo {@code ARTICULO_INACTIVO}
     * @param reintentable verdadero si el mismo intento puede salir bien mas tarde
     */
    public FalloDeInventario(String codigo, boolean reintentable) {
        super(Objects.requireNonNull(codigo, "codigo"));
        this.codigo = codigo;
        this.reintentable = reintentable;
    }

    /** Codigo de Inventario. */
    public String codigo() {
        return codigo;
    }

    /** Verdadero si conviene volver a intentar. */
    public boolean reintentable() {
        return reintentable;
    }
}
