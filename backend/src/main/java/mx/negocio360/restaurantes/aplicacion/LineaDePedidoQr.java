package mx.negocio360.restaurantes.aplicacion;

import java.util.UUID;

/** Una linea de lo que el comensal pide: un platillo, cuantos, una nota de hasta 80 caracteres y la marca de alergia. */
public record LineaDePedidoQr(UUID platilloId, int cantidad, String nota, boolean marcaAlergia) {
}
