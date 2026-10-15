package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import mx.negocio360.contabilidad.dominio.ResultadoEvento.Resultado;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Eventos;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** E-01: deduplicacion por evento_id en evento_procesado. */
class IdempotenciaEventoTest {

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    @Test
    @DisplayName("E-01: el mismo evento entregado tres veces produce una sola poliza y dos OMITIDO")
    void mismoEvento_tresVeces_unaPoliza() {
        var evento = Eventos.venta(e.negocioA, "232.00", "EFECTIVO", Fixtures.AHORA);

        var primero = e.api.contabilizarVenta(evento);
        var segundo = e.api.contabilizarVenta(evento);
        var tercero = e.api.contabilizarVenta(evento);

        assertThat(primero.resultado()).isEqualTo(Resultado.APLICADO);
        assertThat(segundo.resultado()).isEqualTo(Resultado.OMITIDO);
        assertThat(tercero.resultado()).isEqualTo(Resultado.OMITIDO);
        assertThat(segundo.motivo()).isEqualTo("YA_PROCESADO");
        assertThat(e.polizas.delNegocio(e.negocioA)).hasSize(1);
    }

    @Test
    @DisplayName("E-01: el evento queda registrado en evento_procesado con su tipo, version y resultado")
    void evento_quedaRegistrado() {
        var evento = Eventos.venta(e.negocioA, "10.00", "EFECTIVO", Fixtures.AHORA);

        e.api.contabilizarVenta(evento);

        var procesado = e.eventosProcesados.de(e.negocioA, evento.id()).orElseThrow();
        assertThat(procesado.tipo()).isEqualTo("venta.registrada");
        assertThat(procesado.version()).isEqualTo(1);
        assertThat(procesado.resultado()).isEqualTo(Resultado.APLICADO);
        assertThat(procesado.recibidoEn()).isEqualTo(Fixtures.AHORA);
    }

    @Test
    @DisplayName("E-01: si contabilizar falla, el evento no queda marcado y la reentrega lo intenta de nuevo")
    void falla_noQuedaMarcado() {
        var evento = Eventos.venta(e.negocioA, "10.00", "EFECTIVO", Fixtures.AHORA);
        e.polizas.fallarAlInsertar = true;

        try {
            e.api.contabilizarVenta(evento);
        } catch (IllegalStateException esperado) {
            // la base estaba caida
        }
        e.polizas.fallarAlInsertar = false;

        assertThat(e.eventosProcesados.de(e.negocioA, evento.id())).isEmpty();
        assertThat(e.api.contabilizarVenta(evento).resultado()).isEqualTo(Resultado.APLICADO);
        assertThat(e.polizas.delNegocio(e.negocioA)).hasSize(1);
    }
}
