package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.YearMonth;
import mx.negocio360.contabilidad.dominio.EstadoPeriodo;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Eventos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** T-04: el periodo siguiente se abre solo, el dia 1 por tarea programada o con el primer evento del mes. */
class AperturaPeriodoTest {

    private static final YearMonth NOVIEMBRE = YearMonth.of(2026, 11);

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    @Test
    @DisplayName("T-04: la tarea del dia 1 abre noviembre; si ya estaba abierto responde YA_ABIERTO")
    void tareaDelDiaUno_abreElPeriodo() {
        assertThat(e.api.abrirPeriodo(e.negocioA, NOVIEMBRE)).isEqualTo("ABIERTO");
        assertThat(e.api.abrirPeriodo(e.negocioA, NOVIEMBRE)).isEqualTo("YA_ABIERTO");

        var periodos = e.api.periodos(e.ctxA, 2026);
        assertThat(periodos).filteredOn(p -> p.mes().equals(NOVIEMBRE)).singleElement()
                .satisfies(p -> assertThat(p.estado()).isEqualTo(EstadoPeriodo.ABIERTO));
    }

    @Test
    @DisplayName("T-04: una venta del 1 de noviembre a las 00:05 en Puebla cae en noviembre aunque la tarea no corra")
    void primerEventoDelMes_abreElPeriodo() {
        var cincoMinutosDespuesDeMedianoche = Instant.parse("2026-11-01T06:05:00Z");

        var poliza = e.api.poliza(e.ctxA, e.api.contabilizarVenta(
                Eventos.venta(e.negocioA, "10.00", "EFECTIVO", cincoMinutosDespuesDeMedianoche)).polizaId())
                .orElseThrow();

        var noviembre = e.periodos.delMes(e.negocioA, NOVIEMBRE).orElseThrow();
        assertThat(poliza.periodoId()).isEqualTo(noviembre.id());
        assertThat(noviembre.abierto()).isTrue();
    }

    @Test
    @DisplayName("T-04: la tarea sin contabilidad activa no abre nada")
    void tarea_sinActivar_noAbre() {
        assertThat(e.api.abrirPeriodo(e.negocioB, NOVIEMBRE)).isEqualTo("CONTABILIDAD_NO_ACTIVA");
        assertThat(e.api.periodos(e.ctxB, 2026)).isEmpty();
    }

    @Test
    @DisplayName("T-04: la consulta de periodos solo trae los del anio pedido y del negocio")
    void periodos_porAnioYNegocio() {
        e.api.abrirPeriodo(e.negocioA, YearMonth.of(2026, 12));
        e.api.abrirPeriodo(e.negocioA, YearMonth.of(2027, 1));
        e.activar(e.ctxB);
        e.api.abrirPeriodo(e.negocioB, YearMonth.of(2026, 12));

        assertThat(e.api.periodos(e.ctxA, 2026)).extracting(p -> p.mes()).containsExactly(YearMonth.of(2026, 12));
    }
}
