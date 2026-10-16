package mx.negocio360.ventas.dominio;

/** Formas de pago de una venta (VT-04). Solo el efectivo lleva recibido y cambio. */
public enum MetodoPago {
    /** Dinero fisico; admite recibido y cambio. */
    EFECTIVO,
    /** Cobrado en la terminal del negocio; puede llevar referencia de autorizacion. */
    TARJETA,
    /** Transferencia bancaria; puede llevar referencia. */
    TRANSFERENCIA
}
