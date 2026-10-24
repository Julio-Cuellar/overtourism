package mx.negocio360.restaurantes.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
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
 * R4 de punta a punta por HTTP, con la aplicacion completa y sesiones reales de Identidad (roles base): el salon, las
 * mesas, el servicio y el QR de mesa pasan por la puerta de {@code /graphql} y exigen su permiso. Todo dato es de ejemplo.
 */
@SpringBootTest(properties = {"restaurantes.almacen=memoria", "inventario.almacen=memoria"})
@AutoConfigureMockMvc
class GraphQlSalonTest {

    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private IdentidadApi identidad;

    private String token(UUID negocio, String rol) {
        var token = "T" + UUID.randomUUID().toString().replace("-", "") + "xxxxxxxxxx";
        when(identidad.autenticar(token)).thenReturn(Optional.of(new SesionAutenticada(UUID.randomUUID(), "Ana", negocio,
                rol, RolesBase.porCodigo(rol).orElseThrow().permisos(), Instant.parse("2026-10-16T16:30:00Z"))));
        return token;
    }

    private JsonNode graphql(String token, String consulta, Map<String, Object> variables) throws Exception {
        var cuerpo = JSON.writeValueAsString(Map.of("query", consulta, "variables", variables));
        var peticion = post("/graphql").contentType(MediaType.APPLICATION_JSON).content(cuerpo)
                .header("Authorization", "Bearer " + token).with(r -> {
                    r.setRemoteAddr("10.8." + (int) (Math.random() * 250 + 1) + "." + (int) (Math.random() * 250 + 1));
                    return r;
                });
        return JSON.readTree(mvc.perform(peticion).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString());
    }

    private static String codigo(JsonNode respuesta) {
        return respuesta.at("/errors/0/extensions/codigo").asText();
    }

    private String zona(String token, String nombre) throws Exception {
        var r = graphql(token, "mutation($n: String!) { crearZona(nombre: $n) { id } }", Map.of("n", nombre));
        assertThat(r.has("errors")).as(r.toString()).isFalse();
        return r.at("/data/crearZona/id").asText();
    }

    private String mesa(String token, String zona, String etiqueta) throws Exception {
        var r = graphql(token, "mutation($i: RestMesaInput!) { crearMesa(input: $i) { id } }",
                Map.of("i", Map.of("zonaId", zona, "etiqueta", etiqueta, "capacidad", 4)));
        assertThat(r.has("errors")).as(r.toString()).isFalse();
        return r.at("/data/crearMesa/id").asText();
    }

    @Test
    @DisplayName("el dueño arma el salon, abre el servicio y ve la disponibilidad que se publica para el turista")
    void duenoArmaElSalon() throws Exception {
        var dueno = token(UUID.randomUUID(), "DUENO");
        var zona = zona(dueno, "Terraza");
        mesa(dueno, zona, "Mesa 1");
        mesa(dueno, zona, "Mesa 2");
        var abierto = graphql(dueno, "mutation { abrirServicio { libres total abierto } }", Map.of());
        assertThat(abierto.has("errors")).as(abierto.toString()).isFalse();

        var salon = graphql(dueno, "{ salon { servicioAbierto qrHabilitado modo disponibilidad { libres total abierto } "
                + "zonas { nombre } mesas { etiqueta capacidad estado qrPausado } } }", Map.of());

        assertThat(salon.has("errors")).as(salon.toString()).isFalse();
        assertThat(salon.at("/data/salon/servicioAbierto").asBoolean()).isTrue();
        assertThat(salon.at("/data/salon/modo").asText()).isEqualTo("AMBOS");
        assertThat(salon.at("/data/salon/disponibilidad/libres").asInt()).isEqualTo(2);
        assertThat(salon.at("/data/salon/disponibilidad/total").asInt()).isEqualTo(2);
        assertThat(salon.at("/data/salon/zonas/0/nombre").asText()).isEqualTo("Terraza");
        assertThat(salon.at("/data/salon/mesas/0/etiqueta").asText()).isEqualTo("Mesa 1");
        assertThat(salon.at("/data/salon/mesas/0/estado").asText()).isEqualTo("LIBRE");
    }

    @Test
    @DisplayName("bloquear una mesa la marca BLOQUEADA y baja las mesas libres")
    void bloquear() throws Exception {
        var dueno = token(UUID.randomUUID(), "DUENO");
        var mesa = mesa(dueno, zona(dueno, "Salon"), "Mesa 1");

        var r = graphql(dueno, "mutation($m: ID!) { bloquearMesa(id: $m, bloquear: true) { estado bloqueada } }",
                Map.of("m", mesa));

        assertThat(r.at("/data/bloquearMesa/estado").asText()).isEqualTo("BLOQUEADA");
        var salon = graphql(dueno, "{ salon { disponibilidad { libres total } } }", Map.of());
        assertThat(salon.at("/data/salon/disponibilidad/libres").asInt()).isZero();
        assertThat(salon.at("/data/salon/disponibilidad/total").asInt()).isZero();
    }

    @Test
    @DisplayName("el enlace del QR de una mesa sale del servidor y regenerarlo da otro")
    void qr() throws Exception {
        var dueno = token(UUID.randomUUID(), "DUENO");
        var mesa = mesa(dueno, zona(dueno, "Salon"), "Mesa 1");

        var primero = graphql(dueno, "query($m: ID!) { qrDeMesa(mesaId: $m) }", Map.of("m", mesa));
        var segundo = graphql(dueno, "mutation($m: ID!) { regenerarQrDeMesa(mesaId: $m) }", Map.of("m", mesa));

        var url = primero.at("/data/qrDeMesa").asText();
        assertThat(url).contains("/m/").doesNotContain(mesa);
        assertThat(segundo.at("/data/regenerarQrDeMesa").asText()).isNotEqualTo(url).contains("/m/");
    }

    @Test
    @DisplayName("el mesero ve el salon pero no lo configura ni opera el servicio")
    void meseroSoloVe() throws Exception {
        var negocio = UUID.randomUUID();
        var dueno = token(negocio, "DUENO");
        var zona = zona(dueno, "Salon");
        var mesero = token(negocio, "MESERO");

        assertThat(graphql(mesero, "{ salon { servicioAbierto } }", Map.of()).has("errors")).isFalse();
        var crear = graphql(mesero, "mutation($i: RestMesaInput!) { crearMesa(input: $i) { id } }",
                Map.of("i", Map.of("zonaId", zona, "etiqueta", "Mesa 9", "capacidad", 2)));
        var abrir = graphql(mesero, "mutation { abrirServicio { abierto } }", Map.of());

        assertThat(codigo(crear)).isEqualTo("PERMISO_DENEGADO");
        assertThat(codigo(abrir)).isEqualTo("PERMISO_DENEGADO");
    }

    @Test
    @DisplayName("un negocio no ve ni toca las mesas de otro")
    void aislado() throws Exception {
        var dueno = token(UUID.randomUUID(), "DUENO");
        var mesa = mesa(dueno, zona(dueno, "Salon"), "Mesa 1");
        var otro = token(UUID.randomUUID(), "DUENO");

        var salon = graphql(otro, "{ salon { mesas { id } } }", Map.of());
        var bloquear = graphql(otro, "mutation($m: ID!) { bloquearMesa(id: $m, bloquear: true) { id } }", Map.of("m", mesa));

        assertThat(salon.at("/data/salon/mesas")).isEmpty();
        assertThat(codigo(bloquear)).isEqualTo("MESA_NO_ENCONTRADA");
    }

    @Test
    @DisplayName("una capacidad fuera de rango y una etiqueta repetida salen con su codigo")
    void errores() throws Exception {
        var dueno = token(UUID.randomUUID(), "DUENO");
        var zona = zona(dueno, "Salon");
        mesa(dueno, zona, "Mesa 1");

        var capacidad = graphql(dueno, "mutation($i: RestMesaInput!) { crearMesa(input: $i) { id } }",
                Map.of("i", Map.of("zonaId", zona, "etiqueta", "Mesa 2", "capacidad", 99)));
        var repetida = graphql(dueno, "mutation($i: RestMesaInput!) { crearMesa(input: $i) { id } }",
                Map.of("i", Map.of("zonaId", zona, "etiqueta", "mesa 1", "capacidad", 4)));

        assertThat(codigo(capacidad)).isEqualTo("CAPACIDAD_INVALIDA");
        assertThat(codigo(repetida)).isEqualTo("MESA_DUPLICADA");
    }
}
