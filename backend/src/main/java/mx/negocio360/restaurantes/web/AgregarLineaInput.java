package mx.negocio360.restaurantes.web;

import java.util.UUID;

/** La entrada GraphQL {@code RestAgregarLineaInput}: cuenta, platillo, cantidad, nota y marca de alergia. */
public record AgregarLineaInput(UUID cuentaId, UUID platilloId, int cantidad, String nota, boolean marcaAlergia) {
}
