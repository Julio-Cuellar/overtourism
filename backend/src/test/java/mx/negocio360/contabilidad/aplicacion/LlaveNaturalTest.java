package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import mx.negocio360.contabilidad.dominio.ResultadoEvento.Resultado;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Eventos;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** E-02: llave natural por negocio ademas del evento_id (venta_id, cupon_id o corte_id). */
class LlaveNaturalTest {

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
        e.activar(e.ctxB);
    }

    @Test
    @DisplayName("E-02: dos eventos distintos con el mismo venta_id: el segundo queda OMITIDO con DUPLICADO_NATURAL")
    void mismaVenta_dosEventos_unaPoliza() {
        var ventaId = UUID.randomUUID();
        e.api.contabilizarVenta(Eventos.venta(e.negocioA, ventaId, "50.00", "EFECTIVO", Fixtures.AHORA, null));

        var segundo = e.api.contabilizarVenta(
                Eventos.venta(e.negocioA, ventaId, "50.00", "EFECTIVO", Fixtures.AHORA, null));

        assertThat(segundo.resultado()).isEqualTo(Resultado.OMITIDO);
        assertThat(segundo.motivo()).isEqualTo("DUPLICADO_NATURAL");
        assertThat(e.polizas.delNegocio(e.negocioA)).hasSize(1);
        assertThat(e.eventosProcesados.todas()).extracting(p -> p.resultado())
                .containsExactly(Resultado.APLICADO, Resultado.OMITIDO);
    }

    @Test
    @DisplayName("E-02: la llave es por negocio: el mismo venta_id en otro negocio si se contabiliza")
    void mismaVenta_otroNegocio_seContabiliza() {
        var ventaId = UUID.randomUUID();
        e.api.contabilizarVenta(Eventos.venta(e.negocioA, ventaId, "50.00", "EFECTIVO", Fixtures.AHORA, null));

        var otro = e.api.contabilizarVenta(
                Eventos.venta(e.negocioB, ventaId, "50.00", "EFECTIVO", Fixtures.AHORA, null));

        assertThat(otro.resultado()).isEqualTo(Resultado.APLICADO);
    }
}
