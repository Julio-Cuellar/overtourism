package mx.negocio360.caja.dominio;

/** Tipos de movimiento del libro de caja. El signo del monto dice si entra o sale. */
public enum TipoMovimiento {
    /** Fondo inicial del turno (CJ-02). */
    FONDO,
    /** Pago de una venta registrada (CJ-03). */
    VENTA,
    /** Compensacion de una venta anulada, en negativo (CJ-03). */
    ANULACION,
    /** Efectivo que entra a mano. */
    ENTRADA,
    /** Efectivo que sale a mano, en negativo (CJ-05). */
    RETIRO,
    /** Correccion posterior a un corte (CJ-09, CJ-11; P2). */
    AJUSTE
}
