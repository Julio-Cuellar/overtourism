package mx.negocio360.inventario.dominio;

import org.springframework.modulith.NamedInterface;

/**
 * Como se rastrea un articulo (L-01, D-27): sin rastreo, por lote o por numero de serie. Son excluyentes. Hoy solo se
 * pueden crear articulos {@code NINGUNO}: lotes y series llegan con su tramo, pero el valor ya viaja en la API para que
 * quien consume insumos (Restaurantes, S-05) pueda rechazar un articulo de serie.
 */
@NamedInterface("api")
public enum Rastreo {
    NINGUNO,
    LOTE,
    SERIE
}
