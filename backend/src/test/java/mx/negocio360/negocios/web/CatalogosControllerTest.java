package mx.negocio360.negocios.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

/** R1: {@code GET /catalogos/categorias} es publico, sin datos de nadie, con ETag y cache del celular. */
@SpringBootTest
@AutoConfigureMockMvc
class CatalogosControllerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    @DisplayName("responde sin sesion con los giros, las subcategorias y los atributos activos")
    void responde_sinSesion_conLosCatalogos() throws Exception {
        mvc.perform(get("/catalogos/categorias")).andExpect(status().isOk())
                .andExpect(jsonPath("$.giros.length()").value(3))
                .andExpect(jsonPath("$.giros[?(@.codigo=='RESTAURANTE')].nombre").value("Restaurante"))
                .andExpect(jsonPath("$.subcategorias[?(@.codigo=='TALAVERA')].giro").value("MISCELANEA"))
                .andExpect(jsonPath("$.subcategorias[?(@.giro=='MISCELANEA')].length()").isNotEmpty())
                .andExpect(jsonPath("$.atributos[?(@.tipo=='DIETA')].codigo").value(org.hamcrest.Matchers.hasItem("VEGANA")))
                .andExpect(jsonPath("$.atributos[?(@.tipo=='ACCESIBILIDAD')].codigo")
                        .value(org.hamcrest.Matchers.hasItem("PERSONAL_LSM")));
    }

    @Test
    @DisplayName("lleva ETag y Cache-Control publico, y con If-None-Match responde 304 sin cuerpo")
    void llevaEtag_yResponde304() throws Exception {
        var primera = mvc.perform(get("/catalogos/categorias")).andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("public")))
                .andExpect(header().exists("ETag")).andReturn();
        var etag = primera.getResponse().getHeader("ETag");

        var segunda = mvc.perform(get("/catalogos/categorias").header("If-None-Match", etag))
                .andExpect(status().isNotModified()).andReturn();

        assertThat(segunda.getResponse().getContentAsString()).isEmpty();
    }

    @Test
    @DisplayName("el catalogo no trae datos de ningun negocio ni de ninguna persona")
    void sinDatosDeNadie() throws Exception {
        var cuerpo = mvc.perform(get("/catalogos/categorias")).andReturn().getResponse().getContentAsString();

        assertThat(cuerpo).doesNotContain("negocio_id").doesNotContain("cuenta").doesNotContain("telefono")
                .doesNotContain("correo");
    }
}
