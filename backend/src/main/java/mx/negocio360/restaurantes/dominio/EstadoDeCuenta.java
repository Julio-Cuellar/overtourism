package mx.negocio360.restaurantes.dominio;

/**
 * Los estados de una cuenta (seccion 6.3 del diseno). {@code ABIERTA} y {@code POR_COBRAR} ocupan la mesa; {@code PAGADA} y
 * {@code CANCELADA} son finales y la liberan.
 */
public enum EstadoDeCuenta {
    ABIERTA,
    POR_COBRAR,
    PAGADA,
    CANCELADA;

    /** Verdadero si la cuenta todavia no se paga ni se cancela: la mesa sigue ocupada (MS-02, MS-03). */
    public boolean noPagada() {
        return this == ABIERTA || this == POR_COBRAR;
    }
}
