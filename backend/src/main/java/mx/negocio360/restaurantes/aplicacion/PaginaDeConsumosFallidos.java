package mx.negocio360.restaurantes.aplicacion;

import java.util.List;

/**
 * Una pagina de consumos fallidos, del mas nuevo al mas viejo.
 *
 * @param nodos     fallos de la pagina
 * @param siguiente cursor de la siguiente pagina, o nulo si no hay mas
 */
public record PaginaDeConsumosFallidos(List<ConsumoFallido> nodos, String siguiente) {
}
