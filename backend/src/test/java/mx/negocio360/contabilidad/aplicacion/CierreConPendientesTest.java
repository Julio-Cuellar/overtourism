package mx.negocio360.contabilidad.aplicacion;

import static mx.negocio360.contabilidad.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.YearMonth;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.EstadoPendiente;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Eventos;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** T-03: no se cierra un periodo con pendientes abiertos de ese mes. */
class CierreConPendientesTest {

    private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
        e.api.abrirPeriodo(e.negocioA, OCTUBRE);
    }

    @Test
    @DisplayName("T-03: con un pendiente de octubre, cerrar octubre falla con PENDIENTES_SIN_RESOLVER")
    void cerrar_conPendienteDelMes_falla() {
        e.api.contabilizarVenta(Eventos.venta(e.negocioA, "80.00", "VALES", Fixtures.AHORA));

        fallaCon(() -> e.api.preparar(e.ctxA, new CerrarPeriodoComando(OCTUBRE)),
                CodigoError.PENDIENTES_SIN_RESOLVER);
    }

    @Test
    @DisplayName("T-03: al resolver el pendiente, octubre si se cierra")
    void cerrar_trasResolver_seCierra() {
        e.api.contabilizarVenta(Eventos.venta(e.negocioA, "80.00", "VALES", Fixtures.AHORA));
        var pendiente = e.api.pendientes(e.ctxA, EstadoPendiente.ABIERTO).get(0);
        e.confirmar(e.ctxA, ResolverPendienteComando.descartar(pendiente.id(), "Era una prueba"));

        e.confirmar(e.ctxA, new CerrarPeriodoComando(OCTUBRE));

        assertThat(e.periodos.delMes(e.negocioA, OCTUBRE).orElseThrow().abierto()).isFalse();
    }

    @Test
    @DisplayName("T-03: un pendiente de noviembre no impide cerrar octubre")
    void cerrar_pendienteDeOtroMes_noImpide() {
        e.api.contabilizarVenta(Eventos.venta(e.negocioA, "80.00", "VALES", Instant.parse("2026-11-03T18:00:00Z")));

        e.confirmar(e.ctxA, new CerrarPeriodoComando(OCTUBRE));

        assertThat(e.periodos.delMes(e.negocioA, OCTUBRE).orElseThrow().abierto()).isFalse();
    }

    @Test
    @DisplayName("T-03: si llega un pendiente entre preparar y confirmar, confirmar falla")
    void cerrar_pendienteEntrePrepararYConfirmar_falla() {
        var token = e.api.preparar(e.ctxA, new CerrarPeriodoComando(OCTUBRE)).token();
        e.api.contabilizarVenta(Eventos.venta(e.negocioA, "80.00", "VALES", Fixtures.AHORA));

        fallaCon(() -> e.api.confirmar(e.ctxA, token), CodigoError.PENDIENTES_SIN_RESOLVER);
    }
}
