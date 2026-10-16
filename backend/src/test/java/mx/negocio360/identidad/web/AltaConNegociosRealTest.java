package mx.negocio360.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.util.UUID;
import mx.negocio360.negocios.AltaVista.EstadoAlta;
import mx.negocio360.negocios.NegociosApi;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * La cadena completa con el modulo REAL de Negocios, sin el simulador de desarrollo (D-78, D-79): Identidad publica
 * {@code negocio.registro_solicitado}, Negocios crea el negocio y publica {@code negocio.registrado}, e Identidad deja a
 * la persona como DUENO. Todo por el bus y el outbox de Postgres. Vive aqui porque los pasos HTTP del registro son de
 * este paquete.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AltaConNegociosRealTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private NegociosApi negocios;

    @Test
    @DisplayName("pedir un negocio por HTTP termina con el negocio creado en Negocios y la persona como DUENO, sin simulador")
    void pedirNegocio_terminaConElModuloReal() throws Exception {
        var flujo = new FlujoHttp(mvc);
        var token = flujo.tokenDeRegistro(FlujoHttp.telefono(), FlujoHttp.ipNueva());

        var cuerpo = mvc.perform(post("/registro/negocio").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Fonda Doña Chela\",\"giro\":\"RESTAURANTE\"}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        var negocio = UUID.fromString(JsonPath.read(cuerpo, "$.negocioId"));

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> {
            var perfil = mvc.perform(get("/auth/yo").header("Authorization", "Bearer " + token))
                    .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
            assertThat(JsonPath.<java.util.List<?>>read(perfil, "$.negocios")).hasSize(1);
            assertThat(JsonPath.<String>read(perfil, "$.negocios[0].id")).isEqualTo(negocio.toString());
            assertThat(JsonPath.<String>read(perfil, "$.negocios[0].nombre")).isEqualTo("Fonda Doña Chela");
            assertThat(JsonPath.<String>read(perfil, "$.negocios[0].rol")).isEqualTo("DUENO");
            assertThat(JsonPath.<java.util.List<?>>read(perfil, "$.negociosPendientes")).isEmpty();
        });
        var perfil = mvc.perform(get("/auth/yo").header("Authorization", "Bearer " + token)).andReturn().getResponse()
                .getContentAsString();
        var cuenta = UUID.fromString(JsonPath.read(perfil, "$.cuenta.id"));
        assertThat(negocios.misAltas(cuenta)).singleElement().satisfies(alta -> {
            assertThat(alta.negocioId()).isEqualTo(negocio);
            assertThat(alta.estado()).isEqualTo(EstadoAlta.REGISTRADO);
        });
    }

    @Test
    @DisplayName("con una sesion REAL de Identidad: /graphql la reconoce, el dueno opera su panel con sus permisos reales y lo publico no pide sesion")
    void graphqlConUnaSesionReal() throws Exception {
        var flujo = new FlujoHttp(mvc);
        var token = flujo.tokenDeRegistro(FlujoHttp.telefono(), FlujoHttp.ipNueva());
        var cuerpo = mvc.perform(post("/registro/negocio").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Fonda Doña Chela\",\"giro\":\"RESTAURANTE\"}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        var negocio = JsonPath.<String>read(cuerpo, "$.negocioId");
        await().atMost(Duration.ofSeconds(20)).untilAsserted(() -> assertThat(JsonPath.<java.util.List<?>>read(
                mvc.perform(get("/auth/yo").header("Authorization", "Bearer " + token)).andReturn().getResponse()
                        .getContentAsString(), "$.negocios")).hasSize(1));

        var altas = graphql(token, "{ misAltasDeNegocio { negocioId estado } }");
        var panel = graphql(token, "{ miNegocio { id nombre estado completitud { porcentaje } } }");
        var edicion = graphql(token, "mutation { actualizarNegocio(input: { version: 0, descripcion: \"Comida de la casa\" }) "
                + "{ descripcion version } }");
        var disponibilidad = graphql(token, "mutation { definirDisponibilidad(nivel: LIBRE) { nivel } }");
        var anonima = graphql(null, "{ negocioPublico(id: \"" + negocio + "\") { id } }");
        var malToken = mvc.perform(post("/graphql").header("Authorization", "Bearer " + "z".repeat(43))
                .contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"{ misAltasDeNegocio { estado } }\"}"))
                .andReturn().getResponse().getStatus();

        assertThat(JsonPath.<String>read(altas, "$.data.misAltasDeNegocio[0].negocioId")).isEqualTo(negocio);
        assertThat(JsonPath.<String>read(altas, "$.data.misAltasDeNegocio[0].estado")).isEqualTo("REGISTRADO");
        // D-96, cambio 5: el DUENO de verdad trae los permisos negocios.* de su rol y opera su panel.
        assertThat(JsonPath.<String>read(panel, "$.data.miNegocio.id")).isEqualTo(negocio);
        assertThat(JsonPath.<String>read(panel, "$.data.miNegocio.nombre")).isEqualTo("Fonda Doña Chela");
        assertThat(JsonPath.<Integer>read(panel, "$.data.miNegocio.completitud.porcentaje")).isEqualTo(40);
        assertThat(JsonPath.<String>read(edicion, "$.data.actualizarNegocio.descripcion")).isEqualTo("Comida de la casa");
        assertThat(JsonPath.<Integer>read(edicion, "$.data.actualizarNegocio.version")).isEqualTo(1);
        assertThat(JsonPath.<String>read(disponibilidad, "$.data.definirDisponibilidad.nivel")).isEqualTo("LIBRE");
        assertThat(JsonPath.<Object>read(anonima, "$.data.negocioPublico")).isNull();
        assertThat(malToken).isEqualTo(401);
    }

    private String graphql(String token, String consulta) throws Exception {
        var peticion = post("/graphql").contentType(MediaType.APPLICATION_JSON)
                .content("{\"query\":\"" + consulta.replace("\"", "\\\"") + "\"}");
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        return mvc.perform(peticion).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
    }
}
