package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import java.util.List;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.contabilidad.dominio.TipoPoliza;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Eventos;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * E-08: los eventos propios se publican por el puerto de publicacion, que en produccion escribe en
 * {@code plataforma.event_publication} dentro de la transaccion de la poliza. La parte transaccional (rollback sin
 * fila en el outbox) depende de la capa de infraestructura, fuera de alcance como en Clientes.
 */
class EventosPublicadosTest {

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    private SobreEvento unico(String tipo) {
        var publicados = e.eventos.deTipo(tipo);
        assertThat(publicados).hasSize(1);
        return publicados.get(0);
    }

    @Test
    @DisplayName("E-08: una venta publica poliza.registrada con causa = evento original y correlacion = venta")
    void venta_publicaPolizaRegistrada() {
        var ventaId = java.util.UUID.randomUUID();
        var evento = Eventos.venta(e.negocioA, ventaId, "232.00", "EFECTIVO", Fixtures.AHORA, null);

        var polizaId = e.api.contabilizarVenta(evento).polizaId();

        var sobre = unico("poliza.registrada");
        assertThat(sobre.version()).isEqualTo(1);
        assertThat(sobre.origen()).isEqualTo("contabilidad");
        assertThat(sobre.negocioId()).isEqualTo(e.negocioA);
        assertThat(sobre.causaId()).isEqualTo(evento.id());
        assertThat(sobre.correlacionId()).isEqualTo(ventaId);
        assertThat(sobre.datos()).containsEntry("poliza_id", polizaId.toString())
                .containsEntry("tipo", "INGRESO").containsEntry("folio", 1).containsEntry("origen", "AUTOMATICA")
                .containsEntry("periodo", "2026-10").containsEntry("total", "232.00")
                .containsEntry("referencia_tipo", "VENTA").containsEntry("referencia_id", ventaId.toString());
    }

    @Test
    @DisplayName("E-08: una poliza manual publica poliza.registrada sin causa ni concepto ni actor")
    void manual_publicaSinDatosPersonales() {
        var polizaId = e.confirmar(e.ctxA, new RegistrarPolizaManualComando(TipoPoliza.DIARIO, Fixtures.HOY,
                "Pago a Juan Perez", List.of(
                        new RenglonManual(e.cuenta(e.ctxA, "102.01").id(), Fixtures.m("5.00"), null, null),
                        new RenglonManual(e.cuenta(e.ctxA, "101.01").id(), null, Fixtures.m("5.00"), null))))
                .recursoId();

        var sobre = unico("poliza.registrada");
        assertThat(sobre.causaId()).isNull();
        assertThat(sobre.correlacionId()).isEqualTo(polizaId);
        assertThat(sobre.datos()).doesNotContainKeys("concepto", "creado_por", "referencia_tipo", "referencia_id");
        assertThat(sobre.datos().toString()).doesNotContain("Juan", "locatario-a");
    }

    @Test
    @DisplayName("E-08: un evento que queda pendiente u omitido no publica nada")
    void pendiente_noPublica() {
        var evento = Eventos.venta(e.negocioA, "10.00", "VALES", Fixtures.AHORA);
        e.api.contabilizarVenta(evento);
        e.api.contabilizarVenta(evento);

        assertThat(e.eventos.todos()).isEmpty();
    }

    @Test
    @DisplayName("E-08: cerrar el mes publica periodo.cerrado con ventas netas y resultado del mes")
    void cerrar_publicaPeriodoCerrado() {
        e.api.contabilizarVenta(Eventos.venta(e.negocioA, "232.00", "EFECTIVO", Fixtures.AHORA));
        e.confirmar(e.ctxA, new RegistrarPolizaManualComando(TipoPoliza.DIARIO, Fixtures.HOY, "Faltante", List.of(
                new RenglonManual(e.cuenta(e.ctxA, "601.01").id(), Fixtures.m("15.50"), null, null),
                new RenglonManual(e.cuenta(e.ctxA, "101.01").id(), null, Fixtures.m("15.50"), null))));

        e.confirmar(e.ctxA, new CerrarPeriodoComando(YearMonth.of(2026, 10)));

        var sobre = unico("periodo.cerrado");
        assertThat(sobre.datos()).containsEntry("periodo", "2026-10").containsEntry("ventas_netas", "200.00")
                .containsEntry("resultado", "184.50").containsEntry("cerrado_en", Fixtures.AHORA.toString());
        assertThat(sobre.causaId()).isNull();
    }
}
