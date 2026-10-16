package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * El costo teorico de un platillo (RC-05, RC-06). Las cifras las calcula el codigo. Si a algun insumo le falta el costo, el
 * resultado es {@code incompleto}: no hay costo, margen ni porcentaje y se listan los insumos sin costo; nunca se estima.
 *
 * @param costoPorPorcion costo de una porcion, con dos decimales; nulo si es incompleto
 * @param margen          precio menos costo; nulo si es incompleto
 * @param margenPct       margen entre precio, en porcentaje con dos decimales; nulo si es incompleto
 */
public record CostoDeReceta(UUID platilloId, int version, int rendimiento, BigDecimal precio, BigDecimal costoPorPorcion,
                            BigDecimal margen, BigDecimal margenPct, boolean incompleto, List<UUID> insumosSinCosto,
                            List<CostoDeInsumo> insumos) {

    public CostoDeReceta {
        insumosSinCosto = List.copyOf(insumosSinCosto);
        insumos = List.copyOf(insumos);
    }
}
