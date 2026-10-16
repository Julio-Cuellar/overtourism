package mx.negocio360.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import mx.negocio360.identidad.aplicacion.PlantillasIdentidad;
import mx.negocio360.identidad.dominio.CodigoError;
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
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * IDN-1 · API REST de acceso (D-72): codigo, registro, sesion, perfil, cierre y nombre. Corre con la aplicacion
 * completa. Cada prueba usa su propio telefono y su propia IP para no compartir topes. El registro en dos pasos tiene sus
 * propias pruebas en {@link RegistroControllerTest}.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

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

    private static RequestPostProcessor desde(String ip) {
        return FlujoHttp.desde(ip);
    }

    private ResultActions pedirCodigo(String identificador, String ip) throws Exception {
        return flujo.pedirCodigo(identificador, ip);
    }

    private static String correoDe(String telefono) {
        return FlujoHttp.correoDe(telefono);
    }

    /** El codigo que llego al correo de ese celular. */
    private String codigoDe(String telefono) throws Exception {
        return flujo.codigoDe(correoDe(telefono));
    }

    private ResultActions iniciar(String identificador, String codigo, String ip) throws Exception {
        return flujo.iniciar(identificador, codigo, ip);
    }

    /** Los dos pasos del registro con el nombre dado; devuelve el cuerpo de la sesion. */
    private String registrar(String telefono, String nombre, String ip) throws Exception {
        flujo.registrar(FlujoHttp.formulario(nombre, "Pérez Gómez", correoDe(telefono), telefono, "true"), ip)
                .andExpect(status().isAccepted());
        return flujo.verificar(correoDe(telefono), codigoDe(telefono), ip).andExpect(status().isCreated()).andReturn()
                .getResponse().getContentAsString();
    }

    /** Con nombre registra una cuenta nueva; sin nombre entra a una que ya existe. */
    private String tokenDe(String telefono, String nombre, String ip) throws Exception {
        if (nombre != null) {
            return JsonPath.read(registrar(telefono, nombre, ip), "$.token");
        }
        return flujo.tokenDeAcceso(telefono, correoDe(telefono), ip);
    }

    private static MockHttpServletRequestBuilder conToken(MockHttpServletRequestBuilder peticion, String token) {
        return peticion.header("Authorization", "Bearer " + token);
    }

    private record Respuesta(int estado, String cuerpo, String autenticacion) {
    }

    private Respuesta respuestaDe(MockHttpServletRequestBuilder peticion) throws Exception {
        var r = mvc.perform(peticion).andReturn().getResponse();
        return new Respuesta(r.getStatus(), r.getContentAsString(), r.getHeader("WWW-Authenticate"));
    }

    @Test
    @DisplayName("POST /auth/codigo con un telefono valido responde 202 sin cuerpo")
    void codigo_telefonoValido_responde202SinCuerpo() throws Exception {
        pedirCodigo(telefono(), ipNueva()).andExpect(status().isAccepted()).andExpect(content().string(""));
    }

    @Test
    @DisplayName("POST /auth/codigo con un telefono invalido responde 400 con el texto fijo")
    void codigo_telefonoInvalido_responde400ConTextoFijo() throws Exception {
        pedirCodigo("123", ipNueva()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TELEFONO_INVALIDO"))
                .andExpect(jsonPath("$.mensaje").value(PlantillasIdentidad.texto(CodigoError.TELEFONO_INVALIDO)));
    }

    @Test
    @DisplayName("AS-03: la cuarta solicitud del mismo telefono responde 429 TASA_EXCEDIDA")
    void codigo_cuartaSolicitud_responde429() throws Exception {
        var tel = telefono();
        var ip = ipNueva();
        for (var i = 0; i < 3; i++) {
            pedirCodigo(tel, ip).andExpect(status().isAccepted());
        }

        pedirCodigo(tel, ip).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("TASA_EXCEDIDA"));
    }

    @Test
    @DisplayName("flujo completo: formulario, verificacion, perfil, cambio de nombre, cierre y token ya invalido")
    void flujoCompleto_registroPerfilNombreYCierre() throws Exception {
        var tel = telefono();
        var ip = ipNueva();
        flujo.registrar(FlujoHttp.formulario("Ana López", "Pérez Gómez", correoDe(tel), tel, "true"), ip)
                .andExpect(status().isAccepted());

        var cuerpo = flujo.verificar(correoDe(tel), codigoDe(tel), ip).andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(matchesPattern("[A-Za-z0-9_-]{43}")))
                .andExpect(jsonPath("$.expiraEn").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z")))
                .andExpect(jsonPath("$.cuenta.nombre").value("Ana López"))
                .andExpect(jsonPath("$.cuenta.apellidos").value("Pérez Gómez"))
                .andExpect(jsonPath("$.cuenta.correo").value(correoDe(tel)))
                .andExpect(jsonPath("$.cuenta.telefono").value("+52" + tel))
                .andExpect(jsonPath("$.negocios").isEmpty())
                .andExpect(jsonPath("$.negocioActivo").value(nullValue()))
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(cuerpo, "$.token");

        mvc.perform(conToken(get("/auth/yo"), token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.cuenta.nombre").value("Ana López"))
                .andExpect(jsonPath("$.negocioActivo").value(nullValue()))
                .andExpect(jsonPath("$.negocios").isEmpty());
        mvc.perform(conToken(patch("/cuenta"), token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Ana María López\"}")).andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana María López"));
        mvc.perform(conToken(get("/auth/yo"), token)).andExpect(jsonPath("$.cuenta.nombre").value("Ana María López"));
        mvc.perform(conToken(delete("/auth/sesion"), token)).andExpect(status().isNoContent());
        mvc.perform(conToken(get("/auth/yo"), token)).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("NO_AUTENTICADO"));
    }

    @Test
    @DisplayName("un celular sin cuenta responde 401 CODIGO_INVALIDO, igual que con un codigo incorrecto, y no se le manda nada")
    void sesion_celularSinCuenta_responde401() throws Exception {
        var tel = telefono();
        var ip = ipNueva();
        pedirCodigo(tel, ip).andExpect(status().isAccepted());

        iniciar(tel, "123456", ip).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"));

        mvc.perform(get("/dev/canal/codigo").param("correo", correoDe(tel))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("un codigo incorrecto responde 401 CODIGO_INVALIDO")
    void sesion_codigoIncorrecto_responde401() throws Exception {
        var tel = telefono();
        var ip = ipNueva();
        tokenDe(tel, "Ana López", ip);
        pedirCodigo(tel, ip).andExpect(status().isAccepted());
        var correcto = codigoDe(tel);
        var incorrecto = "000000".equals(correcto) ? "000001" : "000000";

        iniciar(tel, incorrecto, ip).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"))
                .andExpect(jsonPath("$.mensaje").value(PlantillasIdentidad.texto(CodigoError.CODIGO_INVALIDO)));
    }

    @Test
    @DisplayName("AS-02: tras cinco fallos el codigo responde 401 CODIGO_INVALIDO, igual que antes, aunque ahora sea el correcto")
    void sesion_cincoFallos_agotanElCodigo() throws Exception {
        var tel = telefono();
        var ip = ipNueva();
        tokenDe(tel, "Ana López", ip);
        pedirCodigo(tel, ip).andExpect(status().isAccepted());
        var correcto = codigoDe(tel);
        var incorrecto = "000000".equals(correcto) ? "000001" : "000000";
        for (var i = 0; i < 5; i++) {
            iniciar(tel, incorrecto, ip).andExpect(status().isUnauthorized());
        }

        iniciar(tel, correcto, ip).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"));
    }

    @Test
    @DisplayName("AZ-05: un negocioId en el cuerpo se ignora; el negocio activo sigue siendo ninguno")
    void sesion_cuerpoConNegocioId_seIgnora() throws Exception {
        var tel = telefono();
        var ip = ipNueva();

        mvc.perform(post("/registro").contentType(MediaType.APPLICATION_JSON)
                .content(FlujoHttp.formularioDe(tel, null).replace("{", "{\"negocioId\":\"0192f39f-1111-7000-8000-00000000a001\","))
                .with(desde(ip))).andExpect(status().isAccepted());

        flujo.verificar(correoDe(tel), codigoDe(tel), ip).andExpect(status().isCreated())
                .andExpect(jsonPath("$.negocioActivo").value(nullValue()));
    }

    @Test
    @DisplayName("AC-01: el mismo telefono escrito de dos formas es la misma cuenta")
    void sesion_dosFormatosDelMismoTelefono_esLaMismaCuenta() throws Exception {
        var tel = telefono();
        var ip = ipNueva();
        var primero = registrar(tel, "Ana López", ip);
        var otraForma = "+52 " + tel.substring(0, 3) + " " + tel.substring(3, 6) + " " + tel.substring(6);
        pedirCodigo(otraForma, ip).andExpect(status().isAccepted());

        var segundo = iniciar(otraForma, codigoDe(tel), ip).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();

        assertThat((String) JsonPath.read(segundo, "$.cuenta.id")).isEqualTo(JsonPath.read(primero, "$.cuenta.id"));
    }

    @Test
    @DisplayName("AS-08: sin token, con un esquema distinto, inventado o ya cerrado, el 401 es identico")
    void yo_tokenInvalido_losCuatroCasosRespondenIgual() throws Exception {
        var cerrado = tokenDe(telefono(), "Ana López", ipNueva());
        mvc.perform(conToken(delete("/auth/sesion"), cerrado)).andExpect(status().isNoContent());

        var sinToken = respuestaDe(get("/auth/yo"));
        var esquemaDistinto = respuestaDe(get("/auth/yo").header("Authorization", "Basic YWRtaW46YWRtaW4="));
        var inventado = respuestaDe(conToken(get("/auth/yo"), "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
        var revocado = respuestaDe(conToken(get("/auth/yo"), cerrado));

        assertThat(sinToken.estado()).isEqualTo(401);
        assertThat(sinToken.cuerpo()).contains("NO_AUTENTICADO");
        assertThat(sinToken.autenticacion()).isEqualTo("Bearer");
        assertThat(esquemaDistinto).isEqualTo(sinToken);
        assertThat(inventado).isEqualTo(sinToken);
        assertThat(revocado).isEqualTo(sinToken);
    }

    @Test
    @DisplayName("el esquema Bearer no distingue mayusculas, como pide el estandar")
    void yo_esquemaEnMinusculas_autentica() throws Exception {
        var token = tokenDe(telefono(), "Ana López", ipNueva());

        mvc.perform(get("/auth/yo").header("Authorization", "bearer " + token)).andExpect(status().isOk());
    }

    @Test
    @DisplayName("DELETE /auth/sesiones cierra todas las sesiones de la cuenta")
    void cerrarSesiones_204YTodasLasSesionesCaen() throws Exception {
        var tel = telefono();
        var ip = ipNueva();
        var primera = tokenDe(tel, "Ana López", ip);
        var segunda = tokenDe(tel, null, ip);

        mvc.perform(conToken(delete("/auth/sesiones"), primera)).andExpect(status().isNoContent());

        mvc.perform(conToken(get("/auth/yo"), primera)).andExpect(status().isUnauthorized());
        mvc.perform(conToken(get("/auth/yo"), segunda)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("PATCH /cuenta con un nombre invalido responde 400 y sin token responde 401")
    void cuenta_nombreInvalidoOSinToken_rechaza() throws Exception {
        var token = tokenDe(telefono(), "Ana López", ipNueva());

        mvc.perform(conToken(patch("/cuenta"), token).contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"A\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("NOMBRE_INVALIDO"));
        mvc.perform(patch("/cuenta").contentType(MediaType.APPLICATION_JSON).content("{\"nombre\":\"Ana Nueva\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("un formulario en lugar de JSON responde 415 con el error de solicitud")
    void contentType_formulario_responde415() throws Exception {
        mvc.perform(post("/auth/codigo").contentType(MediaType.APPLICATION_FORM_URLENCODED).content("identificador=2225550000"))
                .andExpect(status().isUnsupportedMediaType()).andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    @DisplayName("un JSON mal formado responde 400 y no repite lo que se envio")
    void cuerpoMalFormado_responde400SinEcoDelCuerpo() throws Exception {
        mvc.perform(post("/auth/sesion").contentType(MediaType.APPLICATION_JSON).content("{\"identificador\": \"222 11"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"))
                .andExpect(content().string(not(containsString("222"))));
    }

    @Test
    @DisplayName("SI-14: ningun error muestra trazas, clases ni nombres internos")
    void errores_noRevelanTrazasNiNombresInternos() throws Exception {
        var ip = ipNueva();
        var respuestas = new String[] {
            pedirCodigo("123", ip).andReturn().getResponse().getContentAsString(),
            mvc.perform(post("/auth/sesion").contentType(MediaType.APPLICATION_JSON).content("{"))
                    .andReturn().getResponse().getContentAsString(),
            mvc.perform(post("/auth/codigo").contentType(MediaType.TEXT_PLAIN).content("x")).andReturn().getResponse()
                    .getContentAsString(),
            mvc.perform(get("/auth/yo")).andReturn().getResponse().getContentAsString()};

        for (var cuerpo : respuestas) {
            assertThat(cuerpo).isNotBlank().doesNotContain("Exception").doesNotContain("mx.negocio360")
                    .doesNotContain("\tat ").doesNotContain("org.springframework");
        }
    }

    @Test
    @DisplayName("el codigo de desarrollo no existe para un correo al que nunca se mando uno")
    void dev_correoSinCodigo_responde404() throws Exception {
        mvc.perform(get("/dev/canal/codigo").param("correo", correoDe(telefono()))).andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("el codigo de desarrollo con un correo mal escrito responde 400 CORREO_INVALIDO")
    void dev_correoInvalido_responde400() throws Exception {
        mvc.perform(get("/dev/canal/codigo").param("correo", "sin-arroba")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CORREO_INVALIDO"));
    }

    @Test
    @DisplayName("el codigo de desarrollo sin correo responde 400 SOLICITUD_INVALIDA, no 500")
    void dev_sinCorreo_responde400() throws Exception {
        mvc.perform(get("/dev/canal/codigo")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("SOLICITUD_INVALIDA"));
    }

    @Test
    @DisplayName("SI-02: pedir un codigo para un celular con cuenta y para uno sin cuenta da la misma respuesta")
    void codigo_conYSinCuenta_dalaMismaRespuesta() throws Exception {
        var conCuenta = telefono();
        tokenDe(conCuenta, "Ana López", ipNueva());
        var sinCuenta = telefono();

        var a = mvc.perform(post("/auth/codigo").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identificador\":\"" + conCuenta + "\"}").with(desde(ipNueva()))).andReturn().getResponse();
        var b = mvc.perform(post("/auth/codigo").contentType(MediaType.APPLICATION_JSON)
                .content("{\"identificador\":\"" + sinCuenta + "\"}").with(desde(ipNueva()))).andReturn().getResponse();

        assertThat(a.getStatus()).isEqualTo(b.getStatus()).isEqualTo(202);
        assertThat(a.getContentAsString()).isEqualTo(b.getContentAsString()).isEmpty();
    }

    @Test
    @DisplayName("SI-17: ninguna respuesta de acceso se guarda en cache ni se interpreta como otro tipo de contenido")
    void respuestas_llevanCabecerasQueEvitanElCache() throws Exception {
        var tel = telefono();
        var ip = ipNueva();
        var respuestas = new org.springframework.mock.web.MockHttpServletResponse[] {
            pedirCodigo(tel, ip).andReturn().getResponse(),
            flujo.registrar(FlujoHttp.formularioDe(tel, null), ip).andReturn().getResponse(),
            flujo.verificar(correoDe(tel), codigoDe(tel), ip).andReturn().getResponse(),
            mvc.perform(get("/auth/yo")).andReturn().getResponse(),
            mvc.perform(get("/dev/canal/codigo").param("correo", correoDe(tel))).andReturn().getResponse(),
            pedirCodigo("123", ip).andReturn().getResponse()};

        for (var respuesta : respuestas) {
            assertThat(respuesta.getHeader("Cache-Control")).as("estado %s", respuesta.getStatus()).contains("no-store");
            assertThat(respuesta.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        }
    }

    @Test
    @DisplayName("SI-16: un nombre o un telefono de miles de caracteres se rechaza al leer el cuerpo, con 400 y sin repetirlo")
    void cuerpo_cadenasEnormes_seRechazanConSolicitudInvalida() throws Exception {
        var enorme = "x".repeat(11_000);

        var enNombre = mvc.perform(post("/registro").contentType(MediaType.APPLICATION_JSON)
                .content("{\"telefono\":\"" + telefono() + "\",\"nombre\":\"" + enorme + "\"}")
                .with(desde(ipNueva()))).andReturn().getResponse();
        var enTelefono = pedirCodigo(enorme, ipNueva()).andReturn().getResponse();

        for (var respuesta : new org.springframework.mock.web.MockHttpServletResponse[] {enNombre, enTelefono}) {
            assertThat(respuesta.getStatus()).isEqualTo(400);
            assertThat(respuesta.getContentAsString()).contains("SOLICITUD_INVALIDA").doesNotContain("xxxxxxxxxx");
        }
    }
}
