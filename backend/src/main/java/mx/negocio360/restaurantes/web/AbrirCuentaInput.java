package mx.negocio360.restaurantes.web;

import java.util.UUID;

/** La entrada GraphQL {@code RestAbrirCuentaInput}: la mesa (o nulo en mostrador) y los comensales. */
public record AbrirCuentaInput(UUID mesaId, int comensales) {
}
