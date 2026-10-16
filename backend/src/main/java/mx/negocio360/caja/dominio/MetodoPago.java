package mx.negocio360.caja.dominio;

/**
 * Metodos de pago que Caja lleva por separado. Es un tipo propio de Caja: llega como texto en los eventos de Ventas y
 * Caja no importa el dominio de Ventas (R1).
 */
public enum MetodoPago {
    /** Dinero fisico: el unico que se cuenta en el arqueo y que admite entradas y retiros. */
    EFECTIVO,
    /** Cobrado en la terminal del negocio. */
    TARJETA,
    /** Transferencia bancaria. */
    TRANSFERENCIA
}
