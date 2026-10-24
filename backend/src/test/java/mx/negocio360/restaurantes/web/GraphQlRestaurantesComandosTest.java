package mx.negocio360.restaurantes.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.SesionAutenticada;
import mx.negocio360.identidad.dominio.RolesBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Los comandos de Restaurantes ({@code guardarReceta} y {@code darDeBajaPlatillo}) por HTTP, con la aplicacion completa y una
 * sesion real de Identidad (rol base DUENO): la puerta de {@code /graphql}, el despachador de comandos y el manejador. Las
 * pruebas de {@code ComandosDeRestaurantesTest} no usan Spring y simulan la autorizacion; esta cubre el cableado que ellas no
 * ven. Todo dato es de ejemplo.
 */
@SpringBootTest(properties = {"restaurantes.almacen=memoria", "inventario.almacen=memoria"})
@AutoConfigureMockMvc
class GraphQlRestaurantesComandosTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PREPARAR = "mutation($i: ComandoInput!) { prepararComando(input: $i) "
            + "{ token resumen expiraEn advertencias } }";
    private static final String CONFIRMAR = "mutation($t: String!) { confirmarComando(token: $t) "
            + "{ ok codigo recursoId datos { clave valor } } }";

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private IdentidadApi identidad;

    private String token(String rol) {
        var token = "T" + UUID.randomUUID().toString().replace("-", "") + "xxxxxxxxxx";
        when(identidad.autenticar(token)).thenReturn(Optional.of(new SesionAutenticada(UUID.randomUUID(), "Ana",
                NEGOCIO, rol, RolesBase.porCodigo(rol).orElseThrow().permisos(), Instant.parse("2026-10-16T16:30:00Z"))));
        return token;
    }

    private static final UUID NEGOCIO = UUID.randomUUID();

    private JsonNode graphql(String token, String consulta, Map<String, Object> variables) throws Exception {
        var cuerpo = JSON.writeValueAsString(Map.of("query", consulta, "variables", variables));
        var peticion = post("/graphql").contentType(MediaType.APPLICATION_JSON).content(cuerpo)
                .header("Authorization", "Bearer " + token).with(r -> {
                    r.setRemoteAddr("10.7." + (int) (Math.random() * 250 + 1) + "." + (int) (Math.random() * 250 + 1));
                    return r;
                });
        return JSON.readTree(mvc.perform(peticion).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString());
    }

    private JsonNode comando(String token, Map<String, Object> entrada) throws Exception {
        var preparada = graphql(token, PREPARAR, Map.of("i", entrada));
        assertThat(preparada.has("errors")).as("preparar: " + preparada).isFalse();
        var confirmada = graphql(token, CONFIRMAR, Map.of("t", preparada.at("/data/prepararComando/token").asText()));
        assertThat(confirmada.at("/data/confirmarComando/ok").asBoolean()).as("confirmar: " + confirmada).isTrue();
        return confirmada.at("/data/confirmarComando");
    }

    private String platillo(String token, String nombre) throws Exception {
        var r = graphql(token, "mutation($i: RestPlatilloInput!) { crearPlatillo(input: $i) { id } }",
                Map.of("i", Map.of("nombre", nombre, "precio", "66.00")));
        assertThat(r.has("errors")).as(r.toString()).isFalse();
        return r.at("/data/crearPlatillo/id").asText();
    }

    @Test
    @DisplayName("el dueño guarda una receta por prepararComando y confirmarComando, y el platillo pasa a tener receta")
    void duenoGuardaReceta() throws Exception {
        var dueno = token("DUENO");
        var platillo = platillo(dueno, "Tacos al pastor");
        var tortilla = comando(dueno, Map.of("crearArticulo", Map.of("nombre", "Tortilla", "unidadBase", "pieza")))
                .get("recursoId").asText();
        comando(dueno, Map.of("registrarEntradaInventario", Map.of("articuloId", tortilla, "cantidad", "100",
                "unidad", "pieza", "costoUnitario", "2.00")));

        comando(dueno, Map.of("guardarReceta", Map.of("platilloId", platillo, "rendimiento", 1, "insumos",
                List.of(Map.of("articuloId", tortilla, "cantidad", "3", "unidad", "pieza", "mermaPct", "0")))));

        var vista = graphql(dueno, "query($i: ID!) { platillo(id: $i) { tieneReceta } }", Map.of("i", platillo));
        assertThat(vista.at("/data/platillo/tieneReceta").asBoolean()).as(vista.toString()).isTrue();
    }

    @Test
    @DisplayName("el dueño da de baja un platillo por el despachador de comandos")
    void duenoDaDeBajaUnPlatillo() throws Exception {
        var dueno = token("DUENO");
        var platillo = platillo(dueno, "Tacos de canasta");

        comando(dueno, Map.of("darDeBajaPlatillo", Map.of("platilloId", platillo)));

        var vista = graphql(dueno, "query($i: ID!) { platillo(id: $i) { activo } }", Map.of("i", platillo));
        assertThat(vista.at("/data/platillo/activo").asBoolean()).as(vista.toString()).isFalse();
    }
}
