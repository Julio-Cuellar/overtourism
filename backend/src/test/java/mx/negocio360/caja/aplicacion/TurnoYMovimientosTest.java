package mx.negocio360.caja.aplicacion;

import mx.negocio360.compartido.pruebas.Permisos;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import static mx.negocio360.caja.fakes.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;
import mx.negocio360.caja.dominio.CodigoError;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.caja.dominio.TipoMovimiento;
import mx.negocio360.caja.eventos.TurnoAbierto;
import mx.negocio360.caja.fakes.Escenario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** CF-10, CF-11, CF-14 y CF-01: crear caja, abrir turno y movimientos manuales (CJ-01, CJ-02, CJ-05, CJ-10). */
class TurnoYMovimientosTest {

    private final Escenario e = new Escenario();

    private static BigDecimal d(String valor) {
        return new BigDecimal(valor);
    }

    @Test
    @DisplayName("crear caja: nombre obligatorio y unico por negocio, sin importar mayusculas")
    void crearCaja_nombreUnicoPorNegocio() {
        e.crearCaja(e.ctxA, "Taquilla");

        fallaCon(() -> e.api.preparar(e.ctxA, Permisos.TODOS, new CrearCajaComando("taquilla ")), CodigoError.CAJA_DUPLICADA);
        fallaCon(() -> e.api.preparar(e.ctxA, Permisos.TODOS, new CrearCajaComando("  ")), CodigoError.NOMBRE_CAJA_INVALIDO);
        fallaCon(() -> e.api.preparar(e.ctxA, Permisos.TODOS, new CrearCajaComando("x".repeat(61))),
                CodigoError.NOMBRE_CAJA_INVALIDO);
        assertThat(e.crearCaja(e.ctxB, "Taquilla")).isNotNull();
    }

    @Test
    @DisplayName("CJ-02: abrir turno con 500.00 deja el turno ABIERTO, un movimiento FONDO y publica caja.turno_abierto")
    void abrirTurno_conFondo() {
        var turnoId = e.abrirTurno(e.ctxA, "500.00");

        var saldo = e.api.saldoTurno(e.ctxA, Permisos.de(e.ctxA), turnoId);
        assertThat(saldo.estado()).isEqualTo(EstadoTurno.ABIERTO);
        assertThat(saldo.esperadoPorMetodo()).containsEntry(MetodoPago.EFECTIVO, d("500.00"))
                .containsEntry(MetodoPago.TARJETA, d("0.00")).containsEntry(MetodoPago.TRANSFERENCIA, d("0.00"));
        assertThat(saldo.movimientos()).singleElement().satisfies(m -> {
            assertThat(m.tipo()).isEqualTo(TipoMovimiento.FONDO);
            assertThat(m.monto()).isEqualTo(d("500.00"));
            assertThat(m.registradoPor()).isEqualTo("cajero-a");
        });
        var evento = e.eventos.deTipo(TurnoAbierto.TIPO);
        assertThat(evento).singleElement().satisfies(ev -> {
            assertThat(ev.origen()).isEqualTo("caja");
            assertThat(ev.correlacionId()).isEqualTo(turnoId);
            assertThat(ev.datos()).containsEntry("turno_id", turnoId.toString())
                    .containsEntry("fondo_inicial", "500.00").doesNotContainKey("cajero");
        });
    }

    @Test
    @DisplayName("CJ-02: un fondo de 0.00 se permite y no crea movimiento")
    void abrirTurno_fondoCero_sinMovimiento() {
        var turnoId = e.abrirTurno(e.ctxA, "0");

        assertThat(e.api.saldoTurno(e.ctxA, Permisos.de(e.ctxA), turnoId).movimientos()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1.00", "10.001"})
    @DisplayName("CJ-02: un fondo negativo o con mas de 2 decimales falla con FONDO_INVALIDO")
    void abrirTurno_fondoInvalido(String fondo) {
        var caja = e.crearCaja(e.ctxA, "Taquilla");

        fallaCon(() -> e.api.preparar(e.ctxA, Permisos.de(e.ctxA), new AbrirTurnoComando(caja, d(fondo))), CodigoError.FONDO_INVALIDO);
    }

    @Test
    @DisplayName("CJ-01: una caja no puede tener dos turnos abiertos; tambien al confirmar dos preparados a la vez")
    void abrirTurno_dosVeces_turnoYaAbierto() {
        var caja = e.crearCaja(e.ctxA, "Taquilla");
        var primero = e.api.preparar(e.ctxA, Permisos.de(e.ctxA), new AbrirTurnoComando(caja, d("100"))).token();
        var segundo = e.api.preparar(e.ctxA, Permisos.de(e.ctxA), new AbrirTurnoComando(caja, d("100"))).token();
        e.api.confirmar(e.ctxA, Permisos.de(e.ctxA), primero);

        fallaCon(() -> e.api.confirmar(e.ctxA, Permisos.de(e.ctxA), segundo), CodigoError.TURNO_YA_ABIERTO);
        fallaCon(() -> e.api.preparar(e.ctxA, Permisos.de(e.ctxA), new AbrirTurnoComando(caja, d("100"))), CodigoError.TURNO_YA_ABIERTO);
    }

    @Test
    @DisplayName("abrir turno en una caja de otro negocio falla con CAJA_NO_ENCONTRADA")
    void abrirTurno_cajaAjena() {
        var cajaDeB = e.crearCaja(e.ctxB, "Taquilla");

        fallaCon(() -> e.api.preparar(e.ctxA, Permisos.de(e.ctxA), new AbrirTurnoComando(cajaDeB, d("100"))),
                CodigoError.CAJA_NO_ENCONTRADA);
    }

    @Test
    @DisplayName("turnoActivo devuelve el turno abierto de la caja, y vacio si no hay")
    void turnoActivo() {
        var caja = e.crearCaja(e.ctxA, "Taquilla");
        assertThat(e.api.turnoActivo(e.ctxA, Permisos.de(e.ctxA), caja)).isEmpty();

        var turnoId = e.confirmar(e.ctxA, new AbrirTurnoComando(caja, d("100"))).recursoId();

        assertThat(e.api.turnoActivo(e.ctxA, Permisos.de(e.ctxA), caja)).hasValueSatisfying(t -> assertThat(t.id()).isEqualTo(turnoId));
        assertThat(e.api.turnoActivo(e.ctxB, Permisos.de(e.ctxB), caja)).isEmpty();
    }

    @Test
    @DisplayName("entrada y retiro cambian el efectivo esperado: 500 + 200 - 300 = 400")
    void movimientos_entradaYRetiro() {
        var turnoId = e.abrirTurno(e.ctxA, "500.00");

        e.confirmar(e.ctxA, new RegistrarMovimientoCajaComando(turnoId, TipoMovimientoManual.ENTRADA, d("200.00"),
                "Cambio del banco"));
        e.confirmar(e.ctxA, Permisos.TODOS, new RegistrarMovimientoCajaComando(turnoId, TipoMovimientoManual.RETIRO,
                d("300.00"), "Deposito"));

        var saldo = e.api.saldoTurno(e.ctxA, Permisos.de(e.ctxA), turnoId);
        assertThat(saldo.esperadoPorMetodo()).containsEntry(MetodoPago.EFECTIVO, d("400.00"));
        assertThat(saldo.movimientos()).extracting(m -> m.monto()).containsExactly(d("500.00"), d("200.00"),
                d("-300.00"));
    }

    @Test
    @DisplayName("CJ-05: retirar mas del efectivo esperado falla con RETIRO_MAYOR_AL_EFECTIVO")
    void retiro_mayorAlEfectivo() {
        var turnoId = e.abrirTurno(e.ctxA, "500.00");

        fallaCon(() -> e.api.preparar(e.ctxA, Permisos.TODOS, new RegistrarMovimientoCajaComando(turnoId,
                TipoMovimientoManual.RETIRO, d("500.01"), "Deposito")), CodigoError.RETIRO_MAYOR_AL_EFECTIVO);
    }

    @Test
    @DisplayName("D-127: un cajero mete efectivo pero no lo retira, no crea cajas y no abre turno sin su permiso")
    void permisos_cajeroNoRetiraNiCreaCajas() {
        var turnoId = e.abrirTurno(e.ctxA, "500.00");

        assertThatThrownBy(() -> e.api.preparar(e.ctxA, Permisos.CAJERO, new RegistrarMovimientoCajaComando(turnoId,
                TipoMovimientoManual.RETIRO, d("10.00"), "Deposito"))).isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> e.api.preparar(e.ctxA, Permisos.CAJERO, new CrearCajaComando("Otra")))
                .isInstanceOf(PermisoDenegadoException.class);
        assertThatThrownBy(() -> e.api.preparar(e.ctxA, Permisos.todosMenos("caja.turno.abrir"),
                new AbrirTurnoComando(UUID.randomUUID(), d("1.00")))).isInstanceOf(PermisoDenegadoException.class);
        // Meter efectivo si puede.
        e.confirmar(e.ctxA, Permisos.CAJERO, new RegistrarMovimientoCajaComando(turnoId, TipoMovimientoManual.ENTRADA,
                d("10.00"), "Cambio"));
    }

    @Test
    @DisplayName("D-126: si le quitan retiro.registrar entre preparar y confirmar, falla y el token no se gasta")
    void permisos_retiroRevocadoEntrePasos_noGastaElToken() {
        var turnoId = e.abrirTurno(e.ctxA, "500.00");
        var token = e.api.preparar(e.ctxA, Permisos.TODOS, new RegistrarMovimientoCajaComando(turnoId,
                TipoMovimientoManual.RETIRO, d("100.00"), "Deposito")).token();

        assertThatThrownBy(() -> e.api.confirmar(e.ctxA, Permisos.CAJERO, token))
                .isInstanceOf(PermisoDenegadoException.class);

        e.api.confirmar(e.ctxA, Permisos.TODOS, token);
        assertThat(e.api.saldoTurno(e.ctxA, Permisos.TODOS, turnoId).esperadoPorMetodo())
                .containsEntry(MetodoPago.EFECTIVO, d("400.00"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    @DisplayName("un movimiento manual sin nota falla con NOTA_REQUERIDA")
    void movimiento_sinNota(String nota) {
        var turnoId = e.abrirTurno(e.ctxA, "500.00");

        fallaCon(() -> e.api.preparar(e.ctxA, Permisos.de(e.ctxA), new RegistrarMovimientoCajaComando(turnoId,
                TipoMovimientoManual.ENTRADA, d("10.00"), nota)), CodigoError.NOTA_REQUERIDA);
    }

    @Test
    @DisplayName("un movimiento con monto cero, negativo o de mas de 2 decimales falla con MONTO_INVALIDO")
    void movimiento_montoInvalido() {
        var turnoId = e.abrirTurno(e.ctxA, "500.00");

        for (var monto : new String[] {"0", "-5", "1.001"}) {
            fallaCon(() -> e.api.preparar(e.ctxA, Permisos.de(e.ctxA), new RegistrarMovimientoCajaComando(turnoId,
                    TipoMovimientoManual.ENTRADA, d(monto), "x")), CodigoError.MONTO_INVALIDO);
        }
    }
}
