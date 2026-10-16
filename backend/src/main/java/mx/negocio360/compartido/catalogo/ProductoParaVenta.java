package mx.negocio360.compartido.catalogo;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Lo que Ventas necesita de un producto para venderlo (J1 del diseno, D-128). Lo entrega cualquier {@link FuenteDeProductos}:
 * Catalogo, o un vertical (Miscelaneos, Restaurantes). Lo propio del giro (boleto de
 * museo, venta a granel del miscelaneo) llega como datos: unidad y si admite fraccion (D-139).
 *
 * @param productoId     id del producto en Catalogo
 * @param descripcion    nombre que se copia a la linea
 * @param unidad         pieza, kg, l...
 * @param precio         precio vigente al publico (VT-12: precio final, sin desglose de impuestos)
 * @param admiteFraccion verdadero si se puede vender 0.5 (VT-03)
 * @param activo         falso si ya no se vende
 */
public record ProductoParaVenta(UUID productoId, String descripcion, String unidad, BigDecimal precio,
                                boolean admiteFraccion, boolean activo) {
}
