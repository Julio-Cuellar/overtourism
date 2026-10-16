package mx.negocio360.restaurantes.aplicacion;

import java.util.UUID;

/**
 * Filtros de la lista de platillos. Todos son opcionales y se combinan con «y».
 *
 * @param texto                  parte del nombre, sin distinguir mayusculas
 * @param categoriaId            solo esa categoria
 * @param soloAgotados           solo los agotados
 * @param soloFueraDeTemporada   solo los que tienen temporada y hoy no estan en ella
 * @param soloSinReceta          solo los que no tienen receta activa
 */
public record FiltroPlatillos(String texto, UUID categoriaId, boolean soloAgotados, boolean soloFueraDeTemporada,
                              boolean soloSinReceta) {

    /** Sin filtros. */
    public static final FiltroPlatillos TODOS = new FiltroPlatillos(null, null, false, false, false);
}
