package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

/** Resultado de {@code aBase}: la cantidad en la unidad base del articulo y el factor que se aplico. */
@NamedInterface("api")
public record CantidadEnBase(UUID articuloId, BigDecimal cantidadBase, String unidadBase, BigDecimal factorAplicado) {
}
