package mx.negocio360.contabilidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.EstadoPendiente;
import mx.negocio360.contabilidad.dominio.ResultadoEvento.Resultado;
import mx.negocio360.contabilidad.fakes.Escenario;
import mx.negocio360.contabilidad.fakes.Eventos;
import mx.negocio360.contabilidad.fakes.Fixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** E-06: una version de evento que no se conoce crea un pendiente y no tumba al consumidor. */
class VersionEventoTest {

    private final Escenario e = new Escenario();

    @BeforeEach
    void activar() {
        e.activar(e.ctxA);
    }

    @Test
    @DisplayName("E-06: venta.registrada v2 crea un pendiente VERSION_NO_SOPORTADA y no registra poliza")
    void versionDesconocida_quedaPendiente() {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("venta_id", UUID.randomUUID().toString());
        datos.put("total", "10.00");
        datos.put("medio_pago", "EFECTIVO");

        var resultado = e.api.contabilizarVenta(Eventos.sobre("venta.registrada", 2, e.negocioA, "ventas",
                Fixtures.AHORA, UUID.randomUUID(), datos));

        assertThat(resultado.resultado()).isEqualTo(Resultado.PENDIENTE);
        assertThat(resultado.motivo()).isEqualTo("VERSION_NO_SOPORTADA");
        assertThat(e.polizas.delNegocio(e.negocioA)).isEmpty();
        assertThat(e.api.pendientes(e.ctxA, EstadoPendiente.ABIERTO)).singleElement()
                .satisfies(p -> assertThat(p.motivo()).isEqualTo("VERSION_NO_SOPORTADA"));
    }

    @Test
    @DisplayName("E-06: despues de una version desconocida, la siguiente v1 se contabiliza")
    void versionDesconocida_laColaSigue() {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("venta_id", UUID.randomUUID().toString());
        datos.put("algo_nuevo", "x");
        e.api.contabilizarVenta(Eventos.sobre("venta.registrada", 3, e.negocioA, "ventas", Fixtures.AHORA,
                UUID.randomUUID(), datos));

        var v1 = e.api.contabilizarVenta(Eventos.venta(e.negocioA, "10.00", "EFECTIVO", Fixtures.AHORA));

        assertThat(v1.resultado()).isEqualTo(Resultado.APLICADO);
    }
}
