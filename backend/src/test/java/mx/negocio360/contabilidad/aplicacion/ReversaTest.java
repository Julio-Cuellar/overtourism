package mx.negocio360.contabilidad.aplicacion;

import static mx.negocio360.contabilidad.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.OrigenPoliza;
import mx.negocio360.contabilidad.dominio.TipoPoliza;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A-06: una poliza se revierte una sola vez; la reversa invierte cada renglon y cae en el periodo abierto. */
class ReversaTest {

    private final Escenario e = new Escenario();
    private UUID original;

    @BeforeEach
    void registrarVenta() {
        e.activar(e.ctxA);
        original = e.confirmar(e.ctxA, new RegistrarPolizaManualComando(TipoPoliza.INGRESO, Fixtures.HOY, "Venta",
                List.of(new RenglonManual(e.cuenta(e.ctxA, "101.01").id(), Fixtures.m("232.00"), null, null),
                        new RenglonManual(e.cuenta(e.ctxA, "401.01").id(), null, Fixtures.m("200.00"), null),
                        new RenglonManual(e.cuenta(e.ctxA, "208.01").id(), null, Fixtures.m("32.00"), null))))
                .recursoId();
    }

    @Test
    @DisplayName("A-06: la reversa invierte cada renglon, apunta a la original y deja los saldos en cero")
    void reversar_invierteRenglones() {
        var id = e.confirmar(e.ctxA, new ReversarPolizaComando(original, "Venta anulada en caja")).recursoId();

        var reversa = e.api.poliza(e.ctxA, id).orElseThrow();
        var poliza = e.api.poliza(e.ctxA, original).orElseThrow();
        assertThat(reversa.origen()).isEqualTo(OrigenPoliza.REVERSA);
        assertThat(reversa.reviertePolizaId()).isEqualTo(original);
        assertThat(reversa.tipo()).isEqualTo(poliza.tipo());
        assertThat(reversa.total()).isEqualByComparingTo("232.00");
        assertThat(reversa.concepto()).contains("Venta anulada en caja");
        for (var i = 0; i < poliza.renglones().size(); i++) {
            assertThat(reversa.renglones().get(i).cuentaId()).isEqualTo(poliza.renglones().get(i).cuentaId());
            assertThat(reversa.renglones().get(i).cargo()).isEqualTo(poliza.renglones().get(i).abono());
            assertThat(reversa.renglones().get(i).abono()).isEqualTo(poliza.renglones().get(i).cargo());
        }
        var balanza = e.api.balanza(e.ctxA, YearMonth.from(Fixtures.HOY));
        assertThat(balanza.fila("101.01").saldoFinal()).isEqualByComparingTo("0");
        assertThat(balanza.fila("401.01").saldoFinal()).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("A-06: revertir dos veces falla con POLIZA_YA_REVERTIDA")
    void reversar_dosVeces_falla() {
        var token = e.api.preparar(e.ctxA, new ReversarPolizaComando(original, "Error de captura")).token();
        e.confirmar(e.ctxA, new ReversarPolizaComando(original, "Error de captura"));

        fallaCon(() -> e.api.preparar(e.ctxA, new ReversarPolizaComando(original, "Otra vez")),
                CodigoError.POLIZA_YA_REVERTIDA);
        fallaCon(() -> e.api.confirmar(e.ctxA, token), CodigoError.POLIZA_YA_REVERTIDA);
    }

    @Test
    @DisplayName("A-06: la reversa cae en el periodo abierto de hoy aunque la original sea de un mes cerrado")
    void reversar_originalDeMesCerrado_caeEnElPeriodoAbierto() {
        var septiembre = e.confirmar(e.ctxA, new RegistrarPolizaManualComando(TipoPoliza.DIARIO,
                LocalDate.of(2026, 9, 5), "Ajuste", List.of(
                        new RenglonManual(e.cuenta(e.ctxA, "102.01").id(), Fixtures.m("10.00"), null, null),
                        new RenglonManual(e.cuenta(e.ctxA, "101.01").id(), null, Fixtures.m("10.00"), null))))
                .recursoId();
        var periodo = e.periodos.delMes(e.negocioA, YearMonth.of(2026, 9)).orElseThrow();
        e.periodos.guardar(new mx.negocio360.contabilidad.dominio.Periodo(periodo.id(), periodo.negocioId(),
                periodo.mes(), mx.negocio360.contabilidad.dominio.EstadoPeriodo.CERRADO, periodo.abiertoEn(),
                Fixtures.AHORA, "locatario-a", 0, 1));

        var id = e.confirmar(e.ctxA, new ReversarPolizaComando(septiembre, "Ajuste mal capturado")).recursoId();

        assertThat(e.api.poliza(e.ctxA, id).orElseThrow().fecha()).isEqualTo(Fixtures.HOY);
    }

    @Test
    @DisplayName("A-06: sin motivo falla con MOTIVO_REQUERIDO")
    void reversar_sinMotivo_falla() {
        fallaCon(() -> e.api.preparar(e.ctxA, new ReversarPolizaComando(original, "  ")),
                CodigoError.MOTIVO_REQUERIDO);
    }

    @Test
    @DisplayName("A-06: una poliza inexistente o de otro negocio falla con POLIZA_NO_ENCONTRADA")
    void reversar_polizaAjena_falla() {
        e.activar(e.ctxB);

        fallaCon(() -> e.api.preparar(e.ctxB, new ReversarPolizaComando(original, "x")),
                CodigoError.POLIZA_NO_ENCONTRADA);
        fallaCon(() -> e.api.preparar(e.ctxA, new ReversarPolizaComando(UUID.randomUUID(), "x")),
                CodigoError.POLIZA_NO_ENCONTRADA);
    }
}
