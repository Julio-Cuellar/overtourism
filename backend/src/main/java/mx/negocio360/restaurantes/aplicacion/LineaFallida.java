package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/** Un platillo vendido cuyo consumo no se pudo descontar, con la cantidad vendida. Sirve para el reintento (CM-09, P2). */
public record LineaFallida(UUID platilloId, BigDecimal cantidad) {
}
