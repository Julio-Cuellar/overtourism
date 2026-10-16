package mx.negocio360.ventas.dominio;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

/**
 * Renglon de una venta, con precio, descripcion y unidad copiados de Catalogo al vender (VT-02). Es inmutable.
 *
 * @param linea          numero de renglon, desde 1
 * @param productoId     referencia opaca a Catalogo
 * @param descripcion    copia de la descripcion al vender
 * @param cantidad       escala 4, HALF_UP
 * @param unidad         unidad de Catalogo (pieza, kg, l...)
 * @param precioUnitario escala 4, HALF_UP
 * @param importe        {@code cantidad × precioUnitario}, escala 2, HALF_UP
 * @param serie          numero de serie del articulo, o nulo
 */
public record LineaVenta(int linea, UUID productoId, String descripcion, BigDecimal cantidad, String unidad,
                         BigDecimal precioUnitario, BigDecimal importe, String serie) {

    /** Exige todos los datos salvo la serie, y numero de linea desde 1. */
    public LineaVenta {
        if (linea < 1) {
            throw new IllegalArgumentException("La linea empieza en 1");
        }
        Objects.requireNonNull(productoId, "productoId");
        Objects.requireNonNull(descripcion, "descripcion");
        Objects.requireNonNull(cantidad, "cantidad");
        Objects.requireNonNull(unidad, "unidad");
        Objects.requireNonNull(precioUnitario, "precioUnitario");
        Objects.requireNonNull(importe, "importe");
    }
}
