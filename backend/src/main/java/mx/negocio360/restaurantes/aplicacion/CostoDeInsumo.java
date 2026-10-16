package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Cuanto cuesta un insumo en la receta completa (para su rendimiento, antes de repartirlo entre porciones).
 *
 * @param cantidadBase cantidad en la unidad base del articulo, sin merma; nula si no se pudo convertir
 * @param cpp          costo promedio ponderado por unidad base; nulo si no tiene
 * @param costo        {@code cantidadBase × (1 + merma/100) × cpp}, con 4 decimales; nulo si falta algun dato
 */
public record CostoDeInsumo(UUID articuloId, String nombre, BigDecimal cantidadBase, String unidadBase,
                            BigDecimal mermaPct, BigDecimal cpp, BigDecimal costo) {
}
