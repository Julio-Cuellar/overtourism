package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/** Un insumo de una receta para mostrar; el nombre es nulo si el articulo ya no esta en Inventario. */
public record InsumoVista(UUID articuloId, String nombre, BigDecimal cantidad, String unidad, BigDecimal mermaPct) {
}
