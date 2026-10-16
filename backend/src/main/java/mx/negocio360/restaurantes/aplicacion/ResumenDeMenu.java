package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;

/**
 * Conteos del menu para el panel, entre los platillos activos. La cobertura de recetas es el porcentaje de platillos con
 * receta activa, con dos decimales; sin platillos es 0.
 */
public record ResumenDeMenu(int total, int agotados, int fueraDeTemporada, int conReceta, int sinReceta,
                            BigDecimal coberturaDeRecetasPct) {
}
