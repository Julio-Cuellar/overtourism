package mx.negocio360.contabilidad.dominio;

/** Estado de una cuenta. Una cuenta nunca se borra: se desactiva (C-03). */
public enum EstadoCuenta {
    /** Puede recibir movimientos si ademas es afectable. */
    ACTIVA,
    /** No recibe movimientos nuevos (A-04). */
    INACTIVA
}
