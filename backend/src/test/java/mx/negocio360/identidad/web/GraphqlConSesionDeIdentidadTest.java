package mx.negocio360.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import mx.negocio360.seguridad.SesionDePrueba;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * IDN-4, Fase 1: /graphql autentica con la sesion que emite Identidad (SG-01) y el resolvedor recibe el negocio y el
 * actor de esa sesion, nunca del cliente (SG-02). Usa los pasos HTTP reales de registro, negocio y acceso.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GraphqlConSesionDeIdentidadTest {

    private static final String CONSULTA = "{\"query\":\"{ contextoDePrueba }\"}";

    @Autowired
    private MockMvc mvc;

    private FlujoHttp flujo;

    @BeforeEach
    void preparar() {
        flujo = new FlujoHttp(mvc);
    }

    /** POST /graphql; si Spring GraphQL responde de forma asincrona, completa el despacho. */
    private MvcResult graphql(String token, String cuerpo, String... cabeceras) throws Exception {
        var peticion = post("/graphql").contentType(MediaType.APPLICATION_JSON).content(cuerpo);
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        for (var i = 0; i + 1 < cabeceras.length; i += 2) {
            peticion.header(cabeceras[i], cabeceras[i + 1]);
        }
        var resultado = mvc.perform(peticion).andReturn();
        return resultado.getRequest().isAsyncStarted() ? mvc.perform(asyncDispatch(resultado)).andReturn() : resultado;
    }

    private String tokenSinNegocio() throws Exception {
        return flujo.tokenDeRegistro(FlujoHttp.telefono(), FlujoHttp.ipNueva());
    }

    private FlujoHttp.SesionConNegocio tokenConNegocio() throws Exception {
        return flujo.sesionConNegocio();
    }

    @Test
    @DisplayName("SG-01 y SG-02: con una sesion valida el resolvedor ve el negocio activo y la cuenta de esa sesion")
    void sesionValida_elResolvedorRecibeElContextoDeLaSesion() throws Exception {
        var sesion = tokenConNegocio();

        var respuesta = graphql(sesion.token(), CONSULTA);

        assertThat(respuesta.getResponse().getStatus()).isEqualTo(200);
        String contexto = JsonPath.read(respuesta.getResponse().getContentAsString(), "$.data.contextoDePrueba");
        assertThat(contexto).startsWith(sesion.negocio() + "|");
        assertThat(contexto.substring(contexto.indexOf('|') + 1)).matches("[0-9a-f-]{36}");
    }

    @Test
    @DisplayName("SG-02: un negocio que el cliente mande por cabecera o variable se ignora")
    void negocioDelCliente_seIgnora() throws Exception {
        var sesion = tokenConNegocio();
        var ajeno = UUID.randomUUID().toString();

        var cabecera = graphql(sesion.token(), CONSULTA, "X-Negocio-Id", ajeno, "X-Negocio", ajeno);
        var variable = graphql(sesion.token(), "{\"query\":\"{ contextoDePrueba }\",\"variables\":{\"negocioId\":\""
                + ajeno + "\"}}");

        for (var respuesta : new MvcResult[] {cabecera, variable}) {
            String contexto = JsonPath.read(respuesta.getResponse().getContentAsString(), "$.data.contextoDePrueba");
            assertThat(contexto).startsWith(sesion.negocio() + "|").doesNotContain(ajeno);
        }
    }

    @Test
    @DisplayName("SG-02: pedir un argumento negocioId es un error de validacion, no un cambio de negocio")
    void argumentoNegocioId_noExiste() throws Exception {
        var sesion = tokenConNegocio();

        var respuesta = graphql(sesion.token(), "{\"query\":\"{ contextoDePrueba(negocioId: \\\"x\\\") }\"}");

        var cuerpo = respuesta.getResponse().getContentAsString();
        assertThat(JsonPath.<Object>read(cuerpo, "$.errors")).isNotNull();
        assertThat(cuerpo).doesNotContain(sesion.negocio().toString());
    }

    @Test
    @DisplayName("AZ-04: una sesion valida sin negocio activo llega a GraphQL, pero el campo se niega con SIN_NEGOCIO_ACTIVO")
    void sesionSinNegocioActivo_noOpera() throws Exception {
        var respuesta = graphql(tokenSinNegocio(), CONSULTA);

        assertThat(respuesta.getResponse().getStatus()).isEqualTo(200);
        var cuerpo = respuesta.getResponse().getContentAsString();
        assertThat(JsonPath.<String>read(cuerpo, "$.errors[0].extensions.codigo")).isEqualTo("SIN_NEGOCIO_ACTIVO");
        assertThat(cuerpo).doesNotContain("PERMISO_DENEGADO");
    }

    @Test
    @DisplayName("SG-01 y AS-08: con token falso o revocado la respuesta es exactamente la misma: 401 con cuerpo fijo")
    void tokenFalsoORevocado_mismaRespuesta() throws Exception {
        var sesion = tokenConNegocio();
        mvc.perform(delete("/auth/sesion").header("Authorization", "Bearer " + sesion.token()))
                .andExpect(status().is2xxSuccessful());

        var falso = graphql("A".repeat(43), CONSULTA).getResponse();
        var revocado = graphql(sesion.token(), CONSULTA).getResponse();

        for (var respuesta : new org.springframework.mock.web.MockHttpServletResponse[] {falso, revocado}) {
            assertThat(respuesta.getStatus()).isEqualTo(401);
            assertThat(respuesta.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
            assertThat(JsonPath.<String>read(respuesta.getContentAsString(), "$.errors[0].extensions.codigo"))
                    .isEqualTo("NO_AUTENTICADO");
        }
        assertThat(falso.getContentAsString()).isEqualTo(revocado.getContentAsString());
    }

    @Test
    @DisplayName("sin token la peticion llega a GraphQL (lo publico no pide sesion) y lo que pide negocio se niega con NO_AUTENTICADO")
    void tokenAusente_llegaAGraphqlYSeNiega() throws Exception {
        var ausente = graphql(null, CONSULTA).getResponse();

        assertThat(ausente.getStatus()).isEqualTo(200);
        var cuerpo = ausente.getContentAsString();
        assertThat(JsonPath.<String>read(cuerpo, "$.errors[0].extensions.codigo")).isEqualTo("NO_AUTENTICADO");
        assertThat(cuerpo).doesNotContain(SesionDePrueba.NEGOCIO.toString());
    }

    @Test
    @DisplayName("un token valido de una sesion no sirve en otra cuenta: cada sesion ve solo su negocio")
    void dosSesiones_cadaUnaVeSuNegocio() throws Exception {
        var una = tokenConNegocio();
        var otra = tokenConNegocio();

        String delaUna = JsonPath.read(graphql(una.token(), CONSULTA).getResponse().getContentAsString(),
                "$.data.contextoDePrueba");
        String delaOtra = JsonPath.read(graphql(otra.token(), CONSULTA).getResponse().getContentAsString(),
                "$.data.contextoDePrueba");

        assertThat(delaUna).startsWith(una.negocio() + "|");
        assertThat(delaOtra).startsWith(otra.negocio() + "|");
        assertThat(una.negocio()).isNotEqualTo(otra.negocio());
    }
}
