package mx.negocio360.contabilidad.dominio;

/** Hecho que dispara una regla de contabilizacion (tabla {@code regla_contable}). */
public enum Disparador {
    /** {@code venta.registrada}; la condicion es el medio de pago. */
    VENTA,
    /** {@code cupon.canjeado}; la condicion es quien paga el descuento. */
    CUPON,
    /** {@code caja.cerrada} con menos efectivo del esperado. */
    CORTE_FALTANTE,
    /** {@code caja.cerrada} con mas efectivo del esperado. */
    CORTE_SOBRANTE,
    /** Retiro de efectivo para deposito (P2, J-05). */
    CORTE_RETIRO
}
