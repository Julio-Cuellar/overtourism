package mx.negocio360.restaurantes.web;

import java.util.List;

/**
 * La entrada GraphQL {@code RestPedirPorQrInput}: el token del QR, el identificador del dispositivo (el servidor guarda solo
 * su hash, QR-12) y las lineas del pedido.
 */
public record PedirPorQrInput(String token, String dispositivoId, List<PedidoPorQrLineaInput> lineas) {
}
