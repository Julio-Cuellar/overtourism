package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import mx.negocio360.contabilidad.dominio.ResultadoEvento.Resultado;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Eventos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** T-05: un evento de un periodo cerrado entra en el periodo abierto mas antiguo, el dia 1, como extemporaneo. */
class ExtemporaneaTest {

    private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);
    private static final YearMonth NOVIEMBRE = YearMonth.of(2026, 11);
    private static final Instant TREINTA_Y_UNO_DE_OCTUBRE = Instant.parse("2026-10-31T20:00:00Z");

    private final Escenario e = new Escenario();

    @BeforeEach
    void cerrarOctubre() {
        e.activar(e.ctxA);
        e.api.abrirPeriodo(e.negocioA, OCTUBRE);
        e.confirmar(e.ctxA, new CerrarPeriodoComando(OCTUBRE));
    }

    @Test
    @DisplayName("T-05: venta del 31 de octubre con octubre cerrado: poliza del 1 de noviembre, extemporanea")
    void eventoDeMesCerrado_caeEnElAbiertoMasAntiguo() {
        var resultado = e.api.contabilizarVenta(
                Eventos.venta(e.negocioA, "100.00", "EFECTIVO", TREINTA_Y_UNO_DE_OCTUBRE));

        assertThat(resultado.resultado()).isEqualTo(Resultado.APLICADO);
        var poliza = e.api.poliza(e.ctxA, resultado.polizaId()).orElseThrow();
        assertThat(poliza.extemporanea()).isTrue();
        assertThat(poliza.fecha()).isEqualTo(LocalDate.of(2026, 11, 1));
        assertThat(poliza.periodoId()).isEqualTo(e.periodos.delMes(e.negocioA, NOVIEMBRE).orElseThrow().id());
        assertThat(poliza.concepto()).contains("2026-10-31");
    }

    @Test
    @DisplayName("T-05: los saldos de octubre no cambian; la venta suma en noviembre")
    void eventoDeMesCerrado_noTocaElMesCerrado() {
        e.api.contabilizarVenta(Eventos.venta(e.negocioA, "100.00", "EFECTIVO", TREINTA_Y_UNO_DE_OCTUBRE));

        assertThat(e.api.balanza(e.ctxA, OCTUBRE).fila("101.01").saldoFinal()).isEqualByComparingTo("0");
        assertThat(e.api.balanza(e.ctxA, NOVIEMBRE).fila("101.01").saldoFinal()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("T-05: una venta del mes abierto no es extemporanea")
    void eventoDeMesAbierto_noEsExtemporaneo() {
        var poliza = e.api.poliza(e.ctxA, e.api.contabilizarVenta(Eventos.venta(e.negocioA, "5.00", "EFECTIVO",
                Instant.parse("2026-11-04T18:00:00Z"))).polizaId()).orElseThrow();

        assertThat(poliza.extemporanea()).isFalse();
        assertThat(poliza.fecha()).isEqualTo(LocalDate.of(2026, 11, 4));
    }
}
