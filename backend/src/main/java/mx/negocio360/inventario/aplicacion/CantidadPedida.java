package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/** Una cantidad en la unidad que la persona escribio, para llevarla a la unidad base con {@code aBase}. */
public record CantidadPedida(UUID articuloId, BigDecimal cantidad, String unidad) {
}
