package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import mx.negocio360.inventario.dominio.Rastreo;

/**
 * Alta de un articulo (6.3). El minimo se captura en {@code unidadMinimo}, que puede ser una unidad estandar o una de
 * {@code unidadesPropias}; sin unidad se entiende en la unidad base. Sin {@code rastreo} el articulo no se rastrea; hoy
 * {@code LOTE} y {@code SERIE} se rechazan con {@code RASTREO_NO_SOPORTADO}.
 */
public record CrearArticuloComando(String nombre, String categoria, String unidadBase, BigDecimal minimo,
                                   String unidadMinimo, List<UnidadPropiaPedida> unidadesPropias,
                                   Rastreo rastreo)
        implements Comando {

    /** Alta sin rastreo: el articulo queda en {@code NINGUNO}. */
    public CrearArticuloComando(String nombre, String categoria, String unidadBase, BigDecimal minimo,
                                String unidadMinimo, List<UnidadPropiaPedida> unidadesPropias) {
        this(nombre, categoria, unidadBase, minimo, unidadMinimo, unidadesPropias, null);
    }
}
