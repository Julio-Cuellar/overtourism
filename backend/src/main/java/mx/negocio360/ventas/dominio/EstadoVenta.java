package mx.negocio360.ventas.dominio;

/** Ciclo de vida de una venta: nace REGISTRADA al confirmar y solo puede pasar a ANULADA (VT-11). */
public enum EstadoVenta {
    /** Venta cobrada y vigente. */
    REGISTRADA,
    /** Venta anulada con motivo y permiso (VT-09); conserva sus lineas, pagos y montos. */
    ANULADA
}
