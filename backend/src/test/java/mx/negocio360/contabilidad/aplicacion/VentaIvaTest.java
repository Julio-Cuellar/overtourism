package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.OrigenPoliza;
import mx.negocio360.contabilidad.dominio.Poliza;
import mx.negocio360.contabilidad.dominio.ResultadoEvento;
import mx.negocio360.contabilidad.dominio.TipoPoliza;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Eventos;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

/** F-02: en ventas cobradas en el momento, el IVA va a IVA trasladado cobrado (208.01). */
class VentaIvaTest {

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    private BigDecimal abonoA(Poliza poliza, String codigo) {
        var cuenta = e.cuenta(e.ctxA, codigo).id();
        return poliza.renglones().stream().filter(r -> r.cuentaId().equals(cuenta)).map(r -> r.abono())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal cargoA(Poliza poliza, String codigo) {
        var cuenta = e.cuenta(e.ctxA, codigo).id();
        return poliza.renglones().stream().filter(r -> r.cuentaId().equals(cuenta)).map(r -> r.cargo())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @ParameterizedTest(name = "{0} carga a {1}")
    @CsvSource({"TARJETA, 103.01", "EFECTIVO, 101.01", "TRANSFERENCIA, 102.01"})
    @DisplayName("F-02: venta de 232.00 carga al medio de pago y abona 200.00 a ventas y 32.00 a 208.01")
    void venta_ivaATrasladadoCobrado(String medio, String cuentaDelMedio) {
        var resultado = e.api.contabilizarVenta(Eventos.venta(e.negocioA, "232.00", medio, Fixtures.AHORA));

        assertThat(resultado.resultado()).isEqualTo(ResultadoEvento.Resultado.APLICADO);
        var poliza = e.api.poliza(e.ctxA, resultado.polizaId()).orElseThrow();
        assertThat(poliza.tipo()).isEqualTo(TipoPoliza.INGRESO);
        assertThat(cargoA(poliza, cuentaDelMedio)).isEqualByComparingTo("232.00");
        assertThat(abonoA(poliza, "401.01")).isEqualByComparingTo("200.00");
        assertThat(abonoA(poliza, "208.01")).isEqualByComparingTo("32.00");
    }

    @Test
    @DisplayName("F-02: la poliza de la venta es automatica, apunta al evento y a la venta, y la crea el sistema")
    void venta_polizaAutomaticaConReferencias() {
        var ventaId = UUID.randomUUID();
        var evento = Eventos.venta(e.negocioA, ventaId, "116.00", "EFECTIVO", Fixtures.AHORA, null);

        var poliza = e.api.poliza(e.ctxA, e.api.contabilizarVenta(evento).polizaId()).orElseThrow();

        assertThat(poliza.origen()).isEqualTo(OrigenPoliza.AUTOMATICA);
        assertThat(poliza.eventoId()).isEqualTo(evento.id());
        assertThat(poliza.referenciaTipo()).isEqualTo("VENTA");
        assertThat(poliza.referenciaId()).isEqualTo(ventaId);
        assertThat(poliza.creadoPor()).isEqualTo("sistema");
        assertThat(poliza.fecha()).isEqualTo(Fixtures.HOY);
    }

    @Test
    @DisplayName("F-02: la fecha contable es la de Puebla: una venta a las 23:30 del 15 cae el 15, no el 16")
    void venta_fechaEnHoraDePuebla() {
        var nocheEnPuebla = java.time.Instant.parse("2026-10-16T05:30:00Z");

        var poliza = e.api.poliza(e.ctxA, e.api.contabilizarVenta(
                Eventos.venta(e.negocioA, "10.00", "EFECTIVO", nocheEnPuebla)).polizaId()).orElseThrow();

        assertThat(poliza.fecha()).isEqualTo(LocalDate.of(2026, 10, 15));
    }
}
