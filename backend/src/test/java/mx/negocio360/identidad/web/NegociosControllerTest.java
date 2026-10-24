package mx.negocio360.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * IDN-2 y D-78 · REST del negocio de la persona: pedir un negocio (publica un evento, no lo registra), el negocio
 * activo, el perfil con rol y permisos, y los roles y permisos del giro. El modulo de negocios se simula con
 * {@code POST /dev/negocios/{id}/registrado}, que solo existe en desarrollo y demo.
 */
@SpringBootTest
@AutoConfigureMockMvc
class NegociosControllerTest {

    @Autowired
    private MockMvc mvc;

    private FlujoHttp flujo;

    @BeforeEach
    void preparar() {
        flujo = new FlujoHttp(mvc);
    }

    private static String telefono() {
        return FlujoHttp.telefono();
    }

    private static String ipNueva() {
        return FlujoHttp.ipNueva();
    }

    private static MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder peticion, String token) {
        return peticion.header("Authorization", "Bearer " + token);
    }

    /** Registra una cuenta nueva (el formulario y la verificacion del correo) y devuelve su token. */
    private String registrar(String tel) throws Exception {
        return flujo.tokenDeRegistro(tel, ipNueva());
    }

    private UUID pedirNegocio(String token, String nombre, String giro) throws Exception {
        var cuerpo = mvc.perform(conToken(post("/registro/negocio"), token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"" + nombre + "\",\"giro\":\"" + giro + "\"}")).andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(cuerpo, "$.negocioId"));
    }

    private ResultActions elModuloDeNegociosResponde(UUID negocioId) throws Exception {
        return mvc.perform(post("/dev/negocios/" + negocioId + "/registrado"));
    }

    @Test
    @DisplayName("POST /registro/negocio responde 202 con el id del negocio pedido y no crea el negocio todavia")
    void pedirNegocio_responde202SinCrearNada() throws Exception {
        var token = registrar(telefono());

        var negocio = pedirNegocio(token, "Fonda Doña Chela", "restaurante");

        assertThat(negocio.version()).isEqualTo(7);
        mvc.perform(conToken(get("/auth/yo"), token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.negocios").isEmpty()).andExpect(jsonPath("$.negocioActivo").value(nullValue()))
                .andExpect(jsonPath("$.negociosPendientes.length()").value(1))
                .andExpect(jsonPath("$.negociosPendientes[0].id").value(negocio.toString()));
    }

    @Test
    @DisplayName("cuando el modulo de negocios responde, la persona ve el negocio como DUENO ya activo, y elegirlo sigue funcionando")
    void flujoCompleto_pedirResponderYElegir() throws Exception {
        var token = registrar(telefono());
        var negocio = pedirNegocio(token, "Fonda Doña Chela", "RESTAURANTE");

        elModuloDeNegociosResponde(negocio).andExpect(status().isNoContent());

        mvc.perform(conToken(get("/auth/yo"), token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.negocios", hasSize(1)))
                .andExpect(jsonPath("$.negocios[0].id").value(negocio.toString()))
                .andExpect(jsonPath("$.negocios[0].nombre").value("Fonda Doña Chela"))
                .andExpect(jsonPath("$.negocios[0].rol").value("DUENO"))
                .andExpect(jsonPath("$.negocioActivo").value(negocio.toString()))
                .andExpect(jsonPath("$.negociosPendientes").isEmpty());
        var cuerpo = mvc.perform(conToken(put("/auth/negocio-activo"), token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"negocioId\":\"" + negocio + "\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value(matchesPattern("[A-Za-z0-9_-]{43}")))
                .andExpect(jsonPath("$.negocioActivo").value(negocio.toString()))
                .andReturn().getResponse().getContentAsString();
        String tokenNuevo = JsonPath.read(cuerpo, "$.token");

        assertThat(tokenNuevo).isNotEqualTo(token);
        mvc.perform(conToken(get("/auth/yo"), token)).andExpect(status().isUnauthorized());
        mvc.perform(conToken(get("/auth/yo"), tokenNuevo)).andExpect(status().isOk())
                .andExpect(jsonPath("$.negocioActivo").value(negocio.toString()))
                .andExpect(jsonPath("$.rol").value("DUENO"))
                .andExpect(jsonPath("$.permisos", hasItem("identidad.miembro.invitar")))
                .andExpect(jsonPath("$.permisos", hasItem("clientes.cliente.anonimizar")));
    }

    @Test
    @DisplayName("un giro invalido responde 400 GIRO_INVALIDO, un nombre invalido 400 NOMBRE_INVALIDO y sin token 401")
    void pedirNegocio_errores() throws Exception {
        var token = registrar(telefono());

        mvc.perform(conToken(post("/registro/negocio"), token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Fonda\",\"giro\":\"FARMACIA\"}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("GIRO_INVALIDO"));
        mvc.perform(conToken(post("/registro/negocio"), token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"F\",\"giro\":\"MUSEO\"}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("NOMBRE_INVALIDO"));
        mvc.perform(post("/registro/negocio").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Fonda\",\"giro\":\"MUSEO\"}")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("AZ-05: un negocioId en el cuerpo de la solicitud del negocio no cambia el negocio pedido")
    void pedirNegocio_ignoraUnNegocioIdDelCuerpo() throws Exception {
        var token = registrar(telefono());
        var intruso = UUID.randomUUID();

        var cuerpo = mvc.perform(conToken(post("/registro/negocio"), token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Fonda\",\"giro\":\"MUSEO\",\"negocioId\":\"" + intruso + "\"}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(cuerpo, "$.negocioId")).isNotEqualTo(intruso.toString());
    }

    @Test
    @DisplayName("AS-06: elegir un negocio ajeno o inexistente responde el mismo 404 NEGOCIO_NO_DISPONIBLE")
    void negocioActivo_ajenoOInexistente_mismoError() throws Exception {
        var ana = registrar(telefono());
        var beto = registrar(telefono());
        var deBeto = pedirNegocio(beto, "Tienda de Beto", "MISCELANEA");
        elModuloDeNegociosResponde(deBeto).andExpect(status().isNoContent());

        for (var id : new UUID[] {deBeto, UUID.randomUUID()}) {
            mvc.perform(conToken(put("/auth/negocio-activo"), ana).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"negocioId\":\"" + id + "\"}")).andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.codigo").value("NEGOCIO_NO_DISPONIBLE"));
        }
        mvc.perform(conToken(put("/auth/negocio-activo"), ana).contentType(MediaType.APPLICATION_JSON)
                .content("{\"negocioId\":\"no-es-un-uuid\"}")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
        mvc.perform(conToken(get("/auth/yo"), ana)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET /roles y GET /permisos: el dueño ve los roles del giro y el catalogo de los modulos")
    void rolesYPermisos_delDueno() throws Exception {
        var token = registrar(telefono());
        var negocio = pedirNegocio(token, "Museo Talavera Viva", "MUSEO");
        elModuloDeNegociosResponde(negocio).andExpect(status().isNoContent());
        var activo = JsonPath.<String>read(mvc.perform(conToken(put("/auth/negocio-activo"), token)
                .contentType(MediaType.APPLICATION_JSON).content("{\"negocioId\":\"" + negocio + "\"}")).andReturn()
                .getResponse().getContentAsString(), "$.token");

        mvc.perform(conToken(get("/roles"), activo)).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].codigo").value(org.hamcrest.Matchers.contains("DUENO", "ADMINISTRADOR",
                        "GERENTE", "TAQUILLERO", "GUIA")))
                .andExpect(jsonPath("$[0].permisos", hasSize(63)));
        mvc.perform(conToken(get("/permisos"), activo)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(63)))
                .andExpect(jsonPath("$[?(@.codigo=='clientes.cliente.anonimizar')].sensible").value(hasItem(true)))
                .andExpect(jsonPath("$[?(@.codigo=='clientes.sugerir')].modulo").value(hasItem("clientes")));
    }

    @Test
    @DisplayName("GET /roles sin sesion responde 401 y sin negocio activo 404")
    void roles_sinSesionOSinNegocio() throws Exception {
        var sinNegocio = registrar(telefono());

        mvc.perform(get("/roles")).andExpect(status().isUnauthorized());
        mvc.perform(get("/permisos")).andExpect(status().isUnauthorized());
        mvc.perform(conToken(get("/roles"), sinNegocio)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.codigo").value("NEGOCIO_NO_DISPONIBLE"));
    }

    @Test
    @DisplayName("el simulador del modulo de negocios responde 404 si nadie pidio ese negocio")
    void simulador_negocioNoPedido_responde404() throws Exception {
        elModuloDeNegociosResponde(UUID.randomUUID()).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("SI-17: las respuestas de negocios y roles no se guardan en cache")
    void respuestas_noSeGuardanEnCache() throws Exception {
        var token = registrar(telefono());
        var respuestas = new org.springframework.mock.web.MockHttpServletResponse[] {
            mvc.perform(conToken(post("/registro/negocio"), token).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"nombre\":\"Fonda\",\"giro\":\"MUSEO\"}")).andReturn().getResponse(),
            mvc.perform(conToken(get("/roles"), token)).andReturn().getResponse(),
            mvc.perform(get("/permisos")).andReturn().getResponse(),
            mvc.perform(conToken(put("/auth/negocio-activo"), token).contentType(MediaType.APPLICATION_JSON)
                    .content("{\"negocioId\":\"" + UUID.randomUUID() + "\"}")).andReturn().getResponse()};

        for (var respuesta : respuestas) {
            assertThat(respuesta.getHeader("Cache-Control")).as("estado %s", respuesta.getStatus()).contains("no-store");
            assertThat(respuesta.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        }
    }

    @Test
    @DisplayName("el perfil de una cuenta sin negocios no tiene rol ni permisos")
    void yo_sinNegocios_sinRolNiPermisos() throws Exception {
        var token = registrar(telefono());

        mvc.perform(conToken(get("/auth/yo"), token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.rol").value(nullValue())).andExpect(jsonPath("$.permisos").isEmpty())
                .andExpect(jsonPath("$.negocios", not(hasItem("x"))));
    }
}
