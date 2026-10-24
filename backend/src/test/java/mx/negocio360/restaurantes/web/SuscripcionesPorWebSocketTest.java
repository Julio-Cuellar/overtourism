package mx.negocio360.restaurantes.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.SesionAutenticada;
import mx.negocio360.identidad.dominio.RolesBase;
import mx.negocio360.restaurantes.aplicacion.RepositorioRestaurantes;
import mx.negocio360.restaurantes.dominio.Restaurante;
import mx.negocio360.restaurantes.infraestructura.AvisosEnVivo;
import mx.negocio360.seguridad.ClienteWebSocket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 8.4 contra un servidor real: la cocina y el salon reciben por WebSocket lo que pasa cuando un comensal pide por QR, sin que
 * ningun mesero haga nada (QR-06, QR-07), y un puesto sin el permiso de ver no llega a abrir la suscripcion.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"restaurantes.almacen=memoria", "inventario.almacen=memoria",
                "spring.graphql.websocket.connection-init-timeout=2s"})
@AutoConfigureMockMvc
class SuscripcionesPorWebSocketTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Duration ESPERA = Duration.ofSeconds(5);
    private static final String DISPOSITIVO = "5b0c1f0e-6a11-4d7e-9a3c-0d2f4e8a7b11";

    @LocalServerPort
    private int puerto;
    @Autowired
    private MockMvc mvc;
    @Autowired
    private RepositorioRestaurantes restaurantes;
    @Autowired
    private AvisosEnVivo avisos;
    @MockitoBean
    private IdentidadApi identidad;

    private String token(UUID negocio, String rol) {
        var token = "T" + UUID.randomUUID().toString().replace("-", "") + "xxxxxxxxxx";
        when(identidad.autenticar(token)).thenReturn(Optional.of(new SesionAutenticada(UUID.randomUUID(), "Ana", negocio,
                rol, RolesBase.porCodigo(rol).orElseThrow().permisos(), Instant.parse("2026-10-16T16:30:00Z"))));
        return token;
    }

    private JsonNode graphql(String token, String consulta, Map<String, Object> variables, String llave) throws Exception {
        var cuerpo = JSON.writeValueAsString(Map.of("query", consulta, "variables", variables));
        var peticion = post("/graphql").contentType(MediaType.APPLICATION_JSON).content(cuerpo).with(r -> {
            r.setRemoteAddr("10.7." + (int) (Math.random() * 250 + 1) + "." + (int) (Math.random() * 250 + 1));
            return r;
        });
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        if (llave != null) {
            peticion.header("Idempotency-Key", llave);
        }
        var respuesta = JSON.readTree(mvc.perform(peticion).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString());
        assertThat(respuesta.has("errors")).as(respuesta.toString()).isFalse();
        return respuesta;
    }

    private ClienteWebSocket conectado(String token) throws Exception {
        var cliente = ClienteWebSocket.conectar(puerto, null);
        cliente.iniciar(token);
        assertThat(cliente.esperar("connection_ack", ESPERA)).isNotNull();
        return cliente;
    }

    private void esperarSuscritos(int cuantos) throws Exception {
        var limite = System.nanoTime() + ESPERA.toNanos();
        while (avisos.suscritos() < cuantos && System.nanoTime() < limite) {
            Thread.sleep(25);
        }
        assertThat(avisos.suscritos()).isGreaterThanOrEqualTo(cuantos);
    }

    private record Armado(UUID negocio, String dueno, String qr, String cemita) {
    }

    private Armado armar() throws Exception {
        var negocio = UUID.randomUUID();
        restaurantes.guardar(Restaurante.nuevo(negocio, Instant.now()).conPublicado(true, Instant.now()));
        var dueno = token(negocio, "DUENO");
        var zona = graphql(dueno, "mutation { crearZona(nombre: \"Salon\") { id } }", Map.of(), null)
                .at("/data/crearZona/id").asText();
        var mesa = graphql(dueno, "mutation($i: RestMesaInput!) { crearMesa(input: $i) { id } }",
                Map.of("i", Map.of("zonaId", zona, "etiqueta", "Mesa 1", "capacidad", 4)), null).at("/data/crearMesa/id")
                .asText();
        graphql(dueno, "mutation { abrirServicio { abierto } }", Map.of(), null);
        var cemita = graphql(dueno, "mutation($i: RestPlatilloInput!) { crearPlatillo(input: $i) { id } }",
                Map.of("i", Map.of("nombre", "Cemita poblana", "precio", "85.00")), null).at("/data/crearPlatillo/id").asText();
        var enlace = graphql(dueno, "query($m: ID!) { qrDeMesa(mesaId: $m) }", Map.of("m", mesa), null)
                .at("/data/qrDeMesa").asText();
        return new Armado(negocio, dueno, enlace.substring(enlace.lastIndexOf('/') + 1), cemita);
    }

    private void pedirPorQr(Armado a, String llave) throws Exception {
        graphql(null, "mutation($i: RestPedirPorQrInput!) { pedirPorQr(input: $i) { abrioLaCuenta } }",
                Map.of("i", Map.of("token", a.qr(), "dispositivoId", DISPOSITIVO, "lineas",
                        java.util.List.of(Map.of("platilloId", a.cemita(), "cantidad", 2, "nota", "sin cebolla")))), llave);
    }

    @Test
    @DisplayName("QR-06, QR-07: un pedido por QR llega a la cocina como comanda y al salon como alerta, sin accion del mesero")
    void pedidoPorQr_llegaEnVivo() throws Exception {
        var a = armar();
        var cocinero = token(a.negocio(), "COCINERO");
        var mesero = token(a.negocio(), "MESERO");
        try (var cocina = conectado(cocinero); var salon = conectado(mesero)) {
            cocina.suscribir("1", "subscription { comandasDeCocina { tipo estacion estado mesa numeroDeCuenta "
                    + "lineas { platillo cantidad nota } } }");
            salon.suscribir("2", "subscription { salonActualizado { tipo mesa lineas abrioLaCuenta libres total abierto } }");
            esperarSuscritos(2);

            pedirPorQr(a, "pedido-ws-0001");

            var comanda = cocina.esperar("next", ESPERA).at("/payload/data/comandasDeCocina");
            assertThat(comanda.at("/tipo").asText()).isEqualTo("ENVIADA");
            assertThat(comanda.at("/estacion").asText()).isEqualTo("COCINA");
            assertThat(comanda.at("/estado").asText()).isEqualTo("NUEVA");
            assertThat(comanda.at("/mesa").asText()).isEqualTo("Mesa 1");
            assertThat(comanda.at("/lineas/0/platillo").asText()).isEqualTo("Cemita poblana");
            assertThat(comanda.at("/lineas/0/nota").asText()).isEqualTo("sin cebolla");
            var tipos = new java.util.ArrayList<String>();
            for (var i = 0; i < 2; i++) {
                tipos.add(salon.esperar("next", ESPERA).at("/payload/data/salonActualizado/tipo").asText());
            }
            assertThat(tipos).containsExactlyInAnyOrder("PEDIDO_QR", "DISPONIBILIDAD");
        }
    }

    @Test
    @DisplayName("SG-16: la cocina de un negocio no recibe lo del pedido por QR de otro")
    void otroNegocio_noRecibe() throws Exception {
        var a = armar();
        var otro = armar();
        var cocinaDeOtro = token(otro.negocio(), "COCINERO");
        try (var cliente = conectado(cocinaDeOtro)) {
            cliente.suscribir("1", "subscription { comandasDeCocina { tipo } }");
            esperarSuscritos(1);

            pedirPorQr(a, "pedido-ws-0002");

            assertThat(cliente.siguienteSinPings(Duration.ofMillis(800))).isNull();
        }
    }

    @Test
    @DisplayName("SG-04: quien no tiene restaurantes.comanda.ver ni restaurantes.salon.ver (la guia) no abre ninguna de las dos suscripciones")
    void sinPermiso_seNiega() throws Exception {
        var a = armar();
        var sinPermiso = token(a.negocio(), "GUIA");
        try (var cliente = conectado(sinPermiso)) {
            cliente.suscribir("1", "subscription { comandasDeCocina { tipo } }");
            cliente.suscribir("2", "subscription { salonActualizado { tipo } }");

            // Cada una se niega y se cierra: nunca llega un dato de la suscripcion.
            var mensajes = new java.util.ArrayList<JsonNode>();
            for (JsonNode m = cliente.siguienteSinPings(Duration.ofSeconds(2)); m != null;
                    m = cliente.siguienteSinPings(Duration.ofMillis(500))) {
                mensajes.add(m);
            }
            assertThat(mensajes).isNotEmpty();
            for (var m : mensajes) {
                assertThat(m.at("/payload/data/comandasDeCocina").isMissingNode() || m.at("/payload/data/comandasDeCocina").isNull())
                        .as(m.toString()).isTrue();
                assertThat(m.at("/payload/data/salonActualizado").isMissingNode() || m.at("/payload/data/salonActualizado").isNull())
                        .as(m.toString()).isTrue();
            }
            assertThat(mensajes.toString()).contains("\"id\":\"1\"").contains("\"id\":\"2\"");
            assertThat(avisos.suscritos()).isZero();
            pedirPorQr(a, "pedido-ws-0003");
            var despues = cliente.siguienteSinPings(Duration.ofMillis(800));
            assertThat(despues == null || !"next".equals(despues.path("type").asText())).as("nada llega a quien no puede ver").isTrue();
        }
    }
}
