package mx.negocio360.ventas.fakes;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import mx.negocio360.ventas.aplicacion.LineaSolicitada;
import mx.negocio360.ventas.aplicacion.PagoSolicitado;
import mx.negocio360.ventas.aplicacion.RegistrarVentaComando;
import mx.negocio360.compartido.catalogo.ProductoParaVenta;
import mx.negocio360.ventas.dominio.MetodoPago;

/**
 * Datos de ejemplo inventados, los mismos del diseño (tienda del museo ficticio «Museo Talavera Viva»): boleto general
 * de 45.00 por pieza y camote a granel a 120.00 el kilo. 2 boletos y medio kilo dan 150.00.
 */
public final class Fixtures {

    public static final UUID BOLETO = UUID.fromString("0191f1b0-0001-7000-8000-000000000001");
    public static final UUID CAMOTE = UUID.fromString("0191f1b0-0002-7000-8000-000000000002");
    public static final UUID INACTIVO = UUID.fromString("0191f1b0-0003-7000-8000-000000000003");
    public static final UUID TURNO = UUID.fromString("0192a39f-0c11-7d20-8e31-abcdefabcdef");
    public static final UUID CUPON = UUID.fromString("0192a4c0-d001-7e00-8f00-000000000abc");
    public static final String CODIGO_CUPON = "PAS-7Q2K";

    private Fixtures() {
    }

    public static ProductoParaVenta boleto() {
        return new ProductoParaVenta(BOLETO, "Boleto general", "pieza", new BigDecimal("45.00"), false, true);
    }

    public static ProductoParaVenta camote() {
        return new ProductoParaVenta(CAMOTE, "Camote poblano", "kg", new BigDecimal("120.00"), true, true);
    }

    public static ProductoParaVenta inactivo() {
        return new ProductoParaVenta(INACTIVO, "Plato descontinuado", "pieza", new BigDecimal("350.00"), false,
                false);
    }

    public static List<LineaSolicitada> dosBoletosYMedioKilo() {
        return List.of(new LineaSolicitada(BOLETO, new BigDecimal("2"), null),
                new LineaSolicitada(CAMOTE, new BigDecimal("0.5"), null));
    }

    public static PagoSolicitado efectivo(String monto, String recibido) {
        return new PagoSolicitado(MetodoPago.EFECTIVO, new BigDecimal(monto),
                recibido == null ? null : new BigDecimal(recibido), null);
    }

    public static PagoSolicitado tarjeta(String monto) {
        return new PagoSolicitado(MetodoPago.TARJETA, new BigDecimal(monto), null, "AUT-123456");
    }

    /** 150.00 sin cupon, pagado con 200.00 en efectivo (cambio 50.00). */
    public static RegistrarVentaComando ventaSinCupon() {
        return new RegistrarVentaComando(TURNO, dosBoletosYMedioKilo(), List.of(efectivo("150.00", "200.00")),
                null, null);
    }

    /** 150.00 con cupon de 15.00 = 135.00, pagado con 200.00 en efectivo (cambio 65.00). */
    public static RegistrarVentaComando ventaConCupon() {
        return new RegistrarVentaComando(TURNO, dosBoletosYMedioKilo(), List.of(efectivo("135.00", "200.00")),
                CODIGO_CUPON, null);
    }
}
