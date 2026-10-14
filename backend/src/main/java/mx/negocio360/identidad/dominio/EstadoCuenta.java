package mx.negocio360.identidad.dominio;

/** Estado de una cuenta. Solo una cuenta ACTIVA inicia sesion y autentica. */
public enum EstadoCuenta {
    ACTIVA,
    SUSPENDIDA,
    ANONIMIZADA
}
