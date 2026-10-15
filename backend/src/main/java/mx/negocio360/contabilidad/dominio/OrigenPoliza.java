package mx.negocio360.contabilidad.dominio;

/** De donde salio una poliza. */
public enum OrigenPoliza {
    /** La genero un evento de otro modulo (venta, cupon o corte). */
    AUTOMATICA,
    /** La capturo una persona con {@code RegistrarPolizaManual} o al resolver un pendiente. */
    MANUAL,
    /** Invierte otra poliza (A-06). */
    REVERSA
}
