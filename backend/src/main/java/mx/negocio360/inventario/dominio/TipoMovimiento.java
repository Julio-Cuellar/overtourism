package mx.negocio360.inventario.dominio;

import org.springframework.modulith.NamedInterface;

/** Tipo de movimiento (columna {@code movimiento.tipo}). */
@NamedInterface("api")
public enum TipoMovimiento {
    ENTRADA,
    SALIDA,
    MERMA,
    AJUSTE_POS,
    AJUSTE_NEG;

    /** Verdadero si el movimiento suma al saldo. */
    public boolean entra() {
        return this == ENTRADA || this == AJUSTE_POS;
    }
}
