package mx.negocio360.restaurantes.web;

import java.util.UUID;
import mx.negocio360.restaurantes.aplicacion.LineaDePedidoQr;

/** La entrada GraphQL {@code RestPedidoPorQrLineaInput}: un platillo, cuantos, una nota y la marca de alergia. */
public record PedidoPorQrLineaInput(UUID platilloId, int cantidad, String nota, boolean marcaAlergia) {

    /** La linea como la entiende el servicio. */
    LineaDePedidoQr aLinea() {
        return new LineaDePedidoQr(platilloId, cantidad, nota, marcaAlergia);
    }
}
