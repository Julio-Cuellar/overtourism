package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.Asiento;
import mx.negocio360.contabilidad.dominio.Cuenta;
import mx.negocio360.contabilidad.dominio.Naturaleza;
import mx.negocio360.contabilidad.dominio.Renglon;
import mx.negocio360.contabilidad.dominio.TipoPoliza;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A-08: los saldos se actualizan con cada poliza y son derivados de los movimientos. */
class SaldosTest {

    private static final YearMonth OCTUBRE = YearMonth.of(2026, 10);
    private static final YearMonth NOVIEMBRE = YearMonth.of(2026, 11);

    private final Escenario e = new Escenario();
    private List<Cuenta> afectables;

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
        afectables = e.api.catalogo(e.ctxA).stream().filter(Cuenta::afectable).toList();
    }

    private void registrar(LocalDate fecha, UUID cargo, UUID abono, String monto) {
        var asiento = new Asiento(TipoPoliza.DIARIO, fecha, "Saldo de prueba", List.of(
                new Renglon(cargo, Fixtures.m(monto), null, null), new Renglon(abono, null, Fixtures.m(monto), null)));
        e.libro.registrar(e.negocioA, asiento, LibroDiario.Registro.manual("prueba"));
    }

    /** Saldo final esperado por cuenta al cierre de un mes, calculado solo con los movimientos. */
    private Map<UUID, BigDecimal> recalcular(YearMonth hasta) {
        var naturaleza = new HashMap<UUID, Naturaleza>();
        afectables.forEach(c -> naturaleza.put(c.id(), c.naturaleza()));
        var saldos = new HashMap<UUID, BigDecimal>();
        for (var poliza : e.polizas.delNegocio(e.negocioA)) {
            if (YearMonth.from(poliza.fecha()).isAfter(hasta)) {
                continue;
            }
            for (var renglon : poliza.renglones()) {
                var neto = renglon.cargo().subtract(renglon.abono());
                if (naturaleza.get(renglon.cuentaId()) == Naturaleza.ACREEDORA) {
                    neto = neto.negate();
                }
                saldos.merge(renglon.cuentaId(), neto, BigDecimal::add);
            }
        }
        return saldos;
    }

    @Test
    @DisplayName("A-08: una venta deja Caja en 232.00 deudora, Ventas en 200.00 e IVA en 32.00 acreedoras")
    void saldo_unaPoliza() {
        var asiento = new Asiento(TipoPoliza.INGRESO, Fixtures.HOY, "Venta", List.of(
                new Renglon(e.cuenta(e.ctxA, "101.01").id(), Fixtures.m("232.00"), null, null),
                new Renglon(e.cuenta(e.ctxA, "401.01").id(), null, Fixtures.m("200.00"), null),
                new Renglon(e.cuenta(e.ctxA, "208.01").id(), null, Fixtures.m("32.00"), null)));
        e.libro.registrar(e.negocioA, asiento, LibroDiario.Registro.manual("prueba"));

        var balanza = e.api.balanza(e.ctxA, OCTUBRE);

        assertThat(balanza.fila("101.01").saldoFinal()).isEqualByComparingTo("232.00");
        assertThat(balanza.fila("101.01").cargos()).isEqualByComparingTo("232.00");
        assertThat(balanza.fila("401.01").saldoFinal()).isEqualByComparingTo("200.00");
        assertThat(balanza.fila("208.01").saldoFinal()).isEqualByComparingTo("32.00");
        assertThat(balanza.totalCargos()).isEqualByComparingTo("232.00");
        assertThat(balanza.totalAbonos()).isEqualByComparingTo("232.00");
        assertThat(balanza.cuadra()).isTrue();
    }

    @Test
    @DisplayName("A-08: tras 1,000 polizas aleatorias en dos meses, la balanza coincide con la recalculada")
    void saldo_milPolizasAleatorias_coincideConLaRecalculada() {
        var azar = new Random(20261015L);
        for (var i = 0; i < 1000; i++) {
            var cargo = afectables.get(azar.nextInt(afectables.size())).id();
            var abono = afectables.get(azar.nextInt(afectables.size())).id();
            var monto = BigDecimal.valueOf(1 + azar.nextInt(500_000), 2).toPlainString();
            var fecha = azar.nextBoolean() ? LocalDate.of(2026, 10, 1 + azar.nextInt(31))
                    : LocalDate.of(2026, 11, 1 + azar.nextInt(30));
            registrar(fecha, cargo, abono, monto);
        }

        for (var mes : List.of(OCTUBRE, NOVIEMBRE)) {
            var balanza = e.api.balanza(e.ctxA, mes);
            var esperado = recalcular(mes);
            for (var cuenta : afectables) {
                assertThat(balanza.fila(cuenta.codigo()).saldoFinal())
                        .as("%s en %s", cuenta.codigo(), mes)
                        .isEqualByComparingTo(esperado.getOrDefault(cuenta.id(), BigDecimal.ZERO));
            }
            assertThat(balanza.cuadra()).isTrue();
        }
    }

    @Test
    @DisplayName("A-08: el saldo inicial de noviembre es el final de octubre, aunque octubre reciba polizas despues")
    void saldo_inicialSigueAlFinalDelMesAnterior() {
        var caja = e.cuenta(e.ctxA, "101.01").id();
        var ventas = e.cuenta(e.ctxA, "401.01").id();
        registrar(LocalDate.of(2026, 10, 3), caja, ventas, "100.00");
        registrar(LocalDate.of(2026, 11, 3), caja, ventas, "40.00");

        registrar(LocalDate.of(2026, 10, 30), caja, ventas, "10.00");

        var noviembre = e.api.balanza(e.ctxA, NOVIEMBRE).fila("101.01");
        assertThat(noviembre.saldoInicial()).isEqualByComparingTo("110.00");
        assertThat(noviembre.saldoFinal()).isEqualByComparingTo("150.00");
    }

    @Test
    @DisplayName("A-08: una cuenta sin movimientos en el mes arrastra su saldo del mes anterior")
    void saldo_cuentaSinMovimientos_arrastraSaldo() {
        registrar(LocalDate.of(2026, 10, 3), e.cuenta(e.ctxA, "102.01").id(), e.cuenta(e.ctxA, "401.01").id(),
                "75.00");
        registrar(LocalDate.of(2026, 11, 3), e.cuenta(e.ctxA, "101.01").id(), e.cuenta(e.ctxA, "401.01").id(),
                "5.00");

        var bancos = e.api.balanza(e.ctxA, NOVIEMBRE).fila("102.01");
        assertThat(bancos.saldoInicial()).isEqualByComparingTo("75.00");
        assertThat(bancos.cargos()).isEqualByComparingTo("0");
        assertThat(bancos.saldoFinal()).isEqualByComparingTo("75.00");
    }

    @Test
    @DisplayName("A-08: la balanza de otro negocio no ve estos saldos")
    void saldo_otroNegocio_noLosVe() {
        e.activar(e.ctxB);
        registrar(Fixtures.HOY, e.cuenta(e.ctxA, "101.01").id(), e.cuenta(e.ctxA, "401.01").id(), "9.00");

        assertThat(e.api.balanza(e.ctxB, OCTUBRE).totalCargos()).isEqualByComparingTo("0");
    }
}
