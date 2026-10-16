package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.clientes.eventos.ClienteConsentimientoCambiado;
import mx.negocio360.clientes.eventos.EstadoConsentimiento;
import mx.negocio360.clientes.eventos.Finalidad;
import mx.negocio360.clientes.eventos.MedioConsentimiento;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

/**
 * Lo que Spring Modulith necesita para que el outbox sea de fiar: serializar los eventos con el mapeador del contexto y
 * volver a leerlos igual al reenviar los pendientes, y reenviar los pendientes al arrancar (al menos una vez, R3).
 */
@SpringBootTest
class ConfiguracionDeEventosTest {

    @Autowired
    private ObjectMapper mapeadorDelContexto;
    @Autowired
    private Environment entorno;

    @Test
    @DisplayName("un evento serializado con el mapeador del contexto se lee igual al reenviarlo")
    void evento_idaYVuelta_conservaTodoElSobre() throws Exception {
        var ahora = Instant.parse("2026-10-15T18:00:00Z");
        var cliente = UUID.randomUUID();
        var original = new SobreEvento(UuidV7.generar(ahora), ClienteConsentimientoCambiado.TIPO,
                ClienteConsentimientoCambiado.VERSION, UUID.randomUUID(), "clientes", ahora, cliente, UUID.randomUUID(),
                new ClienteConsentimientoCambiado(cliente, Finalidad.PROMOCIONES, EstadoConsentimiento.OTORGADO,
                        MedioConsentimiento.PANEL).datos());

        var json = mapeadorDelContexto.writeValueAsString(original);
        var leido = mapeadorDelContexto.readValue(json, SobreEvento.class);

        assertThat(leido).isEqualTo(original);
    }

    @Test
    @DisplayName("el mapeador del contexto lee un evento con campos nuevos que aun no conoce (R5)")
    void evento_conCamposNuevos_seLeeSinFallar() throws Exception {
        var ahora = Instant.parse("2026-10-15T18:00:00Z");
        var id = UuidV7.generar(ahora);
        var json = "{\"id\":\"" + id + "\",\"tipo\":\"cliente.registrado\",\"version\":1,\"negocio_id\":\""
                + UUID.randomUUID() + "\",\"origen\":\"clientes\",\"ocurrido_en\":\"2026-10-15T18:00:00Z\","
                + "\"correlacion_id\":\"" + UUID.randomUUID() + "\",\"campo_futuro\":\"x\",\"datos\":{\"a\":\"b\"}}";

        var leido = mapeadorDelContexto.readValue(json, SobreEvento.class);

        assertThat(leido.id()).isEqualTo(id);
        assertThat(leido.datos()).isEqualTo(Map.of("a", "b"));
    }

    @Test
    @DisplayName("al arrancar se reenvian los eventos pendientes del outbox")
    void arranque_reenviaLosEventosPendientes() {
        var reenviar = entorno.getProperty("spring.modulith.events.republish-outstanding-events-on-restart",
                Boolean.class);

        assertThat(reenviar).isTrue();
    }
}
