package mx.negocio360.contabilidad.dominio;

/** Tipo de poliza. Cada tipo lleva su propia serie de folios por periodo (A-05). */
public enum TipoPoliza {
    /** Entradas de dinero, como una venta cobrada. */
    INGRESO,
    /** Salidas de dinero. */
    EGRESO,
    /** Movimientos sin entrada ni salida de dinero, como un cupon o un faltante de caja. */
    DIARIO
}
