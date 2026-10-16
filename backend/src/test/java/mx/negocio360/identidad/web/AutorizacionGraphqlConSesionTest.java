package mx.negocio360.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SG-03 y SG-19 de punta a punta con el rol real: quien registra un negocio es DUENO, y el DUENO tiene casi todos los
 * permisos de Clientes, incluidos los sensibles. Aun asi, las operaciones sensibles se niegan por omision.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AutorizacionGraphqlConSesionTest {

    @Autowired
    private MockMvc mvc;

    private List<String> codigos(String token, String cuerpo) throws Exception {
        var resultado = mvc.perform(post("/graphql").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(cuerpo)).andReturn();
        var respuesta = (resultado.getRequest().isAsyncStarted() ? mvc.perform(asyncDispatch(resultado)).andReturn()
                : resultado).getResponse().getContentAsString();
        try {
            List<?> codigos = JsonPath.read(respuesta, "$.errors[*].extensions.codigo");
            return codigos.stream().map(String::valueOf).toList();
        } catch (com.jayway.jsonpath.PathNotFoundException sinErrores) {
            return List.of();
        }
    }

    @Test
    @DisplayName("el DUENO pasa una operacion con permiso, pero una sensible pide confirmacion reforzada")
    void dueno_permisoSi_sensibleRequiereConfirmacion() throws Exception {
        var token = new FlujoHttp(mvc).sesionConNegocio().token();

        var permitida = mvc.perform(post("/graphql").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"query\":\"{ soloConPermisoDePrueba }\"}")).andReturn();
        var cuerpo = (permitida.getRequest().isAsyncStarted() ? mvc.perform(asyncDispatch(permitida)).andReturn()
                : permitida).getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(cuerpo, "$.data.soloConPermisoDePrueba")).isEqualTo("paso");

        assertThat(codigos(token, "{\"query\":\"{ sensibleDePrueba }\"}")).containsExactly("CONFIRMACION_REQUERIDA");
    }

    @Test
    @DisplayName("el DUENO prepara un comando comun, pero anonimizar a un cliente (sensible) se niega por omision")
    void dueno_comandoComun_sensibleSeNiega() throws Exception {
        var token = new FlujoHttp(mvc).sesionConNegocio().token();
        var id = "0192f3a0-7c1e-7b52-9d41-5a1c3e8f2b10";

        var comun = "{\"query\":\"mutation($c: ComandoInput!) { comandoDePrueba(input: $c) }\",\"variables\":"
                + "{\"c\":{\"registrarCliente\":{\"nombre\":\"Ana\",\"avisoVersion\":1}}}}";
        var sensible = "{\"query\":\"mutation($c: ComandoInput!) { comandoDePrueba(input: $c) }\",\"variables\":"
                + "{\"c\":{\"anonimizarCliente\":{\"clienteId\":\"" + id + "\"}}}}";

        assertThat(codigos(token, comun)).isEmpty();
        assertThat(codigos(token, sensible)).containsExactly("CONFIRMACION_REQUERIDA");
    }
}
