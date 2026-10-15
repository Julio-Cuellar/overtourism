package mx.negocio360.caja.aplicacion;

import static mx.negocio360.caja.fakes.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import mx.negocio360.caja.dominio.CodigoError;
import mx.negocio360.caja.fakes.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** CF-02 a CF-04: consultas de cajas, saldo de turno y cortes, aisladas por negocio. */
class ConsultasCajaTest {

    private final Escenario e = new Escenario();

    @Test
    @DisplayName("cajas lista solo las del negocio, en orden de alta")
    void cajas_soloDelNegocio() {
        e.crearCaja(e.ctxA, "Taquilla");
        e.crearCaja(e.ctxA, "Mostrador");
        e.crearCaja(e.ctxB, "Taquilla");

        assertThat(e.api.cajas(e.ctxA)).extracting(c -> c.nombre()).containsExactly("Taquilla", "Mostrador");
    }

    @Test
    @DisplayName("el saldo de un turno de otro negocio o inexistente falla con TURNO_NO_ENCONTRADO")
    void saldo_turnoAjeno_turnoNoEncontrado() {
        var turno = e.abrirTurno(e.ctxA, "500.00");

        fallaCon(() -> e.api.saldoTurno(e.ctxB, turno), CodigoError.TURNO_NO_ENCONTRADO);
        fallaCon(() -> e.api.saldoTurno(e.ctxA, UUID.randomUUID()), CodigoError.TURNO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("sin cortes, la lista de cortes esta vacia")
    void cortes_vacio() {
        assertThat(e.api.cortes(e.ctxA)).isEmpty();
    }
}
