package mx.negocio360.restaurantes.web;

import java.util.UUID;

/** La entrada GraphQL {@code RestMesaInput}: zona, etiqueta y capacidad de una mesa. */
public record MesaInput(UUID zonaId, String etiqueta, int capacidad) {
}
