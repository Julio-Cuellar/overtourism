package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.List;

/**
 * Alta de un articulo (6.3). El minimo se captura en {@code unidadMinimo}, que puede ser una unidad estandar o una de
 * {@code unidadesPropias}; sin unidad se entiende en la unidad base.
 */
public record CrearArticuloComando(String nombre, String categoria, String unidadBase, BigDecimal minimo,
                                   String unidadMinimo, List<UnidadPropiaPedida> unidadesPropias)
        implements Comando {
}
