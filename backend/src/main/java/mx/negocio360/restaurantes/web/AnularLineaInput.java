package mx.negocio360.restaurantes.web;

import java.util.UUID;
import mx.negocio360.restaurantes.dominio.MotivoDeAnulacion;

/** La entrada GraphQL {@code RestAnularLineaInput}: la linea, el motivo del catalogo y el texto de OTRO. */
public record AnularLineaInput(UUID lineaId, MotivoDeAnulacion motivo, String texto) {
}
