package mx.negocio360.contabilidad.aplicacion;

import static mx.negocio360.contabilidad.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.EstadoPeriodo;
import mx.negocio360.contabilidad.dominio.Periodo;
import mx.negocio360.contabilidad.dominio.TipoPoliza;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** T-01: un periodo por mes; solo los periodos abiertos reciben polizas. */
class PeriodoAbiertoTest {

    private static final YearMonth SEPTIEMBRE = YearMonth.of(2026, 9);

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    private RegistrarPolizaManualComando poliza(LocalDate fecha) {
        return new RegistrarPolizaManualComando(TipoPoliza.DIARIO, fecha, "Ajuste", List.of(
                new RenglonManual(e.cuenta(e.ctxA, "102.01").id(), Fixtures.m("20.00"), null, null),
                new RenglonManual(e.cuenta(e.ctxA, "101.01").id(), null, Fixtures.m("20.00"), null)));
    }

    private void cerrar(YearMonth mes) {
        var periodo = e.periodos.delMes(e.negocioA, mes).orElseThrow();
        e.periodos.guardar(new Periodo(periodo.id(), periodo.negocioId(), periodo.mes(), EstadoPeriodo.CERRADO,
                periodo.abiertoEn(), Fixtures.AHORA, "locatario-a", 0, periodo.version() + 1));
    }

    @Test
    @DisplayName("T-01: una poliza manual con fecha de un mes cerrado falla con PERIODO_CERRADO al preparar")
    void manual_enMesCerrado_fallaAlPreparar() {
        e.confirmar(e.ctxA, poliza(LocalDate.of(2026, 9, 10)));
        cerrar(SEPTIEMBRE);

        fallaCon(() -> e.api.preparar(e.ctxA, poliza(LocalDate.of(2026, 9, 20))), CodigoError.PERIODO_CERRADO);
    }

    @Test
    @DisplayName("T-01: si el mes se cierra entre preparar y confirmar, confirmar falla con PERIODO_CERRADO")
    void manual_mesCerradoAntesDeConfirmar_falla() {
        e.confirmar(e.ctxA, poliza(LocalDate.of(2026, 9, 10)));
        var token = e.api.preparar(e.ctxA, poliza(LocalDate.of(2026, 9, 20))).token();
        cerrar(SEPTIEMBRE);

        fallaCon(() -> e.api.confirmar(e.ctxA, token), CodigoError.PERIODO_CERRADO);
        assertThat(e.polizas.delNegocio(e.negocioA)).hasSize(1);
    }

    @Test
    @DisplayName("T-01: dos polizas del mismo mes caen en el mismo y unico periodo")
    void unPeriodoPorMes() {
        var primera = e.confirmar(e.ctxA, poliza(LocalDate.of(2026, 10, 1))).recursoId();
        var segunda = e.confirmar(e.ctxA, poliza(LocalDate.of(2026, 10, 31))).recursoId();

        assertThat(e.api.poliza(e.ctxA, primera).orElseThrow().periodoId())
                .isEqualTo(e.api.poliza(e.ctxA, segunda).orElseThrow().periodoId());
        assertThat(e.periodos.delNegocio(e.negocioA)).hasSize(1);
    }

    @Test
    @DisplayName("T-01: el periodo de un negocio no afecta al de otro")
    void periodoPorNegocio() {
        e.activar(e.ctxB);
        e.confirmar(e.ctxA, poliza(LocalDate.of(2026, 9, 10)));
        cerrar(SEPTIEMBRE);

        var comandoB = new RegistrarPolizaManualComando(TipoPoliza.DIARIO, LocalDate.of(2026, 9, 12), "Ajuste",
                List.of(new RenglonManual(e.cuenta(e.ctxB, "102.01").id(), Fixtures.m("1.00"), null, null),
                        new RenglonManual(e.cuenta(e.ctxB, "101.01").id(), null, Fixtures.m("1.00"), null)));
        e.confirmar(e.ctxB, comandoB);

        assertThat(e.periodos.delMes(e.negocioB, SEPTIEMBRE).orElseThrow().abierto()).isTrue();
    }
}
