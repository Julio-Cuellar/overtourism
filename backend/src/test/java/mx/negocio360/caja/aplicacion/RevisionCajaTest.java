package mx.negocio360.caja.aplicacion;

import static mx.negocio360.caja.fakes.Escenario.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.caja.dominio.CodigoError;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.caja.dominio.TipoMovimiento;
import mx.negocio360.caja.fakes.Escenario;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Correcciones de la revision de seguridad y calidad del 2026-10-15 (Bitacora): Caja no pierde eventos, desconfia de
 * los datos del evento, informa el estado efectivo del turno, verifica turnos para Ventas, acota los montos y no
 * expone texto libre.
 */
class RevisionCajaTest {

    private static final Duration RAPIDO = Duration.ofSeconds(2);

    private final Escenario e = new Escenario();
    private final UUID turno = e.abrirTurno(e.ctxA, "500.00");
    private final UUID venta = UUID.randomUUID();

    private BigDecimal esperado(MetodoPago metodo) {
        return e.api.saldoTurno(e.ctxA, turno).esperadoPorMetodo().get(metodo);
    }

    private boolean procesado(SobreEvento evento) {
        return e.libro.procesado(ConsumidorVentas.CONSUMIDOR, evento.id());
    }

    private void cerrarTurnoDirecto() {
        var actual = e.cajas.turno(e.negocioA, turno).orElseThrow();
        e.cajas.actualizarTurno(actual.cerrado(e.reloj.instant()), actual.version());
    }

    private UUID cajaDelTurno() {
        return e.cajas.turno(e.negocioA, turno).orElseThrow().cajaId();
    }

    // A1: un evento que no se pudo aplicar no se marca como procesado

    @Test
    @DisplayName("A1: una venta de un turno ajeno no se aplica ni se marca, y al reentregarse vuelve a revisarse")
    void sinTurno_noSeMarca() {
        var evento = e.ventaRegistrada(e.negocioB, turno, venta, Map.of("EFECTIVO", "150.00"));

        var primero = e.consumidor.procesar(evento);
        var reentrega = e.consumidor.procesar(evento);

        assertThat(primero).isEqualTo(ResultadoAplicacion.SIN_TURNO_ABIERTO);
        assertThat(primero.completado()).isFalse();
        assertThat(reentrega).isEqualTo(ResultadoAplicacion.SIN_TURNO_ABIERTO);
        assertThat(procesado(evento)).isFalse();
    }

    @Test
    @DisplayName("A1: una venta que llega con el turno ya cerrado no se marca (CJ-11 sigue pendiente)")
    void turnoCerrado_ventaNoSeMarca() {
        cerrarTurnoDirecto();
        var evento = e.ventaRegistrada(e.negocioA, turno, venta, Map.of("EFECTIVO", "150.00"));

        assertThat(e.consumidor.procesar(evento)).isEqualTo(ResultadoAplicacion.SIN_TURNO_ABIERTO);
        assertThat(procesado(evento)).isFalse();
        assertThat(e.libro.todos()).noneMatch(m -> venta.equals(m.ventaId()));
    }

    @Test
    @DisplayName("A1: una anulacion que llega con el turno ya cerrado no se marca")
    void turnoCerrado_anulacionNoSeMarca() {
        e.consumidor.procesar(e.ventaRegistrada(e.negocioA, turno, venta, Map.of("EFECTIVO", "150.00")));
        cerrarTurnoDirecto();
        var anulacion = e.ventaAnulada(e.negocioA, turno, venta, Map.of("EFECTIVO", "150.00"));

        assertThat(e.consumidor.procesar(anulacion)).isEqualTo(ResultadoAplicacion.SIN_TURNO_ABIERTO);
        assertThat(procesado(anulacion)).isFalse();
    }

    @Test
    @DisplayName("una venta aplicada si queda marcada como procesada")
    void aplicada_seMarca() {
        var evento = e.ventaRegistrada(e.negocioA, turno, venta, Map.of("EFECTIVO", "150.00"));

        var resultado = e.consumidor.procesar(evento);

        assertThat(resultado.completado()).isTrue();
        assertThat(procesado(evento)).isTrue();
    }

    // M3 y bajo 8: Caja no confia en los datos del evento

    @ParameterizedTest
    @ValueSource(strings = {"-150.00", "0.00", "1.001", "1E+999999999"})
    @DisplayName("M3: un pago negativo, cero, con 3 decimales o enorme es DATOS_INVALIDOS, sin efecto y sin marca")
    void montoInvalido_datosInvalidos(String monto) {
        var evento = e.ventaRegistrada(e.negocioA, turno, venta, Map.of("EFECTIVO", monto));

        var resultado = assertTimeoutPreemptively(RAPIDO, () -> e.consumidor.procesar(evento));

        assertThat(resultado).isEqualTo(ResultadoAplicacion.DATOS_INVALIDOS);
        assertThat(resultado.completado()).isFalse();
        assertThat(procesado(evento)).isFalse();
        assertThat(esperado(MetodoPago.EFECTIVO)).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("M3: una venta sin pagos es DATOS_INVALIDOS")
    void sinPagos_datosInvalidos() {
        var evento = e.ventaRegistrada(e.negocioA, turno, venta, Map.of());

        assertThat(e.consumidor.procesar(evento)).isEqualTo(ResultadoAplicacion.DATOS_INVALIDOS);
    }

    @Test
    @DisplayName("M3: un venta.registrada que no publico Ventas es DATOS_INVALIDOS")
    void origenAjeno_datosInvalidos() {
        var ahora = e.reloj.instant();
        var falso = new SobreEvento(UuidV7.generar(ahora), "venta.registrada", 1, e.negocioA, "clientes", ahora,
                venta, null, Map.of("venta_id", venta.toString(), "turno_id", turno.toString(),
                "pagos", List.of(Map.of("metodo", "EFECTIVO", "monto", "150.00"))));

        assertThat(e.consumidor.procesar(falso)).isEqualTo(ResultadoAplicacion.DATOS_INVALIDOS);
        assertThat(esperado(MetodoPago.EFECTIVO)).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("M3: la anulacion revierte lo que Caja registro, no los montos que diga el evento")
    void anulacion_revierteLoRegistrado() {
        e.consumidor.procesar(e.ventaRegistrada(e.negocioA, turno, venta, Map.of("EFECTIVO", "150.00")));

        var resultado = e.consumidor.procesar(e.ventaAnulada(e.negocioA, turno, venta,
                Map.of("EFECTIVO", "999.00", "TARJETA", "50.00")));

        assertThat(resultado).isEqualTo(ResultadoAplicacion.REVERTIDA);
        assertThat(esperado(MetodoPago.EFECTIVO)).isEqualByComparingTo("500.00");
        assertThat(e.libro.todos()).filteredOn(m -> m.tipo() == TipoMovimiento.ANULACION).singleElement()
                .satisfies(m -> {
                    assertThat(m.metodo()).isEqualTo(MetodoPago.EFECTIVO);
                    assertThat(m.monto()).isEqualByComparingTo("-150.00");
                });
    }

    // Bajo 4: turnoActivo con el estado efectivo

    @Test
    @DisplayName("CF-01: mientras el corte esta vigente, turnoActivo dice EN_CORTE")
    void turnoActivo_enCorte() {
        e.api.preparar(e.ctxA, new CerrarTurnoComando(turno, Map.of(MetodoPago.EFECTIVO, new BigDecimal("500.00")),
                false));

        assertThat(e.api.turnoActivo(e.ctxA, cajaDelTurno()).orElseThrow().estado()).isEqualTo(EstadoTurno.EN_CORTE);
    }

    @Test
    @DisplayName("CF-01 y D-81: con el corte vencido, turnoActivo dice ABIERTO para que el canal deje vender")
    void turnoActivo_corteVencido_abierto() {
        e.api.preparar(e.ctxA, new CerrarTurnoComando(turno, Map.of(MetodoPago.EFECTIVO, new BigDecimal("500.00")),
                false));
        e.reloj.avanzar(Duration.ofMinutes(5));

        var activo = e.api.turnoActivo(e.ctxA, cajaDelTurno()).orElseThrow();

        assertThat(activo.estado()).isEqualTo(EstadoTurno.ABIERTO);
        assertThat(activo.corteVenceEn()).isNull();
    }

    // VT-01: verificador de turno que Caja ofrece a Ventas

    @Test
    @DisplayName("VT-01: el verificador solo acepta un turno del negocio con estado efectivo ABIERTO")
    void verificador_soloAbierto() {
        var verificador = new VerificadorDeTurnoDesdeCaja(e.cajas, e.reloj);

        assertThat(verificador.abierto(e.ctxA, turno)).isTrue();
        assertThat(verificador.abierto(e.ctxB, turno)).isFalse();
        assertThat(verificador.abierto(e.ctxA, UUID.randomUUID())).isFalse();
        assertThat(verificador.abierto(e.ctxA, null)).isFalse();

        e.api.preparar(e.ctxA, new CerrarTurnoComando(turno, Map.of(MetodoPago.EFECTIVO, new BigDecimal("500.00")),
                false));
        assertThat(verificador.abierto(e.ctxA, turno)).isFalse();

        e.reloj.avanzar(Duration.ofMinutes(5));
        assertThat(verificador.abierto(e.ctxA, turno)).isTrue();

        cerrarTurnoDirecto();
        assertThat(verificador.abierto(e.ctxA, turno)).isFalse();
    }

    // M1: montos acotados

    @Test
    @DisplayName("M1: un monto enorme con exponente se rechaza al instante en movimiento, fondo y conteo")
    void montosEnormes_rechazoInmediato() {
        var enorme = new BigDecimal("1E+999999999");
        var otraCaja = e.crearCaja(e.ctxA, "Barra");
        assertTimeoutPreemptively(RAPIDO, () -> {
            fallaCon(() -> e.api.preparar(e.ctxA, new RegistrarMovimientoCajaComando(turno,
                    TipoMovimientoManual.ENTRADA, enorme, "x")), CodigoError.MONTO_INVALIDO);
            fallaCon(() -> e.api.preparar(e.ctxA, new AbrirTurnoComando(otraCaja, enorme)),
                    CodigoError.FONDO_INVALIDO);
            fallaCon(() -> e.api.preparar(e.ctxA, new CerrarTurnoComando(turno, Map.of(MetodoPago.EFECTIVO, enorme),
                    false)), CodigoError.CONTEO_INVALIDO);
        });
    }

    // Copias defensivas y toString

    @Test
    @DisplayName("el comando del corte copia su mapa: cambiar el original despues no lo altera")
    void cerrarTurno_copiaSuMapa() {
        var contado = new HashMap<MetodoPago, BigDecimal>();
        contado.put(MetodoPago.EFECTIVO, new BigDecimal("500.00"));
        var comando = new CerrarTurnoComando(turno, contado, false);

        contado.clear();

        assertThat(comando.contadoPorMetodo()).containsOnlyKeys(MetodoPago.EFECTIVO);
        assertThat(new CerrarTurnoComando(turno, null, false).contadoPorMetodo()).isNull();
    }

    @Test
    @DisplayName("toString de un movimiento manual no muestra la nota")
    void movimiento_toStringSinNota() {
        var nota = "Pago a Don Pepe por garrafones";

        assertThat(new RegistrarMovimientoCajaComando(turno, TipoMovimientoManual.RETIRO, BigDecimal.TEN, nota)
                .toString()).doesNotContain(nota);
    }

    @Test
    @DisplayName("ningun texto invita a reintentar con el mismo token, porque ya se gasto")
    void textos_noInvitanAReusarElToken() {
        for (var codigo : CodigoError.values()) {
            assertThat(PlantillasCaja.de(codigo)).doesNotContainIgnoringCase("inténtalo");
        }
        assertThat(PlantillasCaja.de(CodigoError.CONCURRENCIA)).contains("vuelve a empezar");
        assertThat(PlantillasCaja.de(CodigoError.ERROR_TEMPORAL)).contains("vuelve a empezar");
    }
}
