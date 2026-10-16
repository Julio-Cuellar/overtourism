package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/** Una cantidad ya convertida a la unidad base de su articulo. */
public record CantidadEnBase(UUID articuloId, BigDecimal cantidadBase, String unidadBase) {
}
