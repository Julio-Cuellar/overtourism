package mx.negocio360.contabilidad.aplicacion;

import static mx.negocio360.contabilidad.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.Disparador;
import mx.negocio360.contabilidad.dominio.Poliza;
import mx.negocio360.contabilidad.dominio.ReglaContable;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Eventos;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * F-01: el IVA se desglosa segun la regla vigente (con tasa o exenta). Las tasas llevan fuente y fecha de
 * verificacion; las de estas pruebas son datos de ejemplo.
 */
class ReglaFiscalTest {

    private static final String FUENTE = "Ejemplo de prueba: regla exenta para entradas de museo, no verificada";

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    private ConfigurarReglaComando exentaEnEfectivo() {
        return new ConfigurarReglaComando(Disparador.VENTA, "EFECTIVO", e.cuenta(e.ctxA, "101.01").id(),
                e.cuenta(e.ctxA, "401.01").id(), null, null, FUENTE, e.reloj.instant());
    }

    private BigDecimal movido(Poliza poliza, String codigo) {
        var cuenta = e.cuenta(e.ctxA, codigo).id();
        return poliza.renglones().stream().filter(r -> r.cuentaId().equals(cuenta))
                .map(r -> r.cargo().add(r.abono())).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    @DisplayName("F-01: un museo con regla exenta registra la venta completa en 401.01 y nada en 208.01")
    void reglaExenta_todoAVentas() {
        e.reloj.avanzar(Duration.ofMinutes(1));
        e.confirmar(e.ctxA, exentaEnEfectivo());

        var resultado = e.api.contabilizarVenta(Eventos.venta(e.negocioA, "150.00", "EFECTIVO", e.reloj.instant()));

        var poliza = e.api.poliza(e.ctxA, resultado.polizaId()).orElseThrow();
        assertThat(movido(poliza, "401.01")).isEqualByComparingTo("150.00");
        assertThat(movido(poliza, "208.01")).isEqualByComparingTo("0");
        assertThat(poliza.renglones()).hasSize(2);
    }

    @Test
    @DisplayName("F-01: la regla nueva cierra la anterior en su inicio; la anterior conserva su fuente")
    void reglaNueva_cierraLaAnterior() {
        e.reloj.avanzar(Duration.ofMinutes(1));
        var inicio = e.reloj.instant();

        e.confirmar(e.ctxA, exentaEnEfectivo());

        var efectivo = e.api.reglas(e.ctxA).stream()
                .filter(r -> r.disparador() == Disparador.VENTA && "EFECTIVO".equals(r.condicion())).toList();
        assertThat(efectivo).hasSize(2);
        var anterior = efectivo.stream().filter(r -> r.tasaIva() != null).findFirst().orElseThrow();
        var nueva = efectivo.stream().filter(r -> r.tasaIva() == null).findFirst().orElseThrow();
        assertThat(anterior.vigenteHasta()).isEqualTo(inicio);
        assertThat(anterior.fuente()).isEqualTo(Fixtures.FUENTE_EJEMPLO);
        assertThat(nueva.vigenteDesde()).isEqualTo(inicio);
        assertThat(nueva.fuente()).isEqualTo(FUENTE);
    }

    @Test
    @DisplayName("F-01: una regla que empieza antes o igual que la vigente falla con REGLA_SUPERPUESTA")
    void reglaQueTraslapa_falla() {
        var actual = e.api.reglas(e.ctxA).stream()
                .filter(r -> r.disparador() == Disparador.VENTA && "EFECTIVO".equals(r.condicion()))
                .findFirst().orElseThrow();

        fallaCon(() -> e.api.preparar(e.ctxA, new ConfigurarReglaComando(Disparador.VENTA, "EFECTIVO",
                actual.cuentaCargoId(), actual.cuentaAbonoId(), null, null, FUENTE, actual.vigenteDesde())),
                CodigoError.REGLA_SUPERPUESTA);
    }

    @Test
    @DisplayName("F-01: una tasa sin fuente, fuera de rango o sin cuenta de impuesto falla con TASA_INVALIDA")
    void tasaInvalida_falla() {
        var caja = e.cuenta(e.ctxA, "101.01").id();
        var ventas = e.cuenta(e.ctxA, "401.01").id();
        var iva = e.cuenta(e.ctxA, "208.01").id();
        var despues = e.reloj.instant().plusSeconds(60);

        fallaCon(() -> e.api.preparar(e.ctxA, new ConfigurarReglaComando(Disparador.VENTA, "EFECTIVO", caja, ventas,
                iva, Fixtures.m("0.16"), " ", despues)), CodigoError.TASA_INVALIDA);
        fallaCon(() -> e.api.preparar(e.ctxA, new ConfigurarReglaComando(Disparador.VENTA, "EFECTIVO", caja, ventas,
                iva, Fixtures.m("1.16"), FUENTE, despues)), CodigoError.TASA_INVALIDA);
        fallaCon(() -> e.api.preparar(e.ctxA, new ConfigurarReglaComando(Disparador.VENTA, "EFECTIVO", caja, ventas,
                null, Fixtures.m("0.16"), FUENTE, despues)), CodigoError.TASA_INVALIDA);
    }

    @Test
    @DisplayName("F-01: una regla con una cuenta no afectable o ajena falla con el codigo de A-04")
    void reglaConCuentaInvalida_falla() {
        var despues = e.reloj.instant().plusSeconds(60);

        fallaCon(() -> e.api.preparar(e.ctxA, new ConfigurarReglaComando(Disparador.VENTA, "VALES",
                e.cuenta(e.ctxA, "101").id(), e.cuenta(e.ctxA, "401.01").id(), null, null, FUENTE, despues)),
                CodigoError.CUENTA_NO_AFECTABLE);
        fallaCon(() -> e.api.preparar(e.ctxA, new ConfigurarReglaComando(Disparador.VENTA, "VALES",
                UUID.randomUUID(), e.cuenta(e.ctxA, "401.01").id(), null, null, FUENTE, despues)),
                CodigoError.CUENTA_NO_ENCONTRADA);
    }

    @Test
    @DisplayName("F-01: con una regla para VALES, una venta en vales deja de quedar pendiente")
    void reglaNuevaCondicion_seUsa() {
        e.confirmar(e.ctxA, new ConfigurarReglaComando(Disparador.VENTA, "VALES", e.cuenta(e.ctxA, "103.01").id(),
                e.cuenta(e.ctxA, "401.01").id(), e.cuenta(e.ctxA, "208.01").id(), Fixtures.TASA_EJEMPLO,
                Fixtures.FUENTE_EJEMPLO, e.reloj.instant()));

        var resultado = e.api.contabilizarVenta(Eventos.venta(e.negocioA, "116.00", "VALES", e.reloj.instant()));

        assertThat(resultado.polizaId()).isNotNull();
        assertThat(e.api.reglas(e.ctxA)).extracting(ReglaContable::condicion).contains("VALES");
    }
}
