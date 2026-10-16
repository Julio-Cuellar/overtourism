package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Un insumo tal como lo escribe la persona al guardar una receta.
 *
 * @param articuloId articulo de Inventario
 * @param cantidad   cantidad para el rendimiento de la receta
 * @param unidad     unidad en la que la escribio
 * @param mermaPct   desperdicio sobre lo neto, de 0 a 100
 */
public record InsumoComando(UUID articuloId, BigDecimal cantidad, String unidad, BigDecimal mermaPct) {
}
