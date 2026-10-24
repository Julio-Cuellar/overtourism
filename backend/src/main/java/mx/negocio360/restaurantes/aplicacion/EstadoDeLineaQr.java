package mx.negocio360.restaurantes.aplicacion;

/** Como va una linea pedida por QR, vista por el comensal (QR-11): sigue a su comanda, o queda cancelada. */
public enum EstadoDeLineaQr {
    EN_COCINA,
    PREPARANDO,
    LISTO,
    ENTREGADO,
    CANCELADO
}
