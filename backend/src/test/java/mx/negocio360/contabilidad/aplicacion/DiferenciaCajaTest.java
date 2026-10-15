package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.OrigenPoliza;
import mx.negocio360.contabilidad.dominio.Poliza;
import mx.negocio360.contabilidad.dominio.TipoPoliza;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Eventos;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * J-02: faltante, cargo 601.01 y abono 101.01; sobrante, cargo 101.01 y abono 704.01; diferencia cero, sin poliza.
 */
class DiferenciaCajaTest {

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    private BigDecimal cargoA(Poliza poliza, String codigo) {
        var cuenta = e.cuenta(e.ctxA, codigo).id();
        return poliza.renglones().stream().filter(r -> r.cuentaId().equals(cuenta)).map(r -> r.cargo())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal abonoA(Poliza poliza, String codigo) {
        var cuenta = e.cuenta(e.ctxA, codigo).id();
        return poliza.renglones().stream().filter(r -> r.cuentaId().equals(cuenta)).map(r -> r.abono())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Test
    @DisplayName("J-02: un faltante de 15.50 carga 601.01 y abona 101.01 por 15.50 en una poliza de diario")
    void faltante_polizaDeDiario() {
        var corteId = UUID.randomUUID();

        var resultado = e.api.contabilizarCorte(Eventos.corte(e.negocioA, corteId, UUID.randomUUID(), "1850.00",
                "1834.50", null, Fixtures.AHORA));

        var poliza = e.api.poliza(e.ctxA, resultado.polizaId()).orElseThrow();
        assertThat(poliza.tipo()).isEqualTo(TipoPoliza.DIARIO);
        assertThat(poliza.origen()).isEqualTo(OrigenPoliza.AUTOMATICA);
        assertThat(poliza.referenciaTipo()).isEqualTo("CORTE");
        assertThat(poliza.referenciaId()).isEqualTo(corteId);
        assertThat(poliza.fecha()).isEqualTo(Fixtures.HOY);
        assertThat(cargoA(poliza, "601.01")).isEqualByComparingTo("15.50");
        assertThat(abonoA(poliza, "101.01")).isEqualByComparingTo("15.50");
        assertThat(e.api.cortes(e.ctxA).get(0).polizaId()).isEqualTo(poliza.id());
    }

    @Test
    @DisplayName("J-02: un sobrante de 20.00 carga 101.01 y abona 704.01")
    void sobrante_polizaContraOtrosIngresos() {
        var resultado = e.api.contabilizarCorte(Eventos.corte(e.negocioA, UUID.randomUUID(), UUID.randomUUID(),
                "500.00", "520.00", null, Fixtures.AHORA));

        var poliza = e.api.poliza(e.ctxA, resultado.polizaId()).orElseThrow();
        assertThat(cargoA(poliza, "101.01")).isEqualByComparingTo("20.00");
        assertThat(abonoA(poliza, "704.01")).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("J-02: con diferencia cero no hay poliza")
    void sinDiferencia_sinPoliza() {
        var resultado = e.api.contabilizarCorte(Eventos.corte(e.negocioA, UUID.randomUUID(), UUID.randomUUID(),
                "500.00", "500.00", null, Fixtures.AHORA));

        assertThat(resultado.polizaId()).isNull();
        assertThat(e.polizas.delNegocio(e.negocioA)).isEmpty();
        assertThat(e.api.cortes(e.ctxA).get(0).polizaId()).isNull();
    }
}
