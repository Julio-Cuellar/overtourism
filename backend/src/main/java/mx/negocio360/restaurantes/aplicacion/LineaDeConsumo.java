package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/** Una linea de {@code registrarConsumo}: cuanto de un articulo se gasto, en su unidad base. */
public record LineaDeConsumo(UUID articuloId, BigDecimal cantidad, String unidad) {
}
