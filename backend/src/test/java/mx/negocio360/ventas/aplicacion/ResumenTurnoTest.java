package mx.negocio360.ventas.aplicacion;

import mx.negocio360.compartido.pruebas.Permisos;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.ventas.fakes.Escenario;
import mx.negocio360.ventas.fakes.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** VF-03 resumenTurno: lo que Caja necesita para el corte (CJ-06, CJ-07). */
class ResumenTurnoTest {

    private final Escenario e = new Escenario();

    @Test
    @DisplayName("un turno sin ventas da ceros")
    void resumen_turnoVacio_ceros() {
        var resumen = e.api.resumenTurno(e.ctxA, UUID.randomUUID());

        assertThat(resumen.ventasRegistradas()).isZero();
        assertThat(resumen.ventasAnuladas()).isZero();
        assertThat(resumen.preparacionesVigentes()).isZero();
        assertThat(resumen.totalesPorMetodo()).isEmpty();
    }

    @Test
    @DisplayName("cuenta registradas y anuladas y suma por metodo solo lo que sigue registrado")
    void resumen_cuentaYSumaPorMetodo() {
        e.vender(e.ctxA, Fixtures.ventaSinCupon());
        e.vender(e.ctxA, new RegistrarVentaComando(Fixtures.TURNO, Fixtures.dosBoletosYMedioKilo(),
                List.of(Fixtures.tarjeta("100.00"), Fixtures.efectivo("50.00", "50.00")), null, null));
        var anulada = e.vender(e.ctxA, Fixtures.ventaSinCupon());
        e.confirmar(new ContextoNegocio(e.negocioA, "encargado-a"), new AnularVentaComando(anulada, "Error"));

        var resumen = e.api.resumenTurno(e.ctxA, Fixtures.TURNO);

        assertThat(resumen.turnoId()).isEqualTo(Fixtures.TURNO);
        assertThat(resumen.ventasRegistradas()).isEqualTo(2);
        assertThat(resumen.ventasAnuladas()).isEqualTo(1);
        assertThat(resumen.totalesPorMetodo())
                .containsEntry("EFECTIVO", new BigDecimal("200.00"))
                .containsEntry("TARJETA", new BigDecimal("100.00"));
    }

    @Test
    @DisplayName("cuenta las ventas preparadas sin confirmar de ese turno mientras no vencen")
    void resumen_preparacionesVigentes() {
        e.api.preparar(e.ctxA, Permisos.de(e.ctxA), Fixtures.ventaSinCupon());
        e.api.preparar(e.ctxA, Permisos.de(e.ctxA), Fixtures.ventaSinCupon());
        var otroTurno = UUID.randomUUID();
        e.turnos.abrir(e.negocioA, otroTurno);
        e.api.preparar(e.ctxA, Permisos.de(e.ctxA), new RegistrarVentaComando(otroTurno, Fixtures.dosBoletosYMedioKilo(),
                List.of(Fixtures.efectivo("150.00", null)), null, null));

        assertThat(e.api.resumenTurno(e.ctxA, Fixtures.TURNO).preparacionesVigentes()).isEqualTo(2);

        e.reloj.avanzar(Duration.ofMinutes(5));
        assertThat(e.api.resumenTurno(e.ctxA, Fixtures.TURNO).preparacionesVigentes()).isZero();
    }

    @Test
    @DisplayName("el resumen de un negocio no incluye las ventas de otro")
    void resumen_aisladoPorNegocio() {
        e.vender(e.ctxB, Fixtures.ventaSinCupon());

        assertThat(e.api.resumenTurno(e.ctxA, Fixtures.TURNO).ventasRegistradas()).isZero();
    }
}
