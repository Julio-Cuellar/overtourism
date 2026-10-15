package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import mx.negocio360.contabilidad.dominio.EstadoPendiente;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Eventos;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * E-03: el negocio sale del sobre del evento. Un evento del negocio A no lee ni escribe nada del negocio B. La parte
 * de base (set_config y seguridad por fila) esta en {@code AislamientoContabilidadPostgresTest}.
 */
class NegocioDelSobreTest {

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
        e.activar(e.ctxB);
    }

    @Test
    @DisplayName("E-03: una venta del negocio A se registra en A con las cuentas de A y B no ve nada")
    void eventoDeA_soloTocaA() {
        var evento = Eventos.venta(e.negocioA, "232.00", "EFECTIVO", Fixtures.AHORA);
        var resultado = e.api.contabilizarVenta(evento);

        var poliza = e.api.poliza(e.ctxA, resultado.polizaId()).orElseThrow();
        var cuentasDeA = e.api.catalogo(e.ctxA).stream().map(c -> c.id()).toList();
        assertThat(poliza.negocioId()).isEqualTo(e.negocioA);
        assertThat(poliza.renglones()).allMatch(r -> cuentasDeA.contains(r.cuentaId()));
        assertThat(e.api.poliza(e.ctxB, resultado.polizaId())).isEmpty();
        assertThat(e.api.balanza(e.ctxB, YearMonth.from(Fixtures.HOY)).totalCargos()).isEqualByComparingTo("0");
        assertThat(e.eventosProcesados.de(e.negocioB, evento.id())).isEmpty();
        assertThat(e.eventosProcesados.de(e.negocioA, evento.id())).isPresent();
    }

    @Test
    @DisplayName("E-03: un evento de un negocio sin contabilidad activa no deja nada en otro negocio")
    void eventoDeNegocioInactivo_noTocaAOtro() {
        var otro = java.util.UUID.randomUUID();

        e.api.contabilizarVenta(Eventos.venta(otro, "10.00", "EFECTIVO", Fixtures.AHORA));

        assertThat(e.polizas.delNegocio(e.negocioA)).isEmpty();
        assertThat(e.polizas.delNegocio(e.negocioB)).isEmpty();
        assertThat(e.api.pendientes(e.ctxA, EstadoPendiente.ABIERTO)).isEmpty();
        assertThat(e.api.pendientes(e.ctxB, EstadoPendiente.ABIERTO)).isEmpty();
    }
}
