package mx.negocio360.contabilidad.dominio;

/** Estado de un pendiente contable (E-04). */
public enum EstadoPendiente {
    /** Espera a una persona. */
    ABIERTO,
    /** Se resolvio con una poliza. */
    CONTABILIZADO,
    /** Se descarto con un motivo. */
    DESCARTADO
}
