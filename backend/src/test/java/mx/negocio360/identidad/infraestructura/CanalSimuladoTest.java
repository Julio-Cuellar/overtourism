package mx.negocio360.identidad.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/**
 * D-77: el canal simulado escribe el codigo en la consola, para poder registrarse y entrar sin un servicio de
 * mensajeria. Es la unica excepcion a SI-15: un solo registro con nombre propio, con el correo enmascarado.
 */
class CanalSimuladoTest {

    private static final String CORREO = "ana.lopez@example.com";

    private final CanalSimulado canal = new CanalSimulado();
    private Logger registro;
    private ListAppender<ILoggingEvent> eventos;

    @BeforeEach
    void conectar() {
        registro = (Logger) LoggerFactory.getLogger(CanalSimulado.NOMBRE_DEL_REGISTRO);
        eventos = new ListAppender<>();
        eventos.start();
        registro.addAppender(eventos);
    }

    @AfterEach
    void desconectar() {
        registro.detachAppender(eventos);
    }

    @Test
    @DisplayName("enviar escribe una linea en la consola con el codigo y el correo enmascarado")
    void enviar_escribeElCodigoEnLaConsola() {
        canal.enviar(CORREO, "123456");

        assertThat(eventos.list).singleElement().satisfies(evento -> {
            assertThat(evento.getLevel()).isEqualTo(Level.INFO);
            assertThat(evento.getFormattedMessage()).contains("123456").contains("a***@example.com").contains("SIMULADO");
        });
    }

    @Test
    @DisplayName("SI-15: la linea nunca lleva el correo completo")
    void enviar_laLineaNoLlevaElTelefonoCompleto() {
        canal.enviar(CORREO, "123456");

        assertThat(eventos.list).singleElement().satisfies(evento -> assertThat(evento.getFormattedMessage())
                .doesNotContain("ana.lopez").doesNotContain("lopez"));
    }

    @Test
    @DisplayName("el registro tiene nombre propio, distinto de cualquier clase, para poder permitirlo solo a el")
    void nombreDelRegistro_esPropio() {
        assertThat(CanalSimulado.NOMBRE_DEL_REGISTRO).isEqualTo("identidad.canal.simulado");
    }

    @Test
    @DisplayName("ultimoCodigoDe devuelve el ultimo codigo enviado a ese correo")
    void ultimoCodigoDe_devuelveElUltimo() {
        canal.enviar(CORREO, "111111");
        canal.enviar(CORREO, "222222");

        assertThat(canal.ultimoCodigoDe(CORREO)).contains("222222");
        assertThat(canal.ultimoCodigoDe("otra@example.com")).isEmpty();
    }
}
