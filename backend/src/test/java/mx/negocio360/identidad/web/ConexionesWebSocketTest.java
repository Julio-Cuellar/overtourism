package mx.negocio360.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;

import mx.negocio360.seguridad.ClienteWebSocket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** SG-15: el tope de conexiones nuevas por IP, en su propio contexto porque todas las pruebas conectan desde 127.0.0.1. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "seguridad.websocket.conexiones-por-minuto-ip=3")
class ConexionesWebSocketTest {

    @LocalServerPort
    private int puerto;

    @Test
    @DisplayName("3 conexiones nuevas por minuto desde una IP pasan; la cuarta recibe 429 en el apreton de manos")
    void conexionesPorIp() throws Exception {
        for (var i = 0; i < 3; i++) {
            try (var cliente = ClienteWebSocket.conectar(puerto, null)) {
                assertThat(cliente).isNotNull();
            }
        }

        assertThat(ClienteWebSocket.motivoDelRechazo(puerto, null)).contains("429");
    }
}
