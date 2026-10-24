package mx.negocio360.restaurantes.aplicacion;

/** El resultado de un pedido por QR: si abrio la cuenta de la mesa y como queda el pedido del dispositivo. */
public record ResultadoPedidoQrVista(boolean abrioLaCuenta, EstadoDePedidoQrVista estado) {
}
