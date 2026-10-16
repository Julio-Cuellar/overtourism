package mx.negocio360.caja.aplicacion;

import mx.negocio360.compartido.pruebas.Permisos;
import static mx.negocio360.caja.fakes.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import mx.negocio360.caja.FiltroCortes;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
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

        assertThat(e.api.cajas(e.ctxA, Permisos.de(e.ctxA))).extracting(c -> c.nombre()).containsExactly("Taquilla", "Mostrador");
    }

    @Test
    @DisplayName("el saldo de un turno de otro negocio o inexistente falla con TURNO_NO_ENCONTRADO")
    void saldo_turnoAjeno_turnoNoEncontrado() {
        var turno = e.abrirTurno(e.ctxA, "500.00");

        fallaCon(() -> e.api.saldoTurno(e.ctxB, Permisos.de(e.ctxB), turno), CodigoError.TURNO_NO_ENCONTRADO);
        fallaCon(() -> e.api.saldoTurno(e.ctxA, Permisos.de(e.ctxA), UUID.randomUUID()), CodigoError.TURNO_NO_ENCONTRADO);
    }

    @Test
    @DisplayName("sin cortes, la lista de cortes esta vacia")
    void cortes_vacio() {
        assertThat(e.api.cortes(e.ctxA, Permisos.TODOS, null, 20, null).elementos()).isEmpty();
    }

    @Test
    @DisplayName("D-131: un negocio no puede tener mas de 50 cajas; la 51 falla con DEMASIADAS_CAJAS")
    void crearCaja_tope50() {
        for (var i = 1; i <= 50; i++) {
            e.crearCaja(e.ctxA, "Caja " + i);
        }

        fallaCon(() -> e.api.preparar(e.ctxA, Permisos.TODOS, new CrearCajaComando("Caja 51")),
                CodigoError.DEMASIADAS_CAJAS);
        assertThat(e.api.cajas(e.ctxA, Permisos.CAJERO)).hasSize(50);
        e.crearCaja(e.ctxB, "Otra de otro negocio");
    }

    @Test
    @DisplayName("D-126: cajas, turnoActivo, saldoTurno y movimientos piden caja.turno.ver; cortes pide caja.corte.ver")
    void consultas_sinPermiso_permisoDenegado() {
        var turno = e.abrirTurno(e.ctxA, "500.00");
        var sinVer = Permisos.todosMenos("caja.turno.ver");

        assertThatThrownBy(() -> e.api.cajas(e.ctxA, sinVer)).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> e.api.turnoActivo(e.ctxA, sinVer, UUID.randomUUID()))
                .isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> e.api.saldoTurno(e.ctxA, sinVer, turno)).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> e.api.movimientos(e.ctxA, sinVer, turno, 20, null))
                .isInstanceOf(PermisoDenegadoException.class);
        // El cajero ve sus turnos pero no audita los cortes pasados.
        assertThatThrownBy(() -> e.api.cortes(e.ctxA, Permisos.CAJERO, null, 20, null))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("movimientos pagina el libro del turno con cursor, en orden de registro, y no mezcla negocios")
    void movimientos_paginaConCursor() {
        var turno = e.abrirTurno(e.ctxA, "500.00");
        for (var i = 1; i <= 3; i++) {
            e.confirmar(e.ctxA, new RegistrarMovimientoCajaComando(turno, TipoMovimientoManual.ENTRADA,
                    new BigDecimal(i + ".00"), "Entrada " + i));
        }

        var primera = e.api.movimientos(e.ctxA, Permisos.CAJERO, turno, 3, null);
        var segunda = e.api.movimientos(e.ctxA, Permisos.CAJERO, turno, 3, primera.cursorFinal());

        assertThat(primera.elementos()).extracting(m -> m.monto().toPlainString())
                .containsExactly("500.00", "1.00", "2.00");
        assertThat(segunda.elementos()).extracting(m -> m.monto().toPlainString()).containsExactly("3.00");
        assertThat(segunda.hayMas()).isFalse();
        assertThat(e.api.movimientos(e.ctxB, Permisos.CAJERO, turno, 20, null).elementos()).isEmpty();
        fallaCon(() -> e.api.movimientos(e.ctxA, Permisos.CAJERO, turno, 0, null), CodigoError.PAGINACION_INVALIDA);
    }

    @Test
    @DisplayName("cortes rechaza rangos de mas de 92 dias y tamanos invalidos, y filtra por caja")
    void cortes_filtrosYPaginacion() {
        var ahora = e.reloj.instant();

        fallaCon(() -> e.api.cortes(e.ctxA, Permisos.TODOS,
                new FiltroCortes(null, ahora.minus(Duration.ofDays(93)), ahora), 20, null),
                CodigoError.FILTRO_INVALIDO);
        fallaCon(() -> e.api.cortes(e.ctxA, Permisos.TODOS, null, 101, null), CodigoError.PAGINACION_INVALIDA);
        assertThat(e.api.cortes(e.ctxA, Permisos.TODOS, new FiltroCortes(UUID.randomUUID(), null, null), 20, null)
                .elementos()).isEmpty();
    }
}
