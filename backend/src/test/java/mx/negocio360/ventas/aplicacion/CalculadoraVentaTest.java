package mx.negocio360.ventas.aplicacion;

import static mx.negocio360.ventas.fakes.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.catalogo.ProductoParaVenta;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.MetodoPago;
import mx.negocio360.ventas.fakes.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** VT-02, VT-03, VT-04 y VT-08: cifras de la venta calculadas solo por el codigo. */
class CalculadoraVentaTest {

    private static final Map<UUID, ProductoParaVenta> PRODUCTOS = Map.of(
            Fixtures.BOLETO, Fixtures.boleto(), Fixtures.CAMOTE, Fixtures.camote(),
            Fixtures.INACTIVO, Fixtures.inactivo());

    private static LineaSolicitada linea(UUID producto, String cantidad) {
        return new LineaSolicitada(producto, new BigDecimal(cantidad), null);
    }

    @Test
    @DisplayName("VT-02: copia descripcion, unidad y precio de Catalogo y numera las lineas desde 1")
    void lineas_copianDatosDeCatalogo() {
        var lineas = CalculadoraVenta.lineas(Fixtures.dosBoletosYMedioKilo(), PRODUCTOS);

        assertThat(lineas).hasSize(2);
        var boleto = lineas.get(0);
        assertThat(boleto.linea()).isEqualTo(1);
        assertThat(boleto.descripcion()).isEqualTo("Boleto general");
        assertThat(boleto.unidad()).isEqualTo("pieza");
        assertThat(boleto.precioUnitario()).isEqualTo(new BigDecimal("45.0000"));
        assertThat(boleto.cantidad()).isEqualTo(new BigDecimal("2.0000"));
        assertThat(boleto.importe()).isEqualTo(new BigDecimal("90.00"));
        assertThat(lineas.get(1).linea()).isEqualTo(2);
        assertThat(lineas.get(1).importe()).isEqualTo(new BigDecimal("60.00"));
    }

    @Test
    @DisplayName("VT-08: el subtotal es la suma de importes: 90.00 + 60.00 = 150.00")
    void subtotal_sumaImportes() {
        var lineas = CalculadoraVenta.lineas(Fixtures.dosBoletosYMedioKilo(), PRODUCTOS);

        assertThat(CalculadoraVenta.subtotal(lineas)).isEqualTo(new BigDecimal("150.00"));
    }

    @Test
    @DisplayName("el importe redondea HALF_UP: 0.333 kg a 120.00 = 39.96")
    void importe_redondeaHalfUp() {
        var lineas = CalculadoraVenta.lineas(List.of(linea(Fixtures.CAMOTE, "0.333")), PRODUCTOS);

        assertThat(lineas.get(0).importe()).isEqualTo(new BigDecimal("39.96"));
    }

    @Test
    @DisplayName("VT-03: medio boleto no se puede vender (el producto no admite fraccion)")
    void lineas_fraccionEnProductoPorPieza_cantidadNoFraccionable() {
        fallaCon(() -> CalculadoraVenta.lineas(List.of(linea(Fixtures.BOLETO, "0.5")), PRODUCTOS),
                CodigoError.CANTIDAD_NO_FRACCIONABLE);
    }

    @Test
    @DisplayName("VT-03: 2.0 boletos es una cantidad entera y si se acepta")
    void lineas_enteroConCeros_seAcepta() {
        var lineas = CalculadoraVenta.lineas(List.of(linea(Fixtures.BOLETO, "2.0")), PRODUCTOS);

        assertThat(lineas.get(0).importe()).isEqualTo(new BigDecimal("90.00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "0.00001", "100000000"})
    @DisplayName("VT-03: cantidad cero, negativa, con mas de 4 decimales o desmedida falla con CANTIDAD_INVALIDA")
    void lineas_cantidadFueraDeRango_cantidadInvalida(String cantidad) {
        fallaCon(() -> CalculadoraVenta.lineas(List.of(linea(Fixtures.CAMOTE, cantidad)), PRODUCTOS),
                CodigoError.CANTIDAD_INVALIDA);
    }

    @Test
    @DisplayName("una linea sin producto o sin cantidad falla con CANTIDAD_INVALIDA")
    void lineas_incompleta_cantidadInvalida() {
        fallaCon(() -> CalculadoraVenta.lineas(List.of(new LineaSolicitada(Fixtures.BOLETO, null, null)), PRODUCTOS),
                CodigoError.CANTIDAD_INVALIDA);
        fallaCon(() -> CalculadoraVenta.lineas(List.of(new LineaSolicitada(null, BigDecimal.ONE, null)), PRODUCTOS),
                CodigoError.PRODUCTO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("sin lineas falla con VENTA_SIN_LINEAS")
    void lineas_vacias_ventaSinLineas() {
        fallaCon(() -> CalculadoraVenta.lineas(List.of(), PRODUCTOS), CodigoError.VENTA_SIN_LINEAS);
        fallaCon(() -> CalculadoraVenta.lineas(null, PRODUCTOS), CodigoError.VENTA_SIN_LINEAS);
    }

    @Test
    @DisplayName("un producto que Catalogo no conoce falla con PRODUCTO_NO_ENCONTRADO y su id")
    void lineas_productoDesconocido_productoNoEncontrado() {
        var desconocido = UUID.randomUUID();

        var error = fallaCon(() -> CalculadoraVenta.lineas(List.of(linea(desconocido, "1")), PRODUCTOS),
                CodigoError.PRODUCTO_NO_ENCONTRADO);

        assertThat(error.recursoId()).contains(desconocido);
    }

    @Test
    @DisplayName("un producto inactivo falla con PRODUCTO_INACTIVO")
    void lineas_productoInactivo_productoInactivo() {
        fallaCon(() -> CalculadoraVenta.lineas(List.of(linea(Fixtures.INACTIVO, "1")), PRODUCTOS),
                CodigoError.PRODUCTO_INACTIVO);
    }

    @Test
    @DisplayName("VT-04: efectivo de 200.00 para 135.00 da 65.00 de cambio")
    void pagos_efectivoConCambio() {
        var pagos = CalculadoraVenta.pagos(new BigDecimal("135.00"), List.of(Fixtures.efectivo("135.00", "200.00")));

        assertThat(pagos).singleElement().satisfies(p -> {
            assertThat(p.metodo()).isEqualTo(MetodoPago.EFECTIVO);
            assertThat(p.monto()).isEqualTo(new BigDecimal("135.00"));
            assertThat(p.recibido()).isEqualTo(new BigDecimal("200.00"));
            assertThat(p.cambio()).isEqualTo(new BigDecimal("65.00"));
        });
        assertThat(CalculadoraVenta.cambio(pagos)).isEqualTo(new BigDecimal("65.00"));
    }

    @Test
    @DisplayName("VT-04: efectivo sin recibido se toma como pago exacto, sin cambio")
    void pagos_efectivoSinRecibido_exacto() {
        var pagos = CalculadoraVenta.pagos(new BigDecimal("150.00"), List.of(Fixtures.efectivo("150.00", null)));

        assertThat(pagos.get(0).recibido()).isEqualTo(new BigDecimal("150.00"));
        assertThat(CalculadoraVenta.cambio(pagos)).isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    @DisplayName("VT-04: pago mixto, 100.00 con tarjeta y 35.00 en efectivo con 50.00 recibidos")
    void pagos_mixto_cuadra() {
        var pagos = CalculadoraVenta.pagos(new BigDecimal("135.00"),
                List.of(Fixtures.tarjeta("100.00"), Fixtures.efectivo("35.00", "50.00")));

        assertThat(pagos).hasSize(2);
        assertThat(pagos.get(0).cambio()).isNull();
        assertThat(pagos.get(0).referencia()).isEqualTo("AUT-123456");
        assertThat(CalculadoraVenta.cambio(pagos)).isEqualTo(new BigDecimal("15.00"));
    }

    @Test
    @DisplayName("VT-04: si la suma de pagos no es el total falla con PAGOS_NO_CUADRAN")
    void pagos_noCuadran() {
        fallaCon(() -> CalculadoraVenta.pagos(new BigDecimal("135.00"), List.of(Fixtures.efectivo("130.00", null))),
                CodigoError.PAGOS_NO_CUADRAN);
        fallaCon(() -> CalculadoraVenta.pagos(new BigDecimal("135.00"), List.of()), CodigoError.PAGOS_NO_CUADRAN);
        fallaCon(() -> CalculadoraVenta.pagos(new BigDecimal("135.00"), null), CodigoError.PAGOS_NO_CUADRAN);
    }

    @Test
    @DisplayName("VT-04: recibir menos efectivo que el monto falla con EFECTIVO_INSUFICIENTE")
    void pagos_recibidoMenor_efectivoInsuficiente() {
        fallaCon(() -> CalculadoraVenta.pagos(new BigDecimal("135.00"),
                List.of(Fixtures.efectivo("135.00", "100.00"))), CodigoError.EFECTIVO_INSUFICIENTE);
    }

    @Test
    @DisplayName("un pago sin metodo, con monto no positivo o con recibido en tarjeta falla con METODO_PAGO_INVALIDO")
    void pagos_malFormados_metodoPagoInvalido() {
        var total = new BigDecimal("10.00");
        fallaCon(() -> CalculadoraVenta.pagos(total, List.of(new PagoSolicitado(null, total, null, null))),
                CodigoError.METODO_PAGO_INVALIDO);
        fallaCon(() -> CalculadoraVenta.pagos(total, List.of(new PagoSolicitado(MetodoPago.TARJETA, total,
                new BigDecimal("20.00"), null))), CodigoError.METODO_PAGO_INVALIDO);
        fallaCon(() -> CalculadoraVenta.pagos(total, List.of(Fixtures.efectivo("0.00", null),
                Fixtures.efectivo("10.00", null))), CodigoError.METODO_PAGO_INVALIDO);
        fallaCon(() -> CalculadoraVenta.pagos(total, List.of(Fixtures.efectivo("10.001", null))),
                CodigoError.METODO_PAGO_INVALIDO);
        fallaCon(() -> CalculadoraVenta.pagos(total, List.of(new PagoSolicitado(MetodoPago.EFECTIVO, null, null,
                null))), CodigoError.METODO_PAGO_INVALIDO);
    }
}
