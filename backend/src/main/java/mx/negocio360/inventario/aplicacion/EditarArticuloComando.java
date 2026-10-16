package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Edicion de un articulo (6.3). Lo que viene nulo no cambia; una categoria en blanco la borra. La unidad base no se
 * puede cambiar (U-01).
 */
public record EditarArticuloComando(UUID articuloId, String nombre, String categoria, BigDecimal minimo,
                                    String unidadMinimo) implements Comando {
}
