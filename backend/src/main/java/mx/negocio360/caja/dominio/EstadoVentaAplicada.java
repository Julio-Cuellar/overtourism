package mx.negocio360.caja.dominio;

/** Que hizo Caja con una venta (CJ-04). */
public enum EstadoVentaAplicada {
    /** Sus pagos entraron al turno. */
    APLICADA,
    /** Se compenso por anulacion, o llego tarde despues de su anulacion. */
    REVERTIDA,
    /** La anulacion llego antes que la venta: la venta tardia no sumara (D-19). */
    ANULADO_PREVIO
}
