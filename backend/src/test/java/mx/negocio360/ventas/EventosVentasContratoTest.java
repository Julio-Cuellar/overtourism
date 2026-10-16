package mx.negocio360.ventas;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.clientes.contratos.VentaAnuladaV1;
import mx.negocio360.clientes.contratos.VentaRegistradaV1;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.eventos.MapeadorEventos;
import mx.negocio360.ventas.aplicacion.AnularVentaComando;
import mx.negocio360.ventas.aplicacion.RegistrarVentaComando;
import mx.negocio360.ventas.eventos.VentaAnulada;
import mx.negocio360.ventas.eventos.VentaRegistrada;
import mx.negocio360.ventas.fakes.Escenario;
import mx.negocio360.ventas.fakes.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D-77: forma exacta de {@code venta.registrada} v1 y {@code venta.anulada} v1, y compatibilidad con el lector que
 * Clientes ya tiene programado (D-48, D-65, R5).
 */
class EventosVentasContratoTest {

    private final Escenario e = new Escenario();

    @Test
    @DisplayName("venta.registrada lleva todos los campos del contrato, con dinero y cantidades como cadena")
    @SuppressWarnings("unchecked")
    void ventaRegistrada_camposDelContrato() {
        e.vender(e.ctxA, Fixtures.ventaConCupon());

        var datos = e.eventos.unicoDeTipo(VentaRegistrada.TIPO).datos();
        assertThat(datos).containsKeys("venta_id", "folio", "serie", "turno_id", "cupon_id", "subtotal", "descuento",
                "total", "lineas", "pagos");
        assertThat(datos).containsEntry("folio", 1L).containsEntry("serie", "A");
        var lineas = (List<Map<String, Object>>) datos.get("lineas");
        assertThat(lineas.get(0)).containsEntry("linea", 1).containsEntry("producto_id", Fixtures.BOLETO.toString())
                .containsEntry("cantidad", "2").containsEntry("unidad", "pieza")
                .containsEntry("precio_unitario", "45.0000").containsEntry("importe", "90.00")
                .doesNotContainKey("serie");
        assertThat(lineas.get(1)).containsEntry("cantidad", "0.5").containsEntry("unidad", "kg")
                .containsEntry("importe", "60.00");
        var pagos = (List<Map<String, Object>>) datos.get("pagos");
        assertThat(pagos).singleElement()
                .satisfies(p -> assertThat(p).containsOnlyKeys("metodo", "monto").containsEntry("monto", "135.00"));
    }

    @Test
    @DisplayName("venta.registrada no lleva datos personales: ni quien cobro ni recibido ni cambio")
    void ventaRegistrada_sinDatosPersonales() {
        e.vender(e.ctxA, Fixtures.ventaSinCupon());

        var texto = e.eventos.unicoDeTipo(VentaRegistrada.TIPO).datos().toString();
        assertThat(texto).doesNotContain("cajero-a").doesNotContain("recibido").doesNotContain("cambio");
    }

    @Test
    @DisplayName("Clientes lee venta.registrada con su lector tolerante: id, cliente y total neto de cupon")
    void ventaRegistrada_laLeeClientes() {
        var cliente = UUID.randomUUID();
        var ventaId = e.vender(e.ctxA, new RegistrarVentaComando(Fixtures.TURNO, Fixtures.dosBoletosYMedioKilo(),
                List.of(Fixtures.efectivo("135.00", null)), Fixtures.CODIGO_CUPON, cliente));

        var leida = MapeadorEventos.crear().convertValue(e.eventos.unicoDeTipo(VentaRegistrada.TIPO).datos(),
                VentaRegistradaV1.class);

        assertThat(leida.ventaId()).isEqualTo(ventaId);
        assertThat(leida.cliente()).contains(cliente);
        assertThat(leida.total()).isEqualTo(new BigDecimal("135.00"));
    }

    @Test
    @DisplayName("Clientes lee venta.anulada con su lector tolerante")
    void ventaAnulada_laLeeClientes() {
        var ventaId = e.vender(e.ctxA, Fixtures.ventaSinCupon());
        e.confirmar(new ContextoNegocio(e.negocioA, "encargado-a"), new AnularVentaComando(ventaId, "Error"));

        var leida = MapeadorEventos.crear().convertValue(e.eventos.unicoDeTipo(VentaAnulada.TIPO).datos(),
                VentaAnuladaV1.class);

        assertThat(leida.ventaId()).isEqualTo(ventaId);
    }
}
