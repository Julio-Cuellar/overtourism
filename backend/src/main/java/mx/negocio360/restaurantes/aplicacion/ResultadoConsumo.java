package mx.negocio360.restaurantes.aplicacion;

/**
 * Lo que hizo {@link ConsumidorDeVentas} con un evento (CM-01 a CM-06). El adaptador del bus usa {@link #reintentable()}: si
 * es verdadero, el evento <b>no</b> se da por entregado y vuelve a llegar sin perderse (D-122).
 */
public enum ResultadoConsumo {
    /** Los insumos de la venta se descontaron. */
    APLICADO,
    /** Inventario ya tenia ese consumo (la misma venta en otro evento). */
    YA_APLICADA,
    /** Ese mismo evento ya se habia procesado (R3). */
    YA_PROCESADO,
    /** Se descontaron los platillos con receta; a otros les falto (se guardaron como consumo fallido). */
    APLICADA_CON_FALLOS,
    /** La anulacion devolvio los insumos. */
    REVERTIDA,
    /** La venta ya estaba revertida. */
    YA_REVERTIDA,
    /** La anulacion llego antes que la venta: queda la marca en Inventario. */
    MARCADA_ANULADO_PREVIO,
    /** La venta llego despues de su anulacion: no descuenta nada. */
    OMITIDA_ANULADO_PREVIO,
    /** Error permanente: se guardo como consumo fallido y no se reintenta. */
    FALLIDA,
    /** Inventario no respondio: el evento se reintentara. */
    REINTENTABLE,
    /** No era para Restaurantes: no trae platillos de receta de este negocio. */
    IGNORADO,
    /** No cumple el contrato v1 o no lo publico Ventas. */
    DATOS_INVALIDOS;

    /** Verdadero si el evento debe volver a entregarse. */
    public boolean reintentable() {
        return this == REINTENTABLE;
    }
}
