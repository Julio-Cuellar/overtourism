package mx.negocio360.restaurantes.dominio;

import java.math.BigDecimal;
import java.util.UUID;
import mx.negocio360.compartido.dinero.Redondeo;

/**
 * Un insumo de una receta (RC-02): un articulo de Inventario con la cantidad que lleva el platillo, en la unidad que escribio
 * la persona, y el desperdicio sobre lo neto (D-167: lo que se consume es cantidad × (1 + merma/100)).
 *
 * @param articuloId id opaco del articulo en Inventario (R1: sin llave foranea)
 * @param cantidad   mayor que 0, con 4 decimales
 * @param unidad     de 1 a 20 caracteres; Inventario valida que se convierta a la unidad base del articulo
 * @param mermaPct   de 0 a 100, con 2 decimales; nulo es 0
 */
public record InsumoDeReceta(UUID articuloId, BigDecimal cantidad, String unidad, BigDecimal mermaPct) {

    private static final int MAX_UNIDAD = 20;
    private static final BigDecimal CIEN = new BigDecimal("100");

    public InsumoDeReceta {
        if (articuloId == null || cantidad == null) {
            throw new RestaurantesException(CodigoError.RECETA_INVALIDA);
        }
        cantidad = Redondeo.cantidad(cantidad);
        if (cantidad.signum() <= 0) {
            throw new RestaurantesException(CodigoError.RECETA_INVALIDA);
        }
        unidad = unidad == null ? "" : unidad.strip();
        if (unidad.isEmpty() || unidad.length() > MAX_UNIDAD) {
            throw new RestaurantesException(CodigoError.RECETA_INVALIDA);
        }
        mermaPct = (mermaPct == null ? BigDecimal.ZERO : mermaPct).setScale(2, Redondeo.MODO);
        if (mermaPct.signum() < 0 || mermaPct.compareTo(CIEN) > 0) {
            throw new RestaurantesException(CodigoError.RECETA_INVALIDA);
        }
    }
}
