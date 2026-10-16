package mx.negocio360.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

/**
 * SG-10 de punta a punta con sesiones reales de Identidad. Aqui los topes se bajan a 5 por minuto por cuenta y 3 fallos
 * por minuto por IP para no hacer decenas de peticiones; los valores iniciales (60 y 20) los fija PropiedadesTasaTest.
 * Cada prueba usa su propia IP y su propia cuenta, porque el limitador es de todo el contexto.
 */
@SpringBootTest(properties = {"seguridad.tasa.peticiones-por-minuto-cuenta=5", "seguridad.tasa.fallos-por-minuto-ip=3"})
@AutoConfigureMockMvc
class TasaGraphqlConSesionTest {

    private static final String CONSULTA = "{\"query\":\"{ arbolDePrueba { valor } }\"}";

    @Autowired
    private MockMvc mvc;

    private MockHttpServletResponse graphql(String token, String ip) throws Exception {
        var peticion = post("/graphql").contentType(MediaType.APPLICATION_JSON).content(CONSULTA)
                .with(FlujoHttp.desde(ip));
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        var resultado = mvc.perform(peticion).andReturn();
        return (resultado.getRequest().isAsyncStarted() ? mvc.perform(asyncDispatch(resultado)).andReturn() : resultado)
                .getResponse();
    }

    @Test
    @DisplayName("una cuenta puede 5 peticiones por minuto; la 6 recibe 429 con Retry-After; otra cuenta no se afecta")
    void porCuenta_cincoYLaSeisSeNiega() throws Exception {
        var flujo = new FlujoHttp(mvc);
        var una = flujo.sesionConNegocio().token();
        var otra = flujo.sesionConNegocio().token();
        var ip = FlujoHttp.ipNueva();

        for (var i = 0; i < 5; i++) {
            assertThat(graphql(una, ip).getStatus()).as("peticion %d", i + 1).isEqualTo(200);
        }
        var negada = graphql(una, ip);

        assertThat(negada.getStatus()).isEqualTo(429);
        assertThat(com.jayway.jsonpath.JsonPath.<String>read(negada.getContentAsString(), "$.errors[0].extensions.codigo"))
                .isEqualTo("DEMASIADAS_SOLICITUDES");
        assertThat(Integer.parseInt(negada.getHeader("Retry-After"))).isBetween(1, 60);
        assertThat(negada.getHeader("Cache-Control")).contains("no-store");
        assertThat(negada.getHeader("Set-Cookie")).isNull();
        assertThat(graphql(otra, ip).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("las peticiones anonimas son validas (lo publico no pide sesion): no gastan el tope de fallos de la IP")
    void porIp_anonimas_noSonFallos() throws Exception {
        var ip = FlujoHttp.ipNueva();

        for (var i = 0; i < 10; i++) {
            assertThat(graphql(null, ip).getStatus()).as("peticion %d", i + 1).isEqualTo(200);
        }

        assertThat(graphql("A".repeat(43), ip).getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("adivinar tokens cuenta igual: 3 tokens falsos y el siguiente intento ya recibe 429")
    void porIp_tokensFalsos_seCuentanComoFallos() throws Exception {
        var ip = FlujoHttp.ipNueva();

        for (var i = 0; i < 3; i++) {
            assertThat(graphql("A".repeat(43), ip).getStatus()).isEqualTo(401);
        }

        assertThat(graphql("B".repeat(43), ip).getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("una IP bloqueada por fallos recibe 429 aunque traiga una sesion valida (se documenta: IP compartida)")
    void porIp_bloqueada_noDejaPasarNiConSesionValida() throws Exception {
        var sesion = new FlujoHttp(mvc).sesionConNegocio().token();
        var ip = FlujoHttp.ipNueva();
        for (var i = 0; i < 3; i++) {
            graphql("A".repeat(43), ip);
        }

        assertThat(graphql(sesion, ip).getStatus()).isEqualTo(429);
        assertThat(graphql(sesion, FlujoHttp.ipNueva()).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("lo que la puerta HTTP rechaza (405, 415...) no gasta fichas de la IP")
    void porIp_rechazosDeLaPuertaNoCuentan() throws Exception {
        var ip = FlujoHttp.ipNueva();

        for (var i = 0; i < 20; i++) {
            assertThat(mvc.perform(get("/graphql").with(FlujoHttp.desde(ip))).andReturn().getResponse().getStatus())
                    .isEqualTo(405);
        }

        assertThat(graphql("A".repeat(43), ip).getStatus()).isEqualTo(401);
    }
}
