package mx.negocio360.restaurantes.aplicacion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * El menu que ve el turista (PB-01 a PB-06).
 *
 * @param negocioId     negocio consultado
 * @param etiqueta      como se llama la pestaña de la ficha: «Menú»
 * @param idioma        idioma en el que viene el contenido
 * @param categorias    categorias con al menos un platillo, en el orden del menu
 * @param sinCategoria  platillos sin categoria
 * @param actualizadoEn ultimo cambio de lo publicado
 * @param aviso         leyenda de precios y disponibilidad
 */
public record MenuPublicoVista(UUID negocioId, String etiqueta, String idioma, List<CategoriaPublicaVista> categorias,
                               List<PlatilloPublicoVista> sinCategoria, Instant actualizadoEn, String aviso) {
}
