package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;

/** Unidad propia que se pide al crear un articulo: su nombre y cuantas unidades base vale (U-03). */
public record UnidadPropiaPedida(String nombre, BigDecimal factor) {
}
