package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lo que Restaurantes necesita saber de un articulo de Inventario para armar y costear una receta.
 *
 * @param id         id del articulo
 * @param nombre     nombre para mostrar
 * @param unidadBase unidad en la que Inventario lo lleva
 * @param cpp        costo promedio ponderado por unidad base; nulo o 0 si todavia no tiene costo
 * @param activo     falso si lo dieron de baja
 * @param rastreo    como lo rastrea Inventario
 */
public record ArticuloDeInventario(UUID id, String nombre, String unidadBase, BigDecimal cpp, boolean activo,
                                   Rastreo rastreo) {
}
