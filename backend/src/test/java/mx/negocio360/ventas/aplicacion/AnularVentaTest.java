package mx.negocio360.ventas.aplicacion;

import static mx.negocio360.ventas.fakes.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.pruebas.RelojMutable;
import mx.negocio360.ventas.dominio.CodigoError;
import mx.negocio360.ventas.dominio.EstadoVenta;
import mx.negocio360.ventas.eventos.VentaAnulada;
import mx.negocio360.ventas.fakes.Escenario;
import mx.negocio360.ventas.fakes.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** VF-11 AnularVenta: VT-09 y VT-11 (D-82). */
class AnularVentaTest {

    private final Escenario e = new Escenario();
    private final ContextoNegocio encargado = new ContextoNegocio(e.negocioA, "encargado-a");

    private UUID ventaRegistrada() {
        return e.vender(e.ctxA, Fixtures.ventaSinCupon());
    }

    @Test
    @DisplayName("un encargado anula con motivo: la venta queda ANULADA con quien, cuando y por que")
    void anular_conPermiso_quedaAnulada() {
        var ventaId = ventaRegistrada();
        e.reloj.avanzar(Duration.ofMinutes(10));

        var resultado = e.confirmar(encargado, new AnularVentaComando(ventaId, "Cobro duplicado"));

        assertThat(resultado.recursoId()).isEqualTo(ventaId);
        var venta = e.api.venta(e.ctxA, ventaId).orElseThrow();
        assertThat(venta.estado()).isEqualTo(EstadoVenta.ANULADA);
        assertThat(venta.anuladaPor()).isEqualTo("encargado-a");
        assertThat(venta.anuladaEn()).isEqualTo(RelojMutable.AHORA.plus(Duration.ofMinutes(10)));
        assertThat(venta.motivoAnulacion()).isEqualTo("Cobro duplicado");
        assertThat(venta.version()).isEqualTo(2);
    }

    @Test
    @DisplayName("VT-11: anular conserva las lineas, los pagos y los montos de la venta")
    void anular_conservaLaVenta() {
        var ventaId = ventaRegistrada();
        var antes = e.api.venta(e.ctxA, ventaId).orElseThrow();

        e.confirmar(encargado, new AnularVentaComando(ventaId, "Error de captura"));

        var despues = e.api.venta(e.ctxA, ventaId).orElseThrow();
        assertThat(despues.lineas()).isEqualTo(antes.lineas());
        assertThat(despues.pagos()).isEqualTo(antes.pagos());
        assertThat(despues.total()).isEqualTo(antes.total());
        assertThat(despues.folio()).isEqualTo(antes.folio());
    }

    @Test
    @DisplayName("publica venta.anulada v1 con turno, motivo, lineas y pagos (D-25, D-77)")
    @SuppressWarnings("unchecked")
    void anular_publicaVentaAnulada() {
        var ventaId = ventaRegistrada();

        e.confirmar(encargado, new AnularVentaComando(ventaId, "Cobro duplicado"));

        var evento = e.eventos.unicoDeTipo(VentaAnulada.TIPO);
        assertThat(evento.correlacionId()).isEqualTo(ventaId);
        assertThat(evento.datos()).containsEntry("venta_id", ventaId.toString())
                .containsEntry("turno_id", Fixtures.TURNO.toString())
                .containsEntry("motivo", "Cobro duplicado");
        var lineas = (List<Map<String, Object>>) evento.datos().get("lineas");
        assertThat(lineas).hasSize(2);
        assertThat(lineas.get(0)).containsEntry("linea", 1).containsEntry("producto_id", Fixtures.BOLETO.toString());
        var pagos = (List<Map<String, Object>>) evento.datos().get("pagos");
        assertThat(pagos).singleElement()
                .satisfies(p -> assertThat(p).containsEntry("metodo", "EFECTIVO").containsEntry("monto", "150.00"));
    }

    @Test
    @DisplayName("VT-09: sin permiso de anular falla con SIN_PERMISO_ANULAR, tambien al preparar")
    void anular_sinPermiso_sinPermisoAnular() {
        var ventaId = ventaRegistrada();

        fallaCon(() -> e.api.preparar(e.ctxA, new AnularVentaComando(ventaId, "Error")),
                CodigoError.SIN_PERMISO_ANULAR);
        assertThat(e.api.venta(e.ctxA, ventaId).orElseThrow().estado()).isEqualTo(EstadoVenta.REGISTRADA);
    }

    @Test
    @DisplayName("VT-09: anular dos veces falla la segunda con VENTA_YA_ANULADA y publica un solo evento")
    void anular_dosVeces_ventaYaAnulada() {
        var ventaId = ventaRegistrada();
        e.confirmar(encargado, new AnularVentaComando(ventaId, "Error"));

        fallaCon(() -> e.api.preparar(encargado, new AnularVentaComando(ventaId, "Otra vez")),
                CodigoError.VENTA_YA_ANULADA);
        assertThat(e.eventos.deTipo(VentaAnulada.TIPO)).hasSize(1);
    }

    @Test
    @DisplayName("VT-09: dos anulaciones preparadas a la vez: la segunda confirmacion falla con VENTA_YA_ANULADA")
    void anular_dosPreparadas_segundaFalla() {
        var ventaId = ventaRegistrada();
        var primera = e.api.preparar(encargado, new AnularVentaComando(ventaId, "Error")).token();
        var segunda = e.api.preparar(encargado, new AnularVentaComando(ventaId, "Error")).token();
        e.api.confirmar(encargado, primera);

        fallaCon(() -> e.api.confirmar(encargado, segunda), CodigoError.VENTA_YA_ANULADA);
    }

    @Test
    @DisplayName("una venta de otro negocio o inexistente falla con VENTA_NO_ENCONTRADA")
    void anular_ventaAjena_ventaNoEncontrada() {
        var ventaId = ventaRegistrada();
        var encargadoB = new ContextoNegocio(e.negocioB, "encargado-b");

        fallaCon(() -> e.api.preparar(encargadoB, new AnularVentaComando(ventaId, "Error")),
                CodigoError.VENTA_NO_ENCONTRADA);
        fallaCon(() -> e.api.preparar(encargado, new AnularVentaComando(UUID.randomUUID(), "Error")),
                CodigoError.VENTA_NO_ENCONTRADA);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    @DisplayName("VT-09: sin motivo falla con MOTIVO_REQUERIDO")
    void anular_sinMotivo_motivoRequerido(String motivo) {
        var ventaId = ventaRegistrada();

        fallaCon(() -> e.api.preparar(encargado, new AnularVentaComando(ventaId, motivo)),
                CodigoError.MOTIVO_REQUERIDO);
    }

    @Test
    @DisplayName("un motivo de mas de 200 caracteres falla con MOTIVO_REQUERIDO")
    void anular_motivoLarguisimo_motivoRequerido() {
        var ventaId = ventaRegistrada();

        fallaCon(() -> e.api.preparar(encargado, new AnularVentaComando(ventaId, "x".repeat(201))),
                CodigoError.MOTIVO_REQUERIDO);
    }

    @Test
    @DisplayName("si otra peticion cambio la venta, falla con CONCURRENCIA y no publica")
    void anular_concurrencia() {
        var ventaId = ventaRegistrada();
        var token = e.api.preparar(encargado, new AnularVentaComando(ventaId, "Error")).token();
        e.ventas.simularConcurrencia = true;

        fallaCon(() -> e.api.confirmar(encargado, token), CodigoError.CONCURRENCIA);
        assertThat(e.eventos.deTipo(VentaAnulada.TIPO)).isEmpty();
    }
}
