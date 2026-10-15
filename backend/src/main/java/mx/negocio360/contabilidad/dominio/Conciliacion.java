package mx.negocio360.contabilidad.dominio;

/** Resultado de conciliar un corte de caja (estados ES3 del diseno). */
public enum Conciliacion {
    /** Contado igual al esperado y, si Caja las reporta, ventas en efectivo iguales. */
    CUADRA,
    /** El efectivo contado no es el esperado; las ventas coinciden o Caja no las reporta. */
    DIFERENCIA_CAJA,
    /** Las ventas en efectivo contabilizadas no coinciden con las que reporta Caja (J-03). */
    DESCUADRE_VENTAS
}
