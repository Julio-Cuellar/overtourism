package mx.negocio360.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import mx.negocio360.seguridad.CapturaDeLogs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** SG-13 y SG-20 de punta a punta: sesion real de Identidad, HTTP, motor real y el registro que queda. */
@SpringBootTest
@AutoConfigureMockMvc
class ErroresGraphqlConSesionTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("un error del resolvedor sale enmascarado por HTTP y deja una sola linea de registro con la cuenta y el negocio")
    void errorDeResolvedor_saleEnmascaradoYSeRegistra() throws Exception {
        var sesion = new FlujoHttp(mvc).sesionConNegocio();

        try (var captura = new CapturaDeLogs("auditoria.graphql")) {
            var resultado = mvc.perform(post("/graphql").header("Authorization", "Bearer " + sesion.token())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"query\":\"{ permisoDenegadoDePrueba fallaInternaDePrueba }\"}")).andReturn();
            var respuesta = (resultado.getRequest().isAsyncStarted() ? mvc.perform(asyncDispatch(resultado)).andReturn()
                    : resultado).getResponse();

            assertThat(respuesta.getStatus()).isEqualTo(200);
            var cuerpo = respuesta.getContentAsString();
            assertThat(JsonPath.<String>read(cuerpo, "$.errors[0].message")).isEqualTo("PERMISO_DENEGADO");
            assertThat(JsonPath.<String>read(cuerpo, "$.errors[1].message")).isEqualTo("ERROR_INTERNO");
            assertThat(cuerpo).doesNotContain("secreto", "telefono", "2221234567", "anonimizar", "IllegalState");

            var lineas = captura.lineas();
            assertThat(lineas).hasSize(1);
            assertThat(lineas.get(0)).contains("negocio=" + sesion.negocio())
                    .contains("operacion=query:permisoDenegadoDePrueba,fallaInternaDePrueba")
                    .contains("resultado=ERROR").contains("errores=PERMISO_DENEGADO,ERROR_INTERNO")
                    .doesNotContain("secreto", "Bearer", sesion.token());
        }
    }
}
