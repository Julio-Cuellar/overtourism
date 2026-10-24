package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * QR-21 con la aplicacion completa: la web publica del comensal, desde su propio origen, hace el prevuelo y luego escanea la
 * mesa sin sesion; un origen que no esta en la lista no pasa ni el prevuelo ni el POST. Sin cookies ni credenciales.
 */
@SpringBootTest(properties = "seguridad.http.origenes-permitidos=https://web.servicial.test")
@AutoConfigureMockMvc
class CorsGraphqlTest {

    private static final String WEB = "https://web.servicial.test";
    private static final String CONSULTA = "{\"query\":\"{ mesaPorQr(token: \\\"no-existe\\\") { mesa } }\"}";

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("el prevuelo del origen permitido se contesta 204 sin sesion y con las cabeceras que el navegador necesita")
    void prevuelo() throws Exception {
        mvc.perform(options("/graphql").header("Origin", WEB).header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "content-type,idempotency-key,x-dispositivo-id")
                        .with(r -> {
                            r.setRemoteAddr("10.5.1.1");
                            return r;
                        }))
                .andExpect(status().isNoContent())
                .andExpect(header().string("Access-Control-Allow-Origin", WEB))
                .andExpect(header().string("Access-Control-Allow-Methods", "POST"))
                .andExpect(header().string("Access-Control-Max-Age", "600"));
    }

    @Test
    @DisplayName("un POST publico desde el origen permitido responde y la respuesta lleva su origen para que el navegador la lea")
    void postPublico() throws Exception {
        var respuesta = mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(CONSULTA)
                        .header("Origin", WEB).header("X-Dispositivo-Id", "5b0c1f0e-6a11-4d7e-9a3c-0d2f4e8a7b11").with(r -> {
                            r.setRemoteAddr("10.5.1.2");
                            return r;
                        }))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", WEB))
                .andExpect(header().string("Vary", org.hamcrest.Matchers.containsString("Origin")))
                .andReturn().getResponse();

        assertThat(respuesta.getContentAsString()).contains("\"mesaPorQr\":null");
        assertThat(respuesta.getHeader("Access-Control-Allow-Credentials")).isNull();
    }

    @Test
    @DisplayName("un rechazo tambien lleva el origen, para que la web pueda leer el error y mostrarlo")
    void rechazoLeible() throws Exception {
        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(CONSULTA).header("Origin", WEB)
                        .header("Idempotency-Key", "corta").with(r -> {
                            r.setRemoteAddr("10.5.1.3");
                            return r;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("Access-Control-Allow-Origin", WEB));
    }

    @Test
    @DisplayName("un origen ajeno recibe 403 en el prevuelo y en el POST, sin cabeceras CORS y sin llegar a GraphQL")
    void origenAjeno() throws Exception {
        for (var peticion : new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[] {
                options("/graphql").header("Origin", "https://malo.test").header("Access-Control-Request-Method", "POST"),
                post("/graphql").contentType(MediaType.APPLICATION_JSON).content(CONSULTA).header("Origin", "https://malo.test")}) {
            mvc.perform(peticion.with(r -> {
                        r.setRemoteAddr("10.5.1.4");
                        return r;
                    }))
                    .andExpect(status().isForbidden())
                    .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
        }
    }

    @Test
    @DisplayName("sin Origin todo sigue como antes: el POST publico responde y no se agregan cabeceras CORS")
    void sinOrigen() throws Exception {
        mvc.perform(post("/graphql").contentType(MediaType.APPLICATION_JSON).content(CONSULTA).with(r -> {
                    r.setRemoteAddr("10.5.1.5");
                    return r;
                }))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
