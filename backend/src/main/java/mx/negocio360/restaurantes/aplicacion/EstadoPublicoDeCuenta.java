package mx.negocio360.restaurantes.aplicacion;

/** Lo unico que el comensal sabe de la cuenta de su mesa (QR-03): si no hay, si esta abierta o si ya se pidio. */
public enum EstadoPublicoDeCuenta {
    SIN_CUENTA,
    ABIERTA,
    SOLICITADA
}
