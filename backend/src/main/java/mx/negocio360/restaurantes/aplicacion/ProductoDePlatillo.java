package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lo que Ventas necesita de un platillo para venderlo. Es la misma forma que {@code ProductoParaVenta} de la plataforma
 * (docs/planes/integracion-ventas-caja.md §9.2); cuando {@code compartido.catalogo} exista, este tipo se reemplaza por ese.
 *
 * @param productoId      id del platillo
 * @param descripcion     nombre que se copia a la linea de venta
 * @param unidad          siempre {@code pieza} (D-162)
 * @param precio          precio vigente al publico, con dos decimales
 * @param admiteFraccion  siempre falso
 * @param activo          falso solo tras la baja logica (MN-14)
 */
public record ProductoDePlatillo(UUID productoId, String descripcion, String unidad, BigDecimal precio,
                                 boolean admiteFraccion, boolean activo) {
}
