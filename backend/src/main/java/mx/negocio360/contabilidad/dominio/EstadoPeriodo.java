package mx.negocio360.contabilidad.dominio;

/** Estado de un periodo contable (T-01). */
public enum EstadoPeriodo {
    /** Recibe polizas. */
    ABIERTO,
    /** No recibe polizas; sus saldos quedan congelados. */
    CERRADO
}
