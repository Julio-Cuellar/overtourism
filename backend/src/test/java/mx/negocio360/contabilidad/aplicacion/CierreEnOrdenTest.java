package mx.negocio360.contabilidad.aplicacion;

import static mx.negocio360.contabilidad.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.EstadoPeriodo;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** T-02: los periodos se cierran en orden. */
class CierreEnOrdenTest {

    private static final YearMonth SEPTIEMBRE = YearMonth.of(2026, 9);
    private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);
    private static final YearMonth NOVIEMBRE = YearMonth.of(2026, 11);

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    private EstadoPeriodo estado(YearMonth mes) {
        return e.periodos.delMes(e.negocioA, mes).orElseThrow().estado();
    }

    @Test
    @DisplayName("T-02: cerrar octubre con septiembre abierto falla con PERIODO_ANTERIOR_ABIERTO")
    void cerrar_conAnteriorAbierto_falla() {
        e.api.abrirPeriodo(e.negocioA, SEPTIEMBRE);
        e.api.abrirPeriodo(e.negocioA, OCTUBRE);

        fallaCon(() -> e.api.preparar(e.ctxA, new CerrarPeriodoComando(OCTUBRE)),
                CodigoError.PERIODO_ANTERIOR_ABIERTO);
    }

    @Test
    @DisplayName("T-02: en orden si se cierra; queda CERRADO con quien y cuando, y se abre el mes siguiente")
    void cerrar_enOrden() {
        e.api.abrirPeriodo(e.negocioA, SEPTIEMBRE);
        e.api.abrirPeriodo(e.negocioA, OCTUBRE);

        e.confirmar(e.ctxA, new CerrarPeriodoComando(SEPTIEMBRE));
        e.confirmar(e.ctxA, new CerrarPeriodoComando(OCTUBRE));

        var octubre = e.periodos.delMes(e.negocioA, OCTUBRE).orElseThrow();
        assertThat(octubre.estado()).isEqualTo(EstadoPeriodo.CERRADO);
        assertThat(octubre.cerradoPor()).isEqualTo("locatario-a");
        assertThat(octubre.cerradoEn()).isEqualTo(Fixtures.AHORA);
        assertThat(estado(NOVIEMBRE)).isEqualTo(EstadoPeriodo.ABIERTO);
    }

    @Test
    @DisplayName("T-02: cerrar un mes ya cerrado falla con PERIODO_CERRADO; uno que no existe con PERIODO_NO_ENCONTRADO")
    void cerrar_cerradoOInexistente_falla() {
        e.api.abrirPeriodo(e.negocioA, SEPTIEMBRE);
        e.confirmar(e.ctxA, new CerrarPeriodoComando(SEPTIEMBRE));

        fallaCon(() -> e.api.preparar(e.ctxA, new CerrarPeriodoComando(SEPTIEMBRE)), CodigoError.PERIODO_CERRADO);
        fallaCon(() -> e.api.preparar(e.ctxA, new CerrarPeriodoComando(YearMonth.of(2025, 1))),
                CodigoError.PERIODO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("T-02: el periodo de otro negocio no cuenta para el orden")
    void cerrar_otroNegocioNoCuenta() {
        e.activar(e.ctxB);
        e.api.abrirPeriodo(e.negocioB, SEPTIEMBRE);
        e.api.abrirPeriodo(e.negocioA, OCTUBRE);

        e.confirmar(e.ctxA, new CerrarPeriodoComando(OCTUBRE));

        assertThat(estado(OCTUBRE)).isEqualTo(EstadoPeriodo.CERRADO);
    }
}
