package mx.negocio360.inventario.eventos;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.dinero.DecimalPlano;

/**
 * {@code inventario.movimiento_registrado} v1: se confirmo un movimiento. Lo consume Contabilidad para el costo de
 * ventas. Las cifras van como cadena decimal; {@code causa_id} se omite cuando el movimiento es manual.
 */
public record MovimientoRegistrado(UUID movimientoId, UUID articuloId, String tipo, BigDecimal cantidadBase,
                                   String unidadBase, BigDecimal costoUnitario, BigDecimal costoTotal,
                                   BigDecimal cppResultante, BigDecimal saldoResultante, String causaTipo,
                                   UUID causaId) {

    public static final String TIPO = "inventario.movimiento_registrado";
    public static final int VERSION = 1;

    public MovimientoRegistrado {
        Objects.requireNonNull(movimientoId, "movimientoId");
        Objects.requireNonNull(articuloId, "articuloId");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(cantidadBase, "cantidadBase");
        Objects.requireNonNull(unidadBase, "unidadBase");
        Objects.requireNonNull(costoUnitario, "costoUnitario");
        Objects.requireNonNull(costoTotal, "costoTotal");
        Objects.requireNonNull(cppResultante, "cppResultante");
        Objects.requireNonNull(saldoResultante, "saldoResultante");
        Objects.requireNonNull(causaTipo, "causaTipo");
    }

    /** Carga del sobre, con las claves del contrato. */
    public Map<String, Object> datos() {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("movimiento_id", movimientoId.toString());
        datos.put("articulo_id", articuloId.toString());
        datos.put("tipo", tipo);
        datos.put("cantidad_base", DecimalPlano.escribir(cantidadBase));
        datos.put("unidad_base", unidadBase);
        datos.put("costo_unitario", DecimalPlano.escribir(costoUnitario));
        datos.put("costo_total", DecimalPlano.escribir(costoTotal));
        datos.put("cpp_resultante", DecimalPlano.escribir(cppResultante));
        datos.put("saldo_resultante", DecimalPlano.escribir(saldoResultante));
        datos.put("causa_tipo", causaTipo);
        if (causaId != null) {
            datos.put("causa_id", causaId.toString());
        }
        return datos;
    }
}
