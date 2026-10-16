package mx.negocio360.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/** Los limites de consulta (SG-06, SG-08 y SG-09) de punta a punta: HTTP, sesion real de Identidad y motor real. */
@SpringBootTest
@AutoConfigureMockMvc
class LimitesGraphqlConSesionTest {

    @Autowired
    private MockMvc mvc;

    private String token;

    @BeforeEach
    void abrirSesion() throws Exception {
        token = new FlujoHttp(mvc).sesionConNegocio().token();
    }

    private MvcResult graphql(String consulta) throws Exception {
        var cuerpo = "{\"query\":\"" + consulta.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
        var resultado = mvc.perform(post("/graphql").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
        return resultado.getRequest().isAsyncStarted() ? mvc.perform(asyncDispatch(resultado)).andReturn() : resultado;
    }

    @Test
    @DisplayName("una consulta normal con sesion responde sus datos")
    void consultaNormal_respondeDatos() throws Exception {
        var respuesta = graphql("{ arbolDePrueba { valor } }").getResponse();

        assertThat(respuesta.getStatus()).isEqualTo(200);
        assertThat(JsonPath.<String>read(respuesta.getContentAsString(), "$.data.arbolDePrueba.valor")).isEqualTo("hoja");
    }

    @Test
    @DisplayName("SG-06: una consulta demasiado profunda recibe el error fijo, tambien por HTTP")
    void consultaProfunda_recibeErrorFijo() throws Exception {
        var consulta = "{ arbolDePrueba { hijo { hijo { hijo { hijo { hijo { hijo { valor } } } } } } } }";

        var respuesta = graphql(consulta).getResponse();

        assertThat(respuesta.getStatus()).isEqualTo(200);
        assertThat(JsonPath.<String>read(respuesta.getContentAsString(), "$.errors[0].message"))
                .isEqualTo("CONSULTA_MUY_PROFUNDA");
        assertThat(JsonPath.<String>read(respuesta.getContentAsString(), "$.errors[0].extensions.codigo"))
                .isEqualTo("CONSULTA_MUY_PROFUNDA");
        assertThat(respuesta.getContentAsString()).doesNotContain("hijo");
    }

    @Test
    @DisplayName("SG-09: una consulta de exactamente 10000 caracteres pasa")
    void consultaEnElTope_pasa() throws Exception {
        var base = "{ arbolDePrueba { valor } }";

        var respuesta = graphql(base + " ".repeat(10_000 - base.length())).getResponse();

        assertThat(respuesta.getStatus()).isEqualTo(200);
        assertThat(JsonPath.<String>read(respuesta.getContentAsString(), "$.data.arbolDePrueba.valor")).isEqualTo("hoja");
    }

    @Test
    @DisplayName("SG-09: con un caracter mas, el lector de JSON la rechaza con 400 y sin cuerpo, antes de llegar al motor")
    void consultaUnCaracterMas_seRechazaConBadRequest() throws Exception {
        var base = "{ arbolDePrueba { valor } }";

        var respuesta = graphql(base + " ".repeat(10_001 - base.length())).getResponse();

        assertThat(respuesta.getStatus()).isEqualTo(400);
        assertThat(respuesta.getContentAsString()).isEmpty();
    }

    @Test
    @DisplayName("SG-08: demasiados campos en la raiz se rechazan antes de ejecutar el resolvedor del contexto")
    void camposRaiz_seRechazanSinEjecutar() throws Exception {
        var consulta = "{ a: contextoDePrueba b: contextoDePrueba c: contextoDePrueba d: contextoDePrueba "
                + "e: contextoDePrueba f: contextoDePrueba }";

        var respuesta = graphql(consulta).getResponse();

        assertThat(JsonPath.<String>read(respuesta.getContentAsString(), "$.errors[0].message"))
                .isEqualTo("DEMASIADOS_CAMPOS_RAIZ");
        assertThat(respuesta.getContentAsString()).doesNotContain("|");
    }
}
