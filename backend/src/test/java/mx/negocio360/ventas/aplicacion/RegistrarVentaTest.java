package mx.negocio360.ventas.aplicacion;

import static mx.negocio360.ventas.fakes.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.pruebas.RelojMutable;
import mx.negocio360.ventas.aplicacion.puertos.ProductoParaVenta;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.EstadoVenta;
import mx.negocio360.ventas.fakes.Escenario;
import mx.negocio360.ventas.fakes.Fixtures;
import mx.negocio360.ventas.eventos.VentaRegistrada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** VF-10 RegistrarVenta: VT-01 a VT-08, VT-12 y VT-13 con preparar -> confirmar. */
class RegistrarVentaTest {

    private final Escenario e = new Escenario();

    @Test
    @DisplayName("preparar muestra total y cambio y no guarda ninguna venta ni publica eventos")
    void preparar_noEscribe() {
        var creada = e.api.preparar(e.ctxA, Fixtures.ventaSinCupon());

        assertThat(creada.resumen()).contains("150.00").contains("50.00");
        assertThat(e.ventas.todas()).isEmpty();
        assertThat(e.eventos.todos()).isEmpty();
    }

    @Test
    @DisplayName("confirmar guarda la venta REGISTRADA con folio 1, sus lineas y pagos, y devuelve folio y cambio")
    void confirmar_guardaLaVenta() {
        var resultado = e.confirmar(e.ctxA, Fixtures.ventaSinCupon());

        var venta = e.api.venta(e.ctxA, resultado.recursoId()).orElseThrow();
        assertThat(venta.estado()).isEqualTo(EstadoVenta.REGISTRADA);
        assertThat(venta.folio()).isEqualTo(1);
        assertThat(venta.serie()).isEqualTo("A");
        assertThat(venta.turnoId()).isEqualTo(Fixtures.TURNO);
        assertThat(venta.subtotal()).isEqualTo(new BigDecimal("150.00"));
        assertThat(venta.descuento()).isEqualTo(new BigDecimal("0.00"));
        assertThat(venta.total()).isEqualTo(new BigDecimal("150.00"));
        assertThat(venta.lineas()).hasSize(2);
        assertThat(venta.pagos()).hasSize(1);
        assertThat(venta.registradaPor()).isEqualTo("cajero-a");
        assertThat(venta.ocurridoEn()).isEqualTo(RelojMutable.AHORA);
        assertThat(resultado.datos()).containsEntry("folio", "1").containsEntry("total", "150.00")
                .containsEntry("cambio", "50.00");
    }

    @Test
    @DisplayName("VT-06: los folios son consecutivos por negocio y no se comparten entre negocios")
    void folios_consecutivosPorNegocio() {
        var primera = e.vender(e.ctxA, Fixtures.ventaSinCupon());
        var segunda = e.vender(e.ctxA, Fixtures.ventaSinCupon());
        var otroNegocio = e.vender(e.ctxB, Fixtures.ventaSinCupon());

        assertThat(e.api.venta(e.ctxA, primera).orElseThrow().folio()).isEqualTo(1);
        assertThat(e.api.venta(e.ctxA, segunda).orElseThrow().folio()).isEqualTo(2);
        assertThat(e.api.venta(e.ctxB, otroNegocio).orElseThrow().folio()).isEqualTo(1);
    }

    @Test
    @DisplayName("VT-06: una preparacion que vence no consume folio")
    void folios_preparacionVencida_noConsumeFolio() {
        e.api.preparar(e.ctxA, Fixtures.ventaSinCupon());
        var vendida = e.vender(e.ctxA, Fixtures.ventaSinCupon());

        assertThat(e.api.venta(e.ctxA, vendida).orElseThrow().folio()).isEqualTo(1);
    }

    @Test
    @DisplayName("confirmar publica un solo venta.registrada v1 de origen ventas, correlacionado con la venta")
    void confirmar_publicaVentaRegistrada() {
        var ventaId = e.vender(e.ctxA, Fixtures.ventaSinCupon());

        var evento = e.eventos.unicoDeTipo(VentaRegistrada.TIPO);
        assertThat(evento.version()).isEqualTo(1);
        assertThat(evento.origen()).isEqualTo("ventas");
        assertThat(evento.negocioId()).isEqualTo(e.negocioA);
        assertThat(evento.correlacionId()).isEqualTo(ventaId);
        assertThat(evento.ocurridoEn()).isEqualTo(RelojMutable.AHORA);
        assertThat(evento.datos()).containsEntry("venta_id", ventaId.toString())
                .containsEntry("total", "150.00").containsEntry("turno_id", Fixtures.TURNO.toString());
    }

    @Test
    @DisplayName("VT-05: si el precio cambia entre preparar y confirmar, falla con COMANDO_CAMBIO y no vende")
    void confirmar_precioCambio_comandoCambio() {
        var token = e.api.preparar(e.ctxA, Fixtures.ventaSinCupon()).token();
        e.catalogo.con(e.negocioA, new ProductoParaVenta(Fixtures.BOLETO, "Boleto general", "pieza",
                new BigDecimal("50.00"), false, true));

        fallaCon(() -> e.api.confirmar(e.ctxA, token), CodigoError.COMANDO_CAMBIO);
        assertThat(e.ventas.todas()).isEmpty();
        assertThat(e.eventos.todos()).isEmpty();
    }

    @Test
    @DisplayName("VT-07 y VT-08: con cupon de 10 % el total es 135.00 y el cupon queda canjeado con esa venta")
    void confirmar_conCupon_totalNetoYCanje() {
        var resultado = e.confirmar(e.ctxA, Fixtures.ventaConCupon());

        var venta = e.api.venta(e.ctxA, resultado.recursoId()).orElseThrow();
        assertThat(venta.descuento()).isEqualTo(new BigDecimal("15.00"));
        assertThat(venta.total()).isEqualTo(new BigDecimal("135.00"));
        assertThat(venta.cuponId()).isEqualTo(Fixtures.CUPON);
        assertThat(resultado.datos()).containsEntry("cambio", "65.00");
        assertThat(e.cupones.canjes()).singleElement().satisfies(c -> {
            assertThat(c.ventaId()).isEqualTo(venta.id());
            assertThat(c.subtotal()).isEqualTo(new BigDecimal("150.00"));
        });
        var datos = e.eventos.unicoDeTipo(VentaRegistrada.TIPO).datos();
        assertThat(datos).containsEntry("subtotal", "150.00").containsEntry("descuento", "15.00")
                .containsEntry("total", "135.00").containsEntry("cupon_id", Fixtures.CUPON.toString());
    }

    @Test
    @DisplayName("VT-07: un cupon desconocido falla al preparar con CUPON_INVALIDO")
    void preparar_cuponDesconocido_cuponInvalido() {
        var comando = new RegistrarVentaComando(Fixtures.TURNO, Fixtures.dosBoletosYMedioKilo(),
                List.of(Fixtures.efectivo("135.00", null)), "NO-EXISTE", null);

        fallaCon(() -> e.api.preparar(e.ctxA, comando), CodigoError.CUPON_INVALIDO);
    }

    @Test
    @DisplayName("VT-07: si el cupon se canjeo en otra venta entre preparar y confirmar, no hay segunda venta")
    void confirmar_cuponYaCanjeado_noVende() {
        var primero = e.api.preparar(e.ctxA, Fixtures.ventaConCupon()).token();
        var segundo = e.api.preparar(e.ctxA, Fixtures.ventaConCupon()).token();
        e.api.confirmar(e.ctxA, primero);

        fallaCon(() -> e.api.confirmar(e.ctxA, segundo), CodigoError.CUPON_YA_CANJEADO);
        assertThat(e.ventas.todas()).hasSize(1);
        assertThat(e.eventos.deTipo(VentaRegistrada.TIPO)).hasSize(1);
    }

    @Test
    @DisplayName("VT-07: si Pasaporte falla al canjear, no se registra la venta ni se gasta folio")
    void confirmar_canjeFalla_noVende() {
        var token = e.api.preparar(e.ctxA, Fixtures.ventaConCupon()).token();
        e.cupones.fallarAlCanjear = true;

        fallaCon(() -> e.api.confirmar(e.ctxA, token), CodigoError.ERROR_TEMPORAL);
        assertThat(e.ventas.todas()).isEmpty();
        assertThat(e.eventos.todos()).isEmpty();

        e.cupones.fallarAlCanjear = false;
        var vendida = e.vender(e.ctxA, Fixtures.ventaConCupon());
        assertThat(e.api.venta(e.ctxA, vendida).orElseThrow().folio()).isEqualTo(1);
    }

    @Test
    @DisplayName("VT-08: un descuento mayor al subtotal falla con DESCUENTO_MAYOR_AL_SUBTOTAL")
    void preparar_descuentoMayorAlSubtotal() {
        e.cupones.montoFijo("GRANDE", UUID.randomUUID(), "500.00");
        var comando = new RegistrarVentaComando(Fixtures.TURNO, Fixtures.dosBoletosYMedioKilo(),
                List.of(Fixtures.efectivo("0.01", null)), "GRANDE", null);

        fallaCon(() -> e.api.preparar(e.ctxA, comando), CodigoError.DESCUENTO_MAYOR_AL_SUBTOTAL);
    }

    @Test
    @DisplayName("VT-01: sin turno la venta falla con TURNO_NO_ABIERTO")
    void preparar_sinTurno_turnoNoAbierto() {
        var comando = new RegistrarVentaComando(null, Fixtures.dosBoletosYMedioKilo(),
                List.of(Fixtures.efectivo("150.00", null)), null, null);

        fallaCon(() -> e.api.preparar(e.ctxA, comando), CodigoError.TURNO_NO_ABIERTO);
    }

    @Test
    @DisplayName("VT-13: el cliente opcional viaja como id en el evento; sin cliente el campo se omite")
    void confirmar_clienteOpcional() {
        var cliente = UUID.randomUUID();
        var conCliente = new RegistrarVentaComando(Fixtures.TURNO, Fixtures.dosBoletosYMedioKilo(),
                List.of(Fixtures.efectivo("150.00", null)), null, cliente);

        var ventaId = e.vender(e.ctxA, conCliente);
        e.vender(e.ctxA, Fixtures.ventaSinCupon());

        assertThat(e.api.venta(e.ctxA, ventaId).orElseThrow().clienteId()).isEqualTo(cliente);
        var eventos = e.eventos.deTipo(VentaRegistrada.TIPO);
        assertThat(eventos.get(0).datos()).containsEntry("cliente_id", cliente.toString());
        assertThat(eventos.get(1).datos()).doesNotContainKeys("cliente_id", "cupon_id");
    }

    @Test
    @DisplayName("un negocio no ve las ventas de otro")
    void venta_deOtroNegocio_noSeVe() {
        var ventaId = e.vender(e.ctxA, Fixtures.ventaSinCupon());

        assertThat(e.api.venta(e.ctxB, ventaId)).isEmpty();
        assertThat(e.api.ventasDelTurno(e.ctxB, Fixtures.TURNO)).isEmpty();
        assertThat(e.api.ventasDelTurno(e.ctxA, Fixtures.TURNO)).hasSize(1);
    }

    @Test
    @DisplayName("Catalogo de un negocio no sirve para otro: el producto no existe alla")
    void preparar_productoDeOtroNegocio_productoNoEncontrado() {
        var soloEnB = UUID.randomUUID();
        e.catalogo.con(e.negocioB, new ProductoParaVenta(soloEnB, "Taza", "pieza", new BigDecimal("80.00"), false,
                true));
        var comando = new RegistrarVentaComando(Fixtures.TURNO,
                List.of(new LineaSolicitada(soloEnB, BigDecimal.ONE, null)), List.of(Fixtures.efectivo("80.00", null)),
                null, null);

        fallaCon(() -> e.api.preparar(e.ctxA, comando), CodigoError.PRODUCTO_NO_ENCONTRADO);
    }
}
