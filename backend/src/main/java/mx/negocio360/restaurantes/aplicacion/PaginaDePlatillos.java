package mx.negocio360.restaurantes.aplicacion;

import java.util.List;

/**
 * Una pagina de platillos.
 *
 * @param nodos     platillos de la pagina, por nombre
 * @param siguiente cursor para pedir la siguiente pagina, o nulo si no hay mas
 */
public record PaginaDePlatillos(List<PlatilloVista> nodos, String siguiente) {
}
