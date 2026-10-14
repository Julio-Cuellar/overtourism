package mx.negocio360.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SI-15: ni codigos, ni tokens, ni telefonos, ni nombres en los registros. Corre con el registro de Spring MVC en
 * DEBUG, que es donde el framework escribe el contenido de las peticiones y las respuestas.
 */
@SpringBootTest(properties = "logging.level.org.springframework.web=DEBUG")
@AutoConfigureMockMvc
class RegistrosSinSecretosTest {

    private static final String TELEFONO = "2226660001";
    private static final String IP = "10.9.9.9";

    @Autowired
    private MockMvc mvc;

    private Logger raiz;
    private ListAppender<ILoggingEvent> registros;

    @BeforeEach
    void conectar() {
        raiz = (Logger) LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME);
        registros = new ListAppender<>();
        registros.start();
        raiz.addAppender(registros);
    }

    @AfterEach
    void desconectar() {
        raiz.detachAppender(registros);
    }

    private static String texto(ILoggingEvent evento) {
        var proxy = evento.getThrowableProxy();
        return evento.getFormattedMessage() + (proxy == null ? "" : " " + proxy.getMessage());
    }

    @Test
    @DisplayName("un recorrido completo, con aciertos y fallos, no deja secretos en los registros")
    void recorridoCompleto_noDejaSecretosEnLosRegistros() throws Exception {
        mvc.perform(post("/auth/codigo").contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefono\":\"" + TELEFONO + "\"}").with(r -> {
                    r.setRemoteAddr(IP);
                    return r;
                })).andExpect(status().isAccepted());
        var codigo = (String) JsonPath.read(mvc.perform(get("/dev/canal/codigo").param("telefono", TELEFONO))
                .andReturn().getResponse().getContentAsString(), "$.codigo");
        var incorrecto = "000000".equals(codigo) ? "000001" : "000000";
        mvc.perform(post("/auth/sesion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefono\":\"" + TELEFONO + "\",\"codigo\":\"" + incorrecto + "\",\"nombre\":\"Ana Prueba\"}"))
                .andExpect(status().isUnauthorized());
        var sesion = mvc.perform(post("/auth/sesion").contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefono\":\"" + TELEFONO + "\",\"codigo\":\"" + codigo + "\",\"nombre\":\"Ana Prueba\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var token = (String) JsonPath.read(sesion, "$.token");
        mvc.perform(get("/auth/yo").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(patch("/cuenta").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana Cambiada\"}")).andExpect(status().isOk());
        mvc.perform(get("/auth/yo").header("Authorization", "Bearer basura-inventada")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/auth/sesion").header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());

        List<String> lineas = registros.list.stream().map(RegistrosSinSecretosTest::texto).toList();
        assertThat(lineas).as("el recorrido debe haber escrito algo para que la prueba tenga sentido").isNotEmpty();
        for (var secreto : List.of(codigo, incorrecto, token, TELEFONO, "+52" + TELEFONO, "Ana Prueba", "Ana Cambiada",
                "basura-inventada")) {
            assertThat(lineas).as("registros con «%s»", secreto).noneMatch(linea -> linea.contains(secreto));
        }
    }
}
