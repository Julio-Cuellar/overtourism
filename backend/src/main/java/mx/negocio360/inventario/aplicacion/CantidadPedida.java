package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

/** Una cantidad en la unidad que la persona escribio, para llevarla a la unidad base con {@code aBase}. */
@NamedInterface("api")
public record CantidadPedida(UUID articuloId, BigDecimal cantidad, String unidad) {
}
