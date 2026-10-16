package mx.negocio360.negocios.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** DM-02 y DM-04 con el perfil demo: siembra al arrancar y los dos comandos del equipo para el stand. */
@SpringBootTest(properties = "negocios.demo.activo=true")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class DemostracionControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("con el perfil demo la aplicacion ya sembro los doce negocios al arrancar y sembrar de nuevo no crea nada")
    void sembrar_esIdempotente_alArrancar() throws Exception {
        mvc.perform(post("/demo/sembrar")).andExpect(status().isOk()).andExpect(jsonPath("$.negocios.length()").value(12))
                .andExpect(jsonPath("$.negociosCreados").value(0)).andExpect(jsonPath("$.resenasSembradas").value(0));
    }

    @Test
    @DisplayName("restablecer deja los doce negocios con sus reseñas sembradas")
    void restablecer_respondeConLosConteos() throws Exception {
        mvc.perform(post("/demo/restablecer")).andExpect(status().isOk())
                .andExpect(jsonPath("$.negocios").value(org.hamcrest.Matchers.greaterThanOrEqualTo(12)))
                .andExpect(jsonPath("$.resenasRestauradas").value(org.hamcrest.Matchers.greaterThanOrEqualTo(24)));
    }
}
