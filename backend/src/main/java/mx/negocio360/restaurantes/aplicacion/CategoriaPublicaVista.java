package mx.negocio360.restaurantes.aplicacion;

import java.util.List;
import java.util.UUID;

/** Una categoria con sus platillos publicados en el menu publico. */
public record CategoriaPublicaVista(UUID id, String nombre, List<PlatilloPublicoVista> platillos) {
}
