package mx.negocio360.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import mx.negocio360.seguridad.ClienteWebSocket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SG-10 y SG-15: adivinar tokens por WebSocket gasta las mismas fichas de la IP que adivinarlos por HTTP. Agotadas, ni el
 * apreton de manos pasa, aunque el siguiente intento traiga una sesion valida.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "seguridad.tasa.fallos-por-minuto-ip=3")
@AutoConfigureMockMvc
class FallosDeInicioWebSocketTest {

    @LocalServerPort
    private int puerto;

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("3 connection_init fallidos agotan las fichas de la IP: el siguiente apreton de manos recibe 429")
    void iniciosFallidos_agotanLaIp() throws Exception {
        var valido = new FlujoHttp(mvc).sesionConNegocio().token();
        for (var i = 0; i < 3; i++) {
            try (var cliente = ClienteWebSocket.conectar(puerto, null)) {
                cliente.iniciar("A".repeat(43));
                assertThat(cliente.cierre(Duration.ofSeconds(5)).getCode()).isEqualTo(4401);
            }
        }

        assertThat(ClienteWebSocket.motivoDelRechazo(puerto, null)).contains("429");
        assertThat(valido).isNotBlank();
    }
}
