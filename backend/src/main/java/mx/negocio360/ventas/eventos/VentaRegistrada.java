package mx.negocio360.ventas.eventos;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import mx.negocio360.compartido.dinero.DecimalPlano;
import mx.negocio360.compartido.dinero.Redondeo;
import mx.negocio360.ventas.dominio.Venta;

/**
 * {@code venta.registrada} v1 (D-136). Se publica al confirmar RegistrarVenta, en la misma transaccion (outbox).
 *
 * <p>Campos de {@code datos}: {@code venta_id}, {@code folio} (entero), {@code serie}, {@code turno_id},
 * {@code cliente_id} (opcional, D-48), {@code cupon_id} (opcional), {@code subtotal}, {@code descuento},
 * {@code total} (neto de cupon, D-65), {@code lineas[]} con {@code linea}, {@code producto_id}, {@code cantidad},
 * {@code unidad}, {@code precio_unitario}, {@code importe} y {@code serie} opcional, y {@code pagos[]} con
 * {@code metodo} y {@code monto}. Dinero y cantidades como cadena decimal. Sin datos personales: no lleva quien cobro
 * ni lo recibido ni el cambio.
 *
 * @param venta venta registrada
 */
public record VentaRegistrada(Venta venta) {

    /** Tipo del evento en el sobre. */
    public static final String TIPO = "venta.registrada";
    /** Version del contrato (R5). */
    public static final int VERSION = 1;

    /** Exige la venta. */
    public VentaRegistrada {
        Objects.requireNonNull(venta, "venta");
    }

    /**
     * Carga del sobre con las claves del contrato. Un opcional que no aplica se omite.
     *
     * @return mapa listo para {@code SobreEvento}
     */
    public Map<String, Object> datos() {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("venta_id", venta.id().toString());
        datos.put("folio", venta.folio());
        datos.put("serie", venta.serie());
        datos.put("turno_id", venta.turnoId().toString());
        if (venta.clienteId() != null) {
            datos.put("cliente_id", venta.clienteId().toString());
        }
        if (venta.cuponId() != null) {
            datos.put("cupon_id", venta.cuponId().toString());
        }
        datos.put("subtotal", DecimalPlano.escribir(venta.subtotal()));
        datos.put("descuento", DecimalPlano.escribir(venta.descuento()));
        datos.put("total", DecimalPlano.escribir(venta.total()));
        datos.put("lineas", venta.lineas().stream().map(l -> {
            var linea = new LinkedHashMap<String, Object>();
            linea.put("linea", l.linea());
            linea.put("producto_id", l.productoId().toString());
            linea.put("cantidad", Redondeo.cantidadComoTexto(l.cantidad()));
            linea.put("unidad", l.unidad());
            linea.put("precio_unitario", DecimalPlano.escribir(l.precioUnitario()));
            linea.put("importe", DecimalPlano.escribir(l.importe()));
            if (l.serie() != null) {
                linea.put("serie", l.serie());
            }
            return linea;
        }).toList());
        datos.put("pagos", VentaAnulada.pagosDe(venta));
        return datos;
    }
}
