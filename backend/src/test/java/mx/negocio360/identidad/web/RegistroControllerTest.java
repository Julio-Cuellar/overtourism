package mx.negocio360.identidad.web;

import static mx.negocio360.identidad.web.FlujoHttp.correoDe;
import static mx.negocio360.identidad.web.FlujoHttp.formulario;
import static mx.negocio360.identidad.web.FlujoHttp.formularioDe;
import static mx.negocio360.identidad.web.FlujoHttp.ipNueva;
import static mx.negocio360.identidad.web.FlujoHttp.telefono;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * D-84 · API REST del registro en dos pasos: {@code POST /registro} guarda el formulario y manda un codigo al correo;
 * {@code POST /registro/verificacion} crea la cuenta; {@code POST /registro/reenvio} pide otro codigo. El codigo "llega" al
 * canal simulado y se lee de {@code GET /dev/canal/codigo}. Corre con la aplicacion completa: el pre-registro vive en el
 * Postgres de pruebas.
 */
@SpringBootTest
@AutoConfigureMockMvc
class RegistroControllerTest {

    @Autowired
    private MockMvc mvc;

    private FlujoHttp flujo;

    @BeforeEach
    void preparar() {
        flujo = new FlujoHttp(mvc);
    }

    // ---- paso 1: el formulario ----

    @Test
    @DisplayName("POST /registro con todos los campos responde 202 con el correo y hasta cuando vale el codigo, sin token ni cuenta")
    void formulario_completo_responde202() throws Exception {
        var tel = telefono();

        flujo.registrar(formularioDe(tel, null), ipNueva()).andExpect(status().isAccepted())
                .andExpect(jsonPath("$.correo").value(correoDe(tel)))
                .andExpect(jsonPath("$.venceEn").value(matchesPattern("\\d{4}-\\d{2}-\\d{2}T.*Z")))
                .andExpect(jsonPath("$.token").doesNotExist()).andExpect(jsonPath("$.cuenta").doesNotExist());
    }

    @Test
    @DisplayName("antes de verificar la cuenta no existe: ni el codigo del correo abre sesion con el celular")
    void formulario_noCreaLaCuenta() throws Exception {
        var tel = telefono();
        var ip = ipNueva();
        flujo.registrar(formularioDe(tel, null), ip).andExpect(status().isAccepted());

        flujo.iniciar(tel, flujo.codigoDe(correoDe(tel)), ip).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"));
    }

    @Test
    @DisplayName("cada campo obligatorio que falta tiene su error: nombre 422, apellidos 400, correo 400, celular 400, aviso 422")
    void formulario_camposFaltantes_cadaUnoTieneSuError() throws Exception {
        var tel = telefono();
        var ip = ipNueva();
        var correo = correoDe(tel);

        flujo.registrar(formulario(null, "López", correo, tel, "true"), ip).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("NOMBRE_REQUERIDO"));
        flujo.registrar(formulario("Ana", null, correo, tel, "true"), ip).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("APELLIDOS_INVALIDOS"));
        flujo.registrar(formulario("Ana", "López", "sin-arroba", tel, "true"), ip).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CORREO_INVALIDO"));
        flujo.registrar(formulario("Ana", "López", correo, "123", "true"), ip).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TELEFONO_INVALIDO"));
        flujo.registrar(formulario("Ana", "López", correo, null, "true"), ip).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("TELEFONO_INVALIDO"));
        flujo.registrar(formulario("Ana", "López", correo, tel, "false"), ip).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AVISO_REQUERIDO"));
        flujo.registrar("{\"nombre\":\"Ana\",\"apellidos\":\"López\",\"correo\":\"" + correo + "\",\"telefono\":\"" + tel
                + "\"}", ip).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.codigo").value("AVISO_REQUERIDO"));

        flujo.registrar(formularioDe(tel, null), ip).andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("un correo o un celular que ya tienen cuenta responden 409 con su error")
    void formulario_duplicados_responden409() throws Exception {
        var tel = telefono();
        var ip = ipNueva();
        flujo.registrarCuenta(tel, null, ip);
        var otro = telefono();

        flujo.registrar(formulario("Beto", "Ruiz", correoDe(tel).toUpperCase(), otro, "true"), ip)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.codigo").value("CORREO_NO_DISPONIBLE"));
        flujo.registrar(formulario("Ana", "López", correoDe(otro), tel, "true"), ip).andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("CUENTA_YA_EXISTE"));
    }

    @Test
    @DisplayName("el formulario tiene tope por correo: el cuarto en 15 minutos responde 429")
    void formulario_cuartoDelMismoCorreo_responde429() throws Exception {
        var tel = telefono();
        for (var i = 0; i < 3; i++) {
            flujo.registrar(formularioDe(tel, null), ipNueva()).andExpect(status().isAccepted());
        }

        flujo.registrar(formularioDe(tel, null), ipNueva()).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("TASA_EXCEDIDA"));
    }

    // ---- paso 2: la verificacion ----

    @Test
    @DisplayName("POST /registro/verificacion con el codigo del correo responde 201 con la cuenta y el token, y el correo queda verificado")
    void verificacion_codigoCorrecto_responde201() throws Exception {
        var tel = telefono();
        var ip = ipNueva();
        flujo.registrar(formularioDe(tel, null), ip).andExpect(status().isAccepted());

        flujo.verificar(correoDe(tel), flujo.codigoDe(correoDe(tel)), ip).andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(matchesPattern("[A-Za-z0-9_-]{43}")))
                .andExpect(jsonPath("$.cuenta.id").value(matchesPattern("[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.cuenta.nombre").value("Ana"))
                .andExpect(jsonPath("$.cuenta.apellidos").value("López Pérez"))
                .andExpect(jsonPath("$.cuenta.correo").value(correoDe(tel)))
                .andExpect(jsonPath("$.cuenta.telefono").value("+52" + tel))
                .andExpect(jsonPath("$.negocios").isEmpty()).andExpect(jsonPath("$.negociosPendientes").isEmpty());
    }

    @Test
    @DisplayName("el codigo es de un solo uso: la segunda verificacion responde 401 CODIGO_INVALIDO")
    void verificacion_codigoUsado_responde401() throws Exception {
        var tel = telefono();
        var ip = ipNueva();
        flujo.registrar(formularioDe(tel, null), ip).andExpect(status().isAccepted());
        var codigo = flujo.codigoDe(correoDe(tel));
        flujo.verificar(correoDe(tel), codigo, ip).andExpect(status().isCreated());

        flujo.verificar(correoDe(tel), codigo, ip).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO"));
    }

    @Test
    @DisplayName("un codigo incorrecto, sin pre-registro o agotado responde lo mismo: 401 CODIGO_INVALIDO con el mismo texto")
    void verificacion_fallos_respondenIgual() throws Exception {
        var tel = telefono();
        var ip = ipNueva();
        flujo.registrar(formularioDe(tel, null), ip).andExpect(status().isAccepted());
        var correcto = flujo.codigoDe(correoDe(tel));
        var incorrecto = "000000".equals(correcto) ? "000001" : "000000";

        var malo = flujo.verificar(correoDe(tel), incorrecto, ip).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("CODIGO_INVALIDO")).andReturn().getResponse().getContentAsString();
        for (var i = 0; i < 4; i++) {
            flujo.verificar(correoDe(tel), incorrecto, ip).andExpect(status().isUnauthorized());
        }
        var agotado = flujo.verificar(correoDe(tel), correcto, ip).andExpect(status().isUnauthorized()).andReturn()
                .getResponse().getContentAsString();
        var inexistente = flujo.verificar("nadie" + tel + "@example.com", correcto, ip).andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(agotado).isEqualTo(malo);
        assertThat(inexistente).isEqualTo(malo);
    }

    @Test
    @DisplayName("un correo mal escrito en la verificacion responde 400 CORREO_INVALIDO")
    void verificacion_correoInvalido_responde400() throws Exception {
        flujo.verificar("sin-arroba", "123456", ipNueva()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CORREO_INVALIDO"));
    }

    // ---- reenvio ----

    @Test
    @DisplayName("el reenvio responde 202 sin cuerpo, haya o no pre-registro, y el codigo nuevo sirve")
    void reenvio_respondeIgualYElCodigoNuevoSirve() throws Exception {
        var tel = telefono();
        var ip = ipNueva();
        flujo.registrar(formularioDe(tel, null), ip).andExpect(status().isAccepted());

        flujo.reenviar(correoDe(tel), ip).andExpect(status().isAccepted()).andExpect(content().string(""));
        flujo.reenviar("nadie" + tel + "@example.com", ip).andExpect(status().isAccepted())
                .andExpect(content().string(""));

        flujo.verificar(correoDe(tel), flujo.codigoDe(correoDe(tel)), ip).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("el reenvio con un correo invalido responde 400 y el tercero seguido responde 429")
    void reenvio_errores() throws Exception {
        flujo.reenviar("sin-arroba", ipNueva()).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("CORREO_INVALIDO"));
        var correo = "tope" + telefono() + "@example.com";
        for (var i = 0; i < 3; i++) {
            flujo.reenviar(correo, ipNueva()).andExpect(status().isAccepted());
        }

        flujo.reenviar(correo, ipNueva()).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.codigo").value("TASA_EXCEDIDA"));
    }

    // ---- acceso con correo e id (D-76, D-85) ----

    @Test
    @DisplayName("el codigo se puede pedir con el correo y con el id, llega al correo y con el se inicia sesion")
    void acceso_conCorreoYConId_abreLaSesion() throws Exception {
        var tel = telefono();
        var cuenta = flujo.registrarCuenta(tel, null, ipNueva());
        String id = JsonPath.read(cuenta, "$.cuenta.id");

        for (var identificador : new String[] {correoDe(tel), id}) {
            var otraIp = ipNueva();
            flujo.pedirCodigo(identificador, otraIp).andExpect(status().isAccepted());
            flujo.iniciar(identificador, flujo.codigoDe(correoDe(tel)), otraIp).andExpect(status().isOk())
                    .andExpect(jsonPath("$.cuenta.id").value(id));
        }
    }

    @Test
    @DisplayName("SI-02: pedir el codigo con un correo sin cuenta responde 202 igual que con uno que si tiene")
    void codigo_correoSinCuenta_respondeIgual() throws Exception {
        var a = flujo.pedirCodigo("nadie" + telefono() + "@example.com", ipNueva()).andReturn().getResponse();

        assertThat(a.getStatus()).isEqualTo(202);
        assertThat(a.getContentAsString()).isEmpty();
    }

    @Test
    @DisplayName("SI-17: ninguna respuesta del registro se guarda en cache")
    void registro_respuestas_noSeGuardanEnCache() throws Exception {
        var tel = telefono();
        var ip = ipNueva();
        var respuestas = new org.springframework.mock.web.MockHttpServletResponse[] {
            flujo.registrar(formularioDe(tel, null), ip).andReturn().getResponse(),
            flujo.reenviar(correoDe(tel), ip).andReturn().getResponse(),
            flujo.verificar(correoDe(tel), flujo.codigoDe(correoDe(tel)), ip).andReturn().getResponse(),
            flujo.verificar(correoDe(tel), "000000", ip).andReturn().getResponse()};

        for (var respuesta : respuestas) {
            assertThat(respuesta.getHeader("Cache-Control")).as("estado %s", respuesta.getStatus()).contains("no-store");
            assertThat(respuesta.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        }
    }

    // ---- D-79 y D-80: la persona y su negocio, y el negocio activo implicito ----

    @Test
    @DisplayName("D-79: con negocio, el formulario responde 202 y la verificacion 201 con la sesion y el negocio pendiente")
    void registro_conNegocio_responde201ConElPendiente() throws Exception {
        var tel = telefono();
        var ip = ipNueva();
        flujo.registrar(formularioDe(tel, "restaurante"), ip).andExpect(status().isAccepted());

        flujo.verificar(correoDe(tel), flujo.codigoDe(correoDe(tel)), ip).andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value(matchesPattern("[A-Za-z0-9_-]{43}")))
                .andExpect(jsonPath("$.negocios").isEmpty())
                .andExpect(jsonPath("$.negociosPendientes.length()").value(1))
                .andExpect(jsonPath("$.negociosPendientes[0].id").value(matchesPattern("[0-9a-f-]{36}")))
                .andExpect(jsonPath("$.negociosPendientes[0].nombre").value("Fonda Doña Chela"))
                .andExpect(jsonPath("$.negociosPendientes[0].giro").value("RESTAURANTE"))
                .andExpect(jsonPath("$.negociosPendientes[0].estado").value("PENDIENTE"));
    }

    @Test
    @DisplayName("D-79 y D-80: de pendiente a negocio activo sin elegir nada ni cambiar de token")
    void registro_conNegocio_flujoCompletoSinElegir() throws Exception {
        var cuerpo = flujo.registrarCuenta(telefono(), "MUSEO", ipNueva());
        String token = JsonPath.read(cuerpo, "$.token");
        String negocio = JsonPath.read(cuerpo, "$.negociosPendientes[0].id");

        mvc.perform(get("/auth/yo").header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.negociosPendientes.length()").value(1))
                .andExpect(jsonPath("$.negocioActivo").doesNotExist());
        mvc.perform(post("/dev/negocios/" + negocio + "/registrado")).andExpect(status().isNoContent());

        mvc.perform(get("/auth/yo").header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andExpect(jsonPath("$.negociosPendientes").isEmpty())
                .andExpect(jsonPath("$.negocios.length()").value(1))
                .andExpect(jsonPath("$.negocioActivo").value(negocio))
                .andExpect(jsonPath("$.rol").value("DUENO"));
        mvc.perform(get("/roles").header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("DUENO"));
    }

    @Test
    @DisplayName("D-79: un negocio mal pedido responde 400 en el formulario y no guarda nada: ni codigo en el canal")
    void registro_conNegocioInvalido_responde400SinGuardarNada() throws Exception {
        var tel = telefono();
        var ip = ipNueva();

        flujo.registrar(formularioDe(tel, "FARMACIA"), ip).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("GIRO_INVALIDO"));

        mvc.perform(get("/dev/canal/codigo").param("correo", correoDe(tel))).andExpect(status().isNotFound());
        flujo.registrar(formularioDe(tel, "MISCELANEA"), ip).andExpect(status().isAccepted());
    }

    @Test
    @DisplayName("sin negocio, la respuesta de la verificacion trae la lista de pendientes vacia")
    void registro_sinNegocio_pendientesVacios() throws Exception {
        var tel = telefono();
        var ip = ipNueva();
        flujo.registrar(formularioDe(tel, null), ip).andExpect(status().isAccepted());

        flujo.verificar(correoDe(tel), flujo.codigoDe(correoDe(tel)), ip).andExpect(status().isCreated())
                .andExpect(jsonPath("$.negociosPendientes").isArray()).andExpect(jsonPath("$.negociosPendientes").isEmpty());
    }

    @Test
    @DisplayName("el formulario ya no acepta codigo: un campo codigo en el cuerpo se ignora y no activa nada")
    void formulario_ignoraUnCodigoEnElCuerpo() throws Exception {
        var tel = telefono();

        mvc.perform(post("/registro").contentType(MediaType.APPLICATION_JSON)
                .content(formularioDe(tel, null).replace("{", "{\"codigo\":\"123456\",")).with(FlujoHttp.desde(ipNueva())))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.token").doesNotExist());
    }
}
