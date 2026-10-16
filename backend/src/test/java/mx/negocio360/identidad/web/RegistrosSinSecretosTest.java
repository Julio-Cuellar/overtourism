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
 * SI-15: ni codigos, ni tokens, ni correos, ni telefonos, ni nombres en los registros. Corre con el registro de Spring MVC en
 * DEBUG, que es donde el framework escribe el contenido de las peticiones y las respuestas.
 */
@SpringBootTest(properties = "logging.level.org.springframework.web=DEBUG")
@AutoConfigureMockMvc
class RegistrosSinSecretosTest {

    private static final String TELEFONO = "2226660001";
    private static final String IP = "10.9.9.9";
    private static final String CANAL = "identidad.canal.simulado";
    private static final String CORREO = "ana.secreta@example.com";
    private static final String OTRO_TELEFONO = "2226660002";
    private static final String OTRO_CORREO = "beto.secreto@example.com";

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
    @DisplayName("un recorrido completo, con aciertos y fallos, no deja secretos en los registros; solo el canal simulado escribe el codigo")
    void recorridoCompleto_noDejaSecretosEnLosRegistros() throws Exception {
        mvc.perform(post("/registro").contentType(MediaType.APPLICATION_JSON).content(formulario(TELEFONO, CORREO))
                .with(r -> {
                    r.setRemoteAddr(IP);
                    return r;
                })).andExpect(status().isAccepted());
        var codigo = codigoDe(CORREO);
        var incorrecto = "000000".equals(codigo) ? "000001" : "000000";
        mvc.perform(post("/registro/verificacion").contentType(MediaType.APPLICATION_JSON).content(verificacion(incorrecto)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/registro/reenvio").contentType(MediaType.APPLICATION_JSON)
                .content("{\"correo\":\"" + CORREO + "\"}")).andExpect(status().isAccepted());
        var nuevo = codigoDe(CORREO);
        var sesion = mvc.perform(post("/registro/verificacion").contentType(MediaType.APPLICATION_JSON)
                .content(verificacion(nuevo))).andExpect(status().isCreated()).andReturn().getResponse()
                .getContentAsString();
        var token = (String) JsonPath.read(sesion, "$.token");
        mvc.perform(get("/auth/yo").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(patch("/cuenta").header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana Cambiada\"}")).andExpect(status().isOk());
        mvc.perform(get("/auth/yo").header("Authorization", "Bearer basura-inventada")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/auth/sesion").header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());

        var ajenos = registros.list.stream().filter(e -> !CANAL.equals(e.getLoggerName())).map(RegistrosSinSecretosTest::texto)
                .toList();
        assertThat(ajenos).as("el recorrido debe haber escrito algo para que la prueba tenga sentido").isNotEmpty();
        for (var secreto : List.of(codigo, incorrecto, nuevo, token, TELEFONO, "+52" + TELEFONO, "Ana Prueba", "Ana Cambiada",
                "Pérez Gómez", CORREO, "ana.secreta", "basura-inventada")) {
            assertThat(ajenos).as("registros con «%s»", secreto).noneMatch(linea -> linea.contains(secreto));
        }
    }

    @Test
    @DisplayName("D-77: el canal simulado es el unico que escribe el codigo, y lo hace con el correo enmascarado")
    void canalSimulado_esLaUnicaExcepcion() throws Exception {
        mvc.perform(post("/registro").contentType(MediaType.APPLICATION_JSON)
                .content(formulario(OTRO_TELEFONO, OTRO_CORREO)).with(r -> {
                    r.setRemoteAddr("10.9.9.10");
                    return r;
                })).andExpect(status().isAccepted());
        var codigo = codigoDe(OTRO_CORREO);

        var delCanal = registros.list.stream().filter(e -> CANAL.equals(e.getLoggerName())).map(RegistrosSinSecretosTest::texto)
                .toList();

        assertThat(delCanal).singleElement().satisfies(linea -> assertThat(linea).contains(codigo)
                .contains("b***@example.com").doesNotContain(OTRO_CORREO).doesNotContain("beto.secreto")
                .doesNotContain(OTRO_TELEFONO));
    }

    private String codigoDe(String correo) throws Exception {
        return JsonPath.read(mvc.perform(get("/dev/canal/codigo").param("correo", correo)).andReturn().getResponse()
                .getContentAsString(), "$.codigo");
    }

    private static String formulario(String telefono, String correo) {
        return "{\"nombre\":\"Ana Prueba\",\"apellidos\":\"Pérez Gómez\",\"correo\":\"" + correo
                + "\",\"telefono\":\"" + telefono + "\",\"aceptaAviso\":true}";
    }

    private static String verificacion(String codigo) {
        return "{\"correo\":\"" + CORREO + "\",\"codigo\":\"" + codigo + "\"}";
    }
}
