package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La puerta HTTP dentro de la cadena real de filtros: se aplica antes de autenticar (un cuerpo enorme no llega ni a
 * Identidad) y sus respuestas llevan las cabeceras de seguridad. El origen permitido sale de la configuracion de
 * pruebas ({@code seguridad.http.origenes-permitidos}).
 */
@SpringBootTest
@AutoConfigureMockMvc
class HttpEstrictoGraphqlTest {

    private static final String OBJETO = "{\"query\":\"{ __typename }\"}";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("GET /graphql responde 405 antes de pedir autenticacion")
    void get_responde405() throws Exception {
        var respuesta = mvc.perform(get("/graphql")).andReturn().getResponse();

        assertThat(respuesta.getStatus()).isEqualTo(405);
        assertThat(respuesta.getHeader("Allow")).isEqualTo("POST");
    }

    @Test
    @DisplayName("un tipo de contenido distinto de JSON responde 415 aunque no haya token")
    void textoPlano_responde415() throws Exception {
        var respuesta = mvc.perform(post("/graphql").contentType(MediaType.TEXT_PLAIN).content(OBJETO))
                .andReturn().getResponse();

        assertThat(respuesta.getStatus()).isEqualTo(415);
    }

    @Test
    @DisplayName("un lote de consultas (arreglo) responde 400")
    void lote_responde400() throws Exception {
        var respuesta = mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content("[" + OBJETO + "]"))
                .andReturn().getResponse();

        assertThat(respuesta.getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("un cuerpo de mas de 64 KB responde 413 y no llega a autenticar")
    void cuerpoGrande_responde413() throws Exception {
        var grande = ("{\"query\":\"" + "x".repeat(70_000) + "\"}").getBytes(StandardCharsets.UTF_8);

        var respuesta = mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(grande))
                .andReturn().getResponse();

        assertThat(respuesta.getStatus()).isEqualTo(413);
    }

    @Test
    @DisplayName("un origen ajeno responde 403; el configurado y la ausencia de Origin llegan a GraphQL (200)")
    void origen_seVerificaContraLaConfiguracion() throws Exception {
        var ajeno = mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(OBJETO)
                .header("Origin", "https://malo.test")).andReturn().getResponse();
        var permitido = mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(OBJETO)
                .header("Origin", "https://panel.ejemplo.test")).andReturn().getResponse();
        var sinOrigen = mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(OBJETO))
                .andReturn().getResponse();

        assertThat(ajeno.getStatus()).isEqualTo(403);
        assertThat(permitido.getStatus()).isEqualTo(200);
        assertThat(sinOrigen.getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("las respuestas de la puerta llevan las cabeceras de seguridad y ningun detalle")
    void rechazos_llevanCabecerasYCuerpoFijo() throws Exception {
        var respuesta = mvc.perform(get("/graphql")).andReturn().getResponse();

        assertThat(respuesta.getHeader("Cache-Control")).contains("no-store");
        assertThat(respuesta.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        var cuerpo = respuesta.getContentAsString();
        assertThat(com.jayway.jsonpath.JsonPath.<String>read(cuerpo, "$.errors[0].extensions.codigo"))
                .isEqualTo("METODO_NO_PERMITIDO");
        assertThat(com.jayway.jsonpath.JsonPath.<String>read(cuerpo, "$.errors[0].message")).isEqualTo("Solo se acepta POST.");
        assertThat(cuerpo).doesNotContain("Exception").doesNotContain("graphql").doesNotContain("at mx");
    }
}
