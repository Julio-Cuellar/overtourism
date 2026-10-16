package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/** Una cantidad en la unidad que escribio la persona, para llevarla a la unidad base con {@code aBase}. */
public record LineaDeInsumo(UUID articuloId, BigDecimal cantidad, String unidad) {
}
