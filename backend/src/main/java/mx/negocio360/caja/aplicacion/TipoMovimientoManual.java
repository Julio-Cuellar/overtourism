package mx.negocio360.caja.aplicacion;

/** Movimientos que una persona puede registrar a mano (CF-11): siempre en efectivo. */
public enum TipoMovimientoManual {
    /** Meter efectivo, por ejemplo cambio del banco. */
    ENTRADA,
    /** Sacar efectivo, por ejemplo para depositar (CJ-05). */
    RETIRO
}
