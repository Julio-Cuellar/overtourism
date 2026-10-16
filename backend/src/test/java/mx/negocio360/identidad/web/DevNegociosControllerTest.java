package mx.negocio360.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import mx.negocio360.identidad.aplicacion.EquipoServicio;
import mx.negocio360.identidad.infraestructura.EscuchaDeNegocios;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/**
 * El simulador del modulo de negocios (solo desarrollo) tiene que ser determinista: cuando responde 204 el negocio ya
 * esta aplicado. Spring Modulith activa {@code @Async} por si solo, asi que {@code EscuchaDeNegocios}, anotado con
 * {@code @ApplicationModuleListener}, corre en otro hilo: llamarlo desde el simulador respondia 204 antes de aplicar el
 * aviso, y el primer {@code GET /roles} despues del arranque daba 404 (se vio al correr la coleccion de Postman).
 */
@SpringBootTest
@AutoConfigureMockMvc
class DevNegociosControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("el simulador aplica el aviso con el servicio, en el mismo hilo, y no a traves del escucha asincrono")
    void simulador_noUsaElEscuchaAsincrono() {
        var parametros = java.util.Arrays.stream(DevNegociosController.class.getConstructors()[0].getParameterTypes()).toList();

        assertThat(parametros).contains(EquipoServicio.class).doesNotContain(EscuchaDeNegocios.class);
    }

    @Test
    @DisplayName("tras el 204 del simulador, el negocio ya esta aplicado: 30 veces seguidas con cuentas nuevas")
    void simulador_responde204ConElNegocioYaAplicado() throws Exception {
        var flujo = new FlujoHttp(mvc);
        for (var i = 0; i < 30; i++) {
            var registro = flujo.registrarCuenta(FlujoHttp.telefono(), "RESTAURANTE", FlujoHttp.ipNueva());
            String token = JsonPath.read(registro, "$.token");
            String negocio = JsonPath.read(registro, "$.negociosPendientes[0].id");

            mvc.perform(post("/dev/negocios/" + negocio + "/registrado")).andExpect(status().isNoContent());

            mvc.perform(get("/roles").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
            mvc.perform(get("/auth/yo").header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                    .andExpect(jsonPath("$.negocios", hasSize(1)));
        }
    }
}
