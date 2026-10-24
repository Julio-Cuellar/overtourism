package mx.negocio360.restaurantes.dominio;

/** Quien abrio la cuenta: el personal con {@code abrirCuenta} o el primer pedido por QR (CT-01, QR-05). */
public enum OrigenDeCuenta {
    PERSONAL,
    QR
}
