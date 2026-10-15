package mx.negocio360.contabilidad.dominio;

/** Naturaleza de una cuenta: de que lado aumenta su saldo. Una hija tiene la de su padre (C-02). */
public enum Naturaleza {
    /** El saldo aumenta con cargos. */
    DEUDORA,
    /** El saldo aumenta con abonos. */
    ACREEDORA
}
