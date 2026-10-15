package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import mx.negocio360.contabilidad.dominio.Asiento;
import mx.negocio360.contabilidad.dominio.Poliza;
import mx.negocio360.contabilidad.dominio.Renglon;
import mx.negocio360.contabilidad.dominio.TipoPoliza;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** A-05: folio consecutivo por negocio, periodo y tipo, asignado al confirmar. */
class FoliosTest {

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
        e.activar(e.ctxB);
    }

    private Poliza registrar(java.util.UUID negocio, TipoPoliza tipo, LocalDate fecha) {
        var contexto = negocio.equals(e.negocioA) ? e.ctxA : e.ctxB;
        var asiento = new Asiento(tipo, fecha, "Prueba de folio", List.of(
                new Renglon(e.cuenta(contexto, "101.01").id(), Fixtures.m("10.00"), null, null),
                new Renglon(e.cuenta(contexto, "401.01").id(), null, Fixtures.m("10.00"), null)));
        return e.libro.registrar(negocio, asiento, LibroDiario.Registro.manual("prueba"));
    }

    @Test
    @DisplayName("A-05: dos polizas de ingreso del mismo mes reciben 1 y 2")
    void folio_consecutivoPorTipo() {
        assertThat(registrar(e.negocioA, TipoPoliza.INGRESO, Fixtures.HOY).folio()).isEqualTo(1);
        assertThat(registrar(e.negocioA, TipoPoliza.INGRESO, Fixtures.HOY).folio()).isEqualTo(2);
    }

    @Test
    @DisplayName("A-05: cada tipo, cada periodo y cada negocio llevan su propia serie")
    void folio_seriesIndependientes() {
        registrar(e.negocioA, TipoPoliza.INGRESO, Fixtures.HOY);
        registrar(e.negocioA, TipoPoliza.INGRESO, Fixtures.HOY);

        assertThat(registrar(e.negocioA, TipoPoliza.DIARIO, Fixtures.HOY).folio()).isEqualTo(1);
        assertThat(registrar(e.negocioA, TipoPoliza.INGRESO, LocalDate.of(2026, 11, 2)).folio()).isEqualTo(1);
        assertThat(registrar(e.negocioB, TipoPoliza.INGRESO, Fixtures.HOY).folio()).isEqualTo(1);
    }

    @Test
    @DisplayName("A-05: el folio se asigna al confirmar, no al preparar")
    void folio_seAsignaAlConfirmar() {
        var comando = new RegistrarPolizaManualComando(TipoPoliza.DIARIO, Fixtures.HOY, "Ajuste", List.of(
                new RenglonManual(e.cuenta(e.ctxA, "102.01").id(), Fixtures.m("5.00"), null, null),
                new RenglonManual(e.cuenta(e.ctxA, "101.01").id(), null, Fixtures.m("5.00"), null)));
        var primero = e.api.preparar(e.ctxA, comando).token();
        var segundo = e.api.preparar(e.ctxA, comando).token();

        var idSegundo = e.api.confirmar(e.ctxA, segundo).recursoId();
        var idPrimero = e.api.confirmar(e.ctxA, primero).recursoId();

        assertThat(e.api.poliza(e.ctxA, idSegundo).orElseThrow().folio()).isEqualTo(1);
        assertThat(e.api.poliza(e.ctxA, idPrimero).orElseThrow().folio()).isEqualTo(2);
    }

    @Test
    @DisplayName("A-05: 50 polizas registradas a la vez reciben los folios 1 a 50 sin repetir")
    void folio_concurrente_sinRepetir() throws Exception {
        var hilos = Executors.newFixedThreadPool(8);
        try {
            var tareas = new ArrayList<Callable<Integer>>();
            for (var i = 0; i < 50; i++) {
                tareas.add(() -> registrar(e.negocioA, TipoPoliza.INGRESO, Fixtures.HOY).folio());
            }
            var folios = new ArrayList<Integer>();
            for (var resultado : hilos.invokeAll(tareas)) {
                folios.add(resultado.get());
            }

            assertThat(folios).containsExactlyInAnyOrderElementsOf(
                    java.util.stream.IntStream.rangeClosed(1, 50).boxed().toList());
        } finally {
            hilos.shutdown();
            hilos.awaitTermination(5, TimeUnit.SECONDS);
        }
    }
}
