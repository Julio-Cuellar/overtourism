package mx.negocio360.ventas.eventos;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import mx.negocio360.compartido.dinero.DecimalPlano;
import mx.negocio360.ventas.dominio.Venta;

/**
 * {@code venta.anulada} v1 (D-77). Se publica al confirmar AnularVenta, en la misma transaccion.
 *
 * <p>Campos de {@code datos}: {@code venta_id}, {@code turno_id}, {@code motivo} (opcional), {@code lineas[]} con
 * {@code linea} y {@code producto_id} (D-25, para Inventario) y {@code pagos[]} con {@code metodo} y {@code monto}
 * (para que Caja compense).
 *
 * @param venta venta ya anulada
 */
public record VentaAnulada(Venta venta) {

    /** Tipo del evento en el sobre. */
    public static final String TIPO = "venta.anulada";
    /** Version del contrato (R5). */
    public static final int VERSION = 1;

    /** Exige la venta. */
    public VentaAnulada {
        Objects.requireNonNull(venta, "venta");
    }

    /**
     * Carga del sobre con las claves del contrato.
     *
     * @return mapa listo para {@code SobreEvento}
     */
    public Map<String, Object> datos() {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("venta_id", venta.id().toString());
        datos.put("turno_id", venta.turnoId().toString());
        if (venta.motivoAnulacion() != null) {
            datos.put("motivo", venta.motivoAnulacion());
        }
        datos.put("lineas", venta.lineas().stream()
                .map(l -> Map.<String, Object>of("linea", l.linea(), "producto_id", l.productoId().toString()))
                .toList());
        datos.put("pagos", pagosDe(venta));
        return datos;
    }

    /** Pagos con solo metodo y monto: lo que recibio y el cambio no viajan. */
    static List<Map<String, Object>> pagosDe(Venta venta) {
        return venta.pagos().stream()
                .map(p -> Map.<String, Object>of("metodo", p.metodo().name(), "monto", DecimalPlano.escribir(p.monto())))
                .toList();
    }
}
