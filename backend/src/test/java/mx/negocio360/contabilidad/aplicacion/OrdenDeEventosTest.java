package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.contabilidad.dominio.Conciliacion;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Eventos;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** E-05: sin orden global, venta, cupon y corte dan el mismo resultado en cualquier orden de llegada. */
class OrdenDeEventosTest {

    private record Llegada(SobreEvento evento, Consumer<SobreEvento> contabilizar) {
    }

    /** Resultado observable de procesar los tres eventos en un orden. */
    private record Resultado(String balanza, Conciliacion conciliacion, int polizas) {
    }

    private static Resultado procesar(List<Integer> orden) {
        var e = new Escenario();
        e.activar(e.ctxA);
        var sesion = UUID.randomUUID();
        var ventaId = UUID.randomUUID();
        var llegadas = List.of(
                new Llegada(Eventos.venta(e.negocioA, ventaId, "232.00", "EFECTIVO", Fixtures.AHORA, sesion),
                        x -> e.api.contabilizarVenta(x)),
                new Llegada(Eventos.canje(e.negocioA, UUID.randomUUID(), ventaId, "29.00", "NEGOCIO", Fixtures.AHORA),
                        x -> e.api.contabilizarCanje(x)),
                new Llegada(Eventos.corte(e.negocioA, UUID.randomUUID(), sesion, "1232.00", "1216.50", "232.00",
                        Fixtures.AHORA), x -> e.api.contabilizarCorte(x)));
        for (var i : orden) {
            llegadas.get(i).contabilizar().accept(llegadas.get(i).evento());
        }
        var balanza = e.api.balanza(e.ctxA, YearMonth.from(Fixtures.HOY)).filas().stream()
                .map(f -> f.codigo() + "=" + f.saldoFinal().toPlainString()).toList().toString();
        return new Resultado(balanza, e.api.cortes(e.ctxA).get(0).conciliacion(),
                e.polizas.delNegocio(e.negocioA).size());
    }

    private static List<List<Integer>> permutaciones() {
        var todas = new ArrayList<List<Integer>>();
        for (var a = 0; a < 3; a++) {
            for (var b = 0; b < 3; b++) {
                for (var c = 0; c < 3; c++) {
                    if (a != b && b != c && a != c) {
                        todas.add(List.of(a, b, c));
                    }
                }
            }
        }
        return todas;
    }

    @Test
    @DisplayName("E-05: las 6 permutaciones de venta, cupon y corte dan la misma balanza y la misma conciliacion")
    void seisPermutaciones_mismoResultado() {
        var esperado = procesar(List.of(0, 1, 2));

        assertThat(esperado.conciliacion()).isEqualTo(Conciliacion.DIFERENCIA_CAJA);
        assertThat(esperado.polizas()).as("venta, cupon y faltante").isEqualTo(3);
        for (var orden : permutaciones()) {
            assertThat(procesar(orden)).as("orden %s", orden).isEqualTo(esperado);
        }
    }

    @Test
    @DisplayName("E-05: la balanza de cualquier orden cuadra")
    void cualquierOrden_cuadra() {
        for (var orden : permutaciones()) {
            var e = new Escenario();
            e.activar(e.ctxA);
            var ventaId = UUID.randomUUID();
            var eventos = List.of(Eventos.venta(e.negocioA, ventaId, "232.00", "EFECTIVO", Fixtures.AHORA, null),
                    Eventos.canje(e.negocioA, UUID.randomUUID(), ventaId, "29.00", "NEGOCIO", Fixtures.AHORA));
            for (var i : orden) {
                if (i == 0) {
                    e.api.contabilizarVenta(eventos.get(0));
                } else if (i == 1) {
                    e.api.contabilizarCanje(eventos.get(1));
                }
            }

            assertThat(e.api.balanza(e.ctxA, YearMonth.from(Fixtures.HOY)).cuadra()).as("orden %s", orden).isTrue();
        }
    }
}
