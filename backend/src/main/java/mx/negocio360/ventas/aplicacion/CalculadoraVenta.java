package mx.negocio360.ventas.aplicacion;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.dinero.Redondeo;
import mx.negocio360.ventas.aplicacion.puertos.ProductoParaVenta;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.LineaVenta;
import mx.negocio360.ventas.dominio.MetodoPago;
import mx.negocio360.ventas.dominio.PagoVenta;
import mx.negocio360.ventas.dominio.VentasException;

/**
 * Cifras de una venta, calculadas solo por el codigo (principio rector). Funciones puras: no leen ni escriben nada.
 * Redondeo HALF_UP con {@link Redondeo} (D-20).
 */
public final class CalculadoraVenta {

    /** Maximo de renglones por venta (limite de entrada). */
    public static final int MAX_LINEAS = 100;
    /** Cota superior exclusiva de una cantidad. */
    static final BigDecimal MAX_CANTIDAD = new BigDecimal("1000000");
    private static final int MAX_CARACTERES_SERIE = 60;
    private static final int MAX_CARACTERES_REFERENCIA = 60;

    private CalculadoraVenta() {
    }

    /**
     * Arma los renglones con precio, descripcion y unidad de Catalogo (VT-02, VT-03).
     *
     * @param solicitadas renglones que pide el panel
     * @param productos   productos de Catalogo del negocio, por id
     * @return renglones numerados desde 1, con importe en escala 2
     * @throws VentasException VENTA_SIN_LINEAS, CANTIDAD_INVALIDA, CANTIDAD_NO_FRACCIONABLE,
     *                         PRODUCTO_NO_ENCONTRADO o PRODUCTO_INACTIVO
     */
    public static List<LineaVenta> lineas(List<LineaSolicitada> solicitadas, Map<UUID, ProductoParaVenta> productos) {
        if (solicitadas == null || solicitadas.isEmpty()) {
            throw new VentasException(CodigoError.VENTA_SIN_LINEAS);
        }
        if (solicitadas.size() > MAX_LINEAS) {
            throw new IllegalArgumentException("Una venta admite hasta " + MAX_LINEAS + " lineas");
        }
        var lineas = new ArrayList<LineaVenta>(solicitadas.size());
        for (var i = 0; i < solicitadas.size(); i++) {
            lineas.add(linea(i + 1, solicitadas.get(i), productos));
        }
        return List.copyOf(lineas);
    }

    private static LineaVenta linea(int numero, LineaSolicitada solicitada, Map<UUID, ProductoParaVenta> productos) {
        if (solicitada == null) {
            throw new VentasException(CodigoError.CANTIDAD_INVALIDA);
        }
        if (solicitada.productoId() == null) {
            throw new VentasException(CodigoError.PRODUCTO_NO_ENCONTRADO);
        }
        var cantidad = cantidadValida(solicitada.cantidad());
        var producto = productos.get(solicitada.productoId());
        if (producto == null) {
            throw new VentasException(CodigoError.PRODUCTO_NO_ENCONTRADO, solicitada.productoId());
        }
        if (!producto.activo()) {
            throw new VentasException(CodigoError.PRODUCTO_INACTIVO, solicitada.productoId());
        }
        if (!producto.admiteFraccion() && cantidad.stripTrailingZeros().scale() > 0) {
            throw new VentasException(CodigoError.CANTIDAD_NO_FRACCIONABLE, solicitada.productoId());
        }
        var cantidadVenta = Redondeo.cantidad(cantidad);
        var precio = Redondeo.precio(producto.precio());
        var importe = Redondeo.dinero(cantidadVenta.multiply(precio));
        return new LineaVenta(numero, producto.productoId(), producto.descripcion(), cantidadVenta, producto.unidad(),
                precio, importe, textoOpcional(solicitada.serie(), MAX_CARACTERES_SERIE));
    }

    private static BigDecimal cantidadValida(BigDecimal cantidad) {
        var valida = cantidad != null && cantidad.signum() > 0 && cantidad.compareTo(MAX_CANTIDAD) < 0
                && cantidad.stripTrailingZeros().scale() <= Redondeo.ESCALA_CANTIDAD;
        if (!valida) {
            throw new VentasException(CodigoError.CANTIDAD_INVALIDA);
        }
        return cantidad;
    }

    /**
     * Suma de importes (VT-08).
     *
     * @param lineas renglones de la venta
     * @return subtotal en escala 2
     */
    public static BigDecimal subtotal(List<LineaVenta> lineas) {
        return Redondeo.dinero(lineas.stream().map(LineaVenta::importe).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    /**
     * Valida los pagos contra el total y calcula el cambio del efectivo (VT-04).
     *
     * @param total       total de la venta, escala 2
     * @param solicitados pagos que captura el cajero
     * @return pagos en escala 2; solo el efectivo lleva recibido y cambio
     * @throws VentasException PAGOS_NO_CUADRAN, EFECTIVO_INSUFICIENTE o METODO_PAGO_INVALIDO
     */
    public static List<PagoVenta> pagos(BigDecimal total, List<PagoSolicitado> solicitados) {
        if (solicitados == null || solicitados.isEmpty()) {
            throw new VentasException(CodigoError.PAGOS_NO_CUADRAN);
        }
        var pagos = solicitados.stream().map(CalculadoraVenta::pago).toList();
        var suma = pagos.stream().map(PagoVenta::monto).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (suma.compareTo(total) != 0) {
            throw new VentasException(CodigoError.PAGOS_NO_CUADRAN);
        }
        return pagos;
    }

    private static PagoVenta pago(PagoSolicitado solicitado) {
        if (solicitado == null || solicitado.metodo() == null || !montoValido(solicitado.monto())) {
            throw new VentasException(CodigoError.METODO_PAGO_INVALIDO);
        }
        var monto = Redondeo.dinero(solicitado.monto());
        var referencia = textoOpcional(solicitado.referencia(), MAX_CARACTERES_REFERENCIA);
        if (solicitado.metodo() != MetodoPago.EFECTIVO) {
            if (solicitado.recibido() != null) {
                throw new VentasException(CodigoError.METODO_PAGO_INVALIDO);
            }
            return new PagoVenta(solicitado.metodo(), monto, null, null, referencia);
        }
        var recibido = solicitado.recibido() == null ? monto : solicitado.recibido();
        if (!montoValido(recibido)) {
            throw new VentasException(CodigoError.METODO_PAGO_INVALIDO);
        }
        if (recibido.compareTo(monto) < 0) {
            throw new VentasException(CodigoError.EFECTIVO_INSUFICIENTE);
        }
        var recibidoEscalado = Redondeo.dinero(recibido);
        return new PagoVenta(MetodoPago.EFECTIVO, monto, recibidoEscalado, recibidoEscalado.subtract(monto),
                referencia);
    }

    private static boolean montoValido(BigDecimal monto) {
        return monto != null && monto.signum() > 0 && monto.stripTrailingZeros().scale() <= Redondeo.ESCALA_DINERO;
    }

    /**
     * Cambio total que se entrega: la suma del cambio de los pagos en efectivo.
     *
     * @param pagos pagos validados
     * @return cambio en escala 2; 0.00 si no hay efectivo
     */
    public static BigDecimal cambio(List<PagoVenta> pagos) {
        return Redondeo.dinero(pagos.stream().map(PagoVenta::cambio).filter(c -> c != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private static String textoOpcional(String texto, int maximo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        var limpio = texto.strip();
        if (limpio.length() > maximo) {
            throw new IllegalArgumentException("Texto de mas de " + maximo + " caracteres");
        }
        return limpio;
    }
}
