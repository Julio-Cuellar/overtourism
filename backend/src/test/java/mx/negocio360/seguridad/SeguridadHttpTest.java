package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Fase 0 de IDN-4: la base de Spring Security. Todo lo que no se declara publico exige autenticacion y falla cerrado
 * (SG-01); las rutas de Identidad que validan su propio token siguen abiertas para la cadena de filtros.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SeguridadHttpTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private org.springframework.web.context.WebApplicationContext contexto;

    /** Solo la cadena de Spring Security, sin los filtros de servlet de la plataforma (entre ellos el de entrada). */
    private MockMvc soloSeguridad() {
        return org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup(contexto)
                .apply(org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    @DisplayName("SG-01: /graphql falla cerrado: si la peticion no paso por el filtro de entrada responde 401 con un cuerpo fijo")
    void graphql_sinElFiltroDeEntrada_responde401ConCuerpoFijo() throws Exception {
        var respuesta = soloSeguridad().perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ __typename }\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andReturn().getResponse();

        assertThat(respuesta.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(respuesta.getContentAsString()).isEqualTo("{\"codigo\":\"NO_AUTENTICADO\"}");
    }

    @Test
    @DisplayName("con la entrada que deja el filtro de /graphql, aunque sea anonima, la cadena deja pasar a GraphQL")
    void graphql_conLaEntradaDelFiltro_dejaPasar() throws Exception {
        var estado = soloSeguridad().perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .requestAttr(mx.negocio360.compartido.graphql.EntradaGraphQl.ATRIBUTO,
                                mx.negocio360.compartido.graphql.EntradaGraphQl.anonima(null))
                        .content("{\"query\":\"{ __typename }\"}"))
                .andReturn().getResponse().getStatus();

        assertThat(estado).isNotIn(401, 403);
    }

    @Test
    @DisplayName("sin token, por el camino completo, una consulta llega a GraphQL: lo publico no pide sesion y lo demas se niega ahi")
    void graphql_sinToken_llegaAGraphql() throws Exception {
        var estado = mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\":\"{ __typename }\"}"))
                .andReturn().getResponse().getStatus();

        assertThat(estado).isNotIn(401, 403);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/graphiql", "/graphql/schema", "/no-existe", "/actuator/env", "/error"})
    @DisplayName("fallar cerrado: una ruta que nadie declaro publica pide autenticacion, no responde 404")
    void rutaNoDeclarada_sinToken_responde401(String ruta) throws Exception {
        mvc.perform(get(ruta)).andExpect(status().isUnauthorized());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"/auth/codigo", "/auth/sesion", "/registro", "/registro/verificacion", "/registro/reenvio"})
    @DisplayName("las rutas de acceso de Identidad siguen abiertas: sin CSRF y sin 401 ni 403 de la cadena de filtros")
    void rutasDeAcceso_conCuerpoVacio_llegaAlControlador(String ruta) throws Exception {
        var estado = mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse().getStatus();

        assertThat(estado).isNotIn(401, 403);
    }

    @Test
    @DisplayName("sin estado: ninguna respuesta crea sesion ni cookie")
    void respuesta_noCreaSesionNiCookie() throws Exception {
        var respuesta = mvc.perform(post("/auth/codigo").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andReturn().getResponse();

        assertThat(respuesta.getHeader("Set-Cookie")).isNull();
        assertThat(respuesta.getCookies()).isEmpty();
    }

    @Test
    @DisplayName("las respuestas del filtro llevan las cabeceras de seguridad: sin cache y sin adivinar el tipo")
    void rechazo_llevaCabecerasDeSeguridad() throws Exception {
        var respuesta = mvc.perform(get("/no-existe")).andReturn().getResponse();

        assertThat(respuesta.getHeader("Cache-Control")).contains("no-store");
        assertThat(respuesta.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(respuesta.getHeader("X-Frame-Options")).isEqualTo("DENY");
    }
}
