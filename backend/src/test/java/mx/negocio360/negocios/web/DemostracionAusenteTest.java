package mx.negocio360.negocios.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** Sin el perfil demo los comandos de la demo no existen, aunque el modo demostracion este encendido. */
@SpringBootTest(properties = "negocios.demo.activo=true")
@AutoConfigureMockMvc
class DemostracionAusenteTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("sin el perfil demo, /demo/sembrar y /demo/restablecer responden 404")
    void sinPerfilDemo_noExisten() throws Exception {
        mvc.perform(post("/demo/sembrar")).andExpect(status().isNotFound());
        mvc.perform(post("/demo/restablecer")).andExpect(status().isNotFound());
    }
}
