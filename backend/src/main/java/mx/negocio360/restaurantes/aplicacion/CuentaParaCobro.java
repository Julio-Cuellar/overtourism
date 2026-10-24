package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Lo que la caja necesita para cobrar una cuenta (CT-07): las lineas <b>agregadas por platillo</b>, el total calculado y
 * la referencia opaca que se manda a Ventas. Es de solo lectura: Restaurantes no escribe la venta.
 */
public record CuentaParaCobro(UUID cuentaId, String referencia, BigDecimal total, List<LineaDeCobro> lineas) {

    /** Un platillo con la cantidad total que se pidio de el. */
    public record LineaDeCobro(UUID productoId, String descripcion, int cantidad) {
    }
}
