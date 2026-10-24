package mx.negocio360.restaurantes.aplicacion;

import java.util.UUID;

/** Una linea que el comensal pidio desde su dispositivo, con su estado y, si se cancelo, el mensaje fijo del motivo (QR-10, QR-11). */
public record LineaDePedidoQrVista(UUID id, String platillo, int cantidad, EstadoDeLineaQr estado, String mensaje) {
}
