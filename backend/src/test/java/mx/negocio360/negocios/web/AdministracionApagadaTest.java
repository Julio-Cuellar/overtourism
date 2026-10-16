package mx.negocio360.negocios.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** SN-15: sin un token de servicio configurado, las rutas /admin no existen (apagadas por omision, tambien en produccion). */
@SpringBootTest
@AutoConfigureMockMvc
class AdministracionApagadaTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("sin token de servicio configurado, /admin responde 404 aunque se mande un token")
    void sinConfiguracion_noExiste() throws Exception {
        mvc.perform(post("/admin/negocios/{n}/resenas/{r}/ocultar", UUID.randomUUID(), UUID.randomUUID())
                .header("X-Servicio-Admin", "cualquiera").contentType(MediaType.APPLICATION_JSON)
                .content("{\"motivo\":\"X\"}")).andExpect(status().isNotFound());
    }
}
