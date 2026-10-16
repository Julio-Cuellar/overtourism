package mx.negocio360.clientes.dominio;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import mx.negocio360.clientes.eventos.EstadoConsentimiento;
import mx.negocio360.clientes.eventos.Finalidad;
import mx.negocio360.clientes.eventos.MedioConsentimiento;
import mx.negocio360.clientes.eventos.OrigenCliente;
import mx.negocio360.clientes.fakes.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Los records del dominio no pueden construirse en un estado que las reglas prohiben. */
class InvariantesTest {

    private static final Instant AHORA = Fixtures.AHORA;
    private static final DatosCliente DATOS = new DatosCliente("Ana", null, null, null, null, null);

    @Test
    @DisplayName("un cliente exige version 1 o mayor y version de aviso 1 o mayor")
    void cliente_versionesMenoresAUno_lanzanIllegalArgument() {
        var id = UUID.randomUUID();
        var negocio = UUID.randomUUID();

        assertThatThrownBy(() -> new Cliente(id, negocio, DATOS, OrigenCliente.PANEL, EstadoCliente.ACTIVO, 1, AHORA,
                null, 0L, AHORA, AHORA, "a")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Cliente(id, negocio, DATOS, OrigenCliente.PANEL, EstadoCliente.ACTIVO, 0, AHORA,
                null, 1L, AHORA, AHORA, "a")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("un consentimiento exige la version del aviso bajo la que se dio (P-04)")
    void consentimiento_sinVersionDeAviso_lanzaIllegalArgument() {
        var id = UUID.randomUUID();

        assertThatThrownBy(() -> new Consentimiento(id, id, id, Finalidad.PROMOCIONES, EstadoConsentimiento.OTORGADO,
                FormaConsentimiento.EXPRESO, MedioConsentimiento.VERBAL, 0, "a", AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("un aviso no puede cerrarse antes de empezar")
    void aviso_cierreAnteriorAlInicio_lanzaIllegalArgument() {
        var aviso = AvisoPrivacidad.nuevo(UUID.randomUUID(), UUID.randomUUID(), 1, "T", "C",
                List.of(new FinalidadAviso("VENTA", false)), "s", "i", "c", "a", AHORA);

        assertThatThrownBy(() -> aviso.cerradoEn(AHORA.minusSeconds(1))).isInstanceOf(IllegalArgumentException.class);
    }
}
