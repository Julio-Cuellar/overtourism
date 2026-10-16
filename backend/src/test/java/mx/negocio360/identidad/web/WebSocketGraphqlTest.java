package mx.negocio360.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import mx.negocio360.seguridad.CapturaDeLogs;
import mx.negocio360.seguridad.ClienteWebSocket;
import mx.negocio360.seguridad.EventoDePrueba;
import mx.negocio360.seguridad.EventosDePrueba;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.socket.CloseStatus;

/**
 * SG-15 y SG-16 contra un servidor real (Tomcat en un puerto al azar): apreton de manos, autenticacion en connection_init,
 * codigos de cierre, tope de suscripciones, aislamiento de eventos por negocio y errores dentro de un flujo. Es lo unico que
 * ejercita el WebSocket de verdad: los filtros HTTP solo ven el apreton y despues todo es del interceptor de GraphQL.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "seguridad.websocket.suscripciones-por-cuenta=2", "spring.graphql.websocket.connection-init-timeout=1s"})
@AutoConfigureMockMvc
class WebSocketGraphqlTest {

    private static final Duration ESPERA = Duration.ofSeconds(5);

    @LocalServerPort
    private int puerto;

    @Autowired
    private MockMvc mvc;

    private FlujoHttp.SesionConNegocio sesion() throws Exception {
        return new FlujoHttp(mvc).sesionConNegocio();
    }

    private ClienteWebSocket conectado(String token) throws Exception {
        var cliente = ClienteWebSocket.conectar(puerto, null);
        cliente.iniciar(token);
        assertThat(cliente.esperar("connection_ack", ESPERA)).isNotNull();
        return cliente;
    }

    // --- Apreton de manos y connection_init ---

    @Test
    @DisplayName("con un token bueno en connection_init se abre la conexion y una consulta responde sus datos")
    void conexionAutenticada_ejecutaUnaConsulta() throws Exception {
        try (var cliente = conectado(sesion().token())) {
            cliente.suscribir("1", "{ arbolDePrueba { valor } }");

            var siguiente = cliente.esperar("next", ESPERA);
            assertThat(siguiente.path("id").asText()).isEqualTo("1");
            assertThat(siguiente.at("/payload/data/arbolDePrueba/valor").asText()).isEqualTo("hoja");
            assertThat(cliente.esperar("complete", ESPERA).path("id").asText()).isEqualTo("1");
        }
    }

    @Test
    @DisplayName("sin token, con un token falso o con un esquema que no es Bearer, la conexion se cierra con 4401")
    void initInvalido_cierraCon4401() throws Exception {
        for (var carga : List.of("{}", "{\"Authorization\":\"Bearer " + "A".repeat(43) + "\"}",
                "{\"Authorization\":\"Basic abc\"}")) {
            try (var cliente = ClienteWebSocket.conectar(puerto, null)) {
                cliente.enviar("{\"type\":\"connection_init\",\"payload\":" + carga + "}");

                var cierre = cliente.cierre(ESPERA);

                assertThat(cierre).as(carga).isNotNull();
                assertThat(cierre.getCode()).as(carga).isEqualTo(4401);
            }
        }
    }

    @Test
    @DisplayName("una operacion antes de connection_init cierra con 4401")
    void subscribeAntesDeInit_cierraCon4401() throws Exception {
        try (var cliente = ClienteWebSocket.conectar(puerto, null)) {
            cliente.suscribir("1", "{ arbolDePrueba { valor } }");

            assertThat(cliente.cierre(ESPERA).getCode()).isEqualTo(4401);
        }
    }

    @Test
    @DisplayName("una conexion que no manda connection_init a tiempo se cierra con 4408")
    void sinInit_cierraPorTiempo() throws Exception {
        try (var cliente = ClienteWebSocket.conectar(puerto, null)) {
            var cierre = cliente.cierre(Duration.ofSeconds(4));

            assertThat(cierre).isNotNull();
            assertThat(cierre.getCode()).isEqualTo(4408);
        }
    }

    @Test
    @DisplayName("el origen se verifica en el apreton: uno ajeno recibe 403; el permitido y la ausencia de Origin pasan")
    void origen_seVerificaEnElApreton() throws Exception {
        assertThat(ClienteWebSocket.motivoDelRechazo(puerto, "https://malo.test")).contains("403");

        try (var conOrigen = ClienteWebSocket.conectar(puerto, "https://panel.ejemplo.test");
             var sinOrigen = ClienteWebSocket.conectar(puerto, null)) {
            assertThat(conOrigen).isNotNull();
            assertThat(sinOrigen).isNotNull();
        }
    }

    @Test
    @DisplayName("un mensaje de mas de 64 KB cierra la conexion con 1009; uno de 9000 caracteres (mas que los 8 KB de Tomcat) pasa")
    void tamanoDeMensaje() throws Exception {
        try (var cliente = conectado(sesion().token())) {
            var base = "{ arbolDePrueba { valor } }";
            cliente.suscribir("1", base + " ".repeat(9_000 - base.length()));
            assertThat(cliente.esperar("next", ESPERA).at("/payload/data/arbolDePrueba/valor").asText()).isEqualTo("hoja");

            cliente.enviar("{\"id\":\"2\",\"type\":\"subscribe\",\"payload\":{\"query\":\"" + "a".repeat(70_000) + "\"}}");

            assertThat(cliente.cierre(ESPERA).getCode()).isEqualTo(1009);
        }
    }

    // --- Cada operacion se valida de nuevo ---

    @Test
    @DisplayName("si la sesion se revoca despues de conectar, la siguiente operacion recibe NO_AUTENTICADO")
    void sesionRevocada_seNotaEnLaSiguienteOperacion() throws Exception {
        var sesion = sesion();
        try (var cliente = conectado(sesion.token())) {
            cliente.suscribir("1", "{ arbolDePrueba { valor } }");
            assertThat(cliente.esperar("next", ESPERA).at("/payload/data/arbolDePrueba/valor").asText()).isEqualTo("hoja");
            cliente.esperar("complete", ESPERA);

            mvc.perform(delete("/auth/sesion").header("Authorization", "Bearer " + sesion.token()));
            cliente.suscribir("2", "{ arbolDePrueba { valor } }");

            var siguiente = cliente.esperar("next", ESPERA);
            assertThat(siguiente.at("/payload/errors/0/extensions/codigo").asText()).isEqualTo("NO_AUTENTICADO");
            assertThat(siguiente.at("/payload/data").isMissingNode() || siguiente.at("/payload/data").isNull()).isTrue();
        }
    }

    // --- Autorizacion de una suscripcion (SG-03 y SG-19) ---

    @Test
    @DisplayName("una suscripcion sensible pide la confirmacion reforzada: CONFIRMACION_REQUERIDA, aun para el DUENO")
    void suscripcionSensible_requiereConfirmacion() throws Exception {
        try (var cliente = conectado(sesion().token())) {
            cliente.suscribir("1", "subscription { sensibleSuscripcionDePrueba }");

            var siguiente = cliente.esperar("next", ESPERA);

            assertThat(siguiente.at("/payload/errors/0/extensions/codigo").asText()).isEqualTo("CONFIRMACION_REQUERIDA");
        }
    }

    // --- Aislamiento de eventos por negocio (SG-16) ---

    private static void esperarSuscriptores(int cuantos) throws Exception {
        var limite = System.nanoTime() + Duration.ofSeconds(5).toNanos();
        while (EventosDePrueba.suscriptores() < cuantos && System.nanoTime() < limite) {
            Thread.sleep(25);
        }
        assertThat(EventosDePrueba.suscriptores()).isGreaterThanOrEqualTo(cuantos);
    }

    private static List<String> textosRecibidos(ClienteWebSocket cliente) throws Exception {
        var textos = new ArrayList<String>();
        JsonNode mensaje;
        while ((mensaje = cliente.siguienteSinPings(Duration.ofMillis(700))) != null) {
            if ("next".equals(mensaje.path("type").asText())) {
                textos.add(mensaje.at("/payload/data/eventosDePrueba").asText());
            }
        }
        return textos;
    }

    @Test
    @DisplayName("SG-16: dos sesiones de negocios distintos suscritas a la vez reciben cada una solo los eventos de su negocio")
    void eventos_cadaNegocioSoloVeLosSuyos() throws Exception {
        var a = sesion();
        var b = sesion();
        var base = EventosDePrueba.suscriptores();
        try (var clienteA = conectado(a.token()); var clienteB = conectado(b.token())) {
            clienteA.suscribir("1", "subscription { eventosDePrueba }");
            clienteB.suscribir("1", "subscription { eventosDePrueba }");
            esperarSuscriptores(base + 2);

            EventosDePrueba.publicar(new EventoDePrueba(a.negocio(), "a1"));
            EventosDePrueba.publicar(new EventoDePrueba(b.negocio(), "b1"));
            EventosDePrueba.publicar(new EventoDePrueba(a.negocio(), "a2"));
            EventosDePrueba.publicar(new EventoDePrueba(java.util.UUID.randomUUID(), "ajeno"));

            assertThat(textosRecibidos(clienteA)).containsExactly("a1", "a2");
            assertThat(textosRecibidos(clienteB)).containsExactly("b1");
        }
    }

    // --- Tope de suscripciones (SG-15) ---

    @Test
    @DisplayName("la tercera operacion abierta de una cuenta recibe TOPE_DE_SUSCRIPCIONES; completar una libera el lugar")
    void topeDeSuscripciones() throws Exception {
        try (var cliente = conectado(sesion().token())) {
            cliente.suscribir("1", "subscription { eventosDePrueba }");
            cliente.suscribir("2", "subscription { eventosDePrueba }");
            cliente.suscribir("3", "subscription { eventosDePrueba }");

            var rechazada = cliente.esperar("next", ESPERA);
            assertThat(rechazada.path("id").asText()).isEqualTo("3");
            assertThat(rechazada.at("/payload/errors/0/extensions/codigo").asText()).isEqualTo("TOPE_DE_SUSCRIPCIONES");
            assertThat(cliente.esperar("complete", ESPERA).path("id").asText()).isEqualTo("3");

            cliente.completar("1");
            Thread.sleep(300);
            cliente.suscribir("4", "subscription { eventosDePrueba }");
            assertThat(cliente.siguienteSinPings(Duration.ofMillis(800))).as("la cuarta si se acepta, no devuelve error")
                    .isNull();

            cliente.suscribir("5", "subscription { eventosDePrueba }");
            var quinta = cliente.esperar("next", ESPERA);
            assertThat(quinta.path("id").asText()).isEqualTo("5");
            assertThat(quinta.at("/payload/errors/0/extensions/codigo").asText()).isEqualTo("TOPE_DE_SUSCRIPCIONES");
        }
    }

    @Test
    @DisplayName("cerrar la conexion libera todos los lugares de la cuenta")
    void cerrarLaConexion_liberaLosLugares() throws Exception {
        var token = sesion().token();
        try (var primera = conectado(token)) {
            primera.suscribir("1", "subscription { eventosDePrueba }");
            primera.suscribir("2", "subscription { eventosDePrueba }");
            Thread.sleep(400);
        }

        try (var segunda = conectado(token)) {
            JsonNode ultimo = null;
            for (var intento = 0; intento < 20; intento++) {
                segunda.suscribir("a" + intento, "subscription { eventosDePrueba }");
                ultimo = segunda.siguienteSinPings(Duration.ofMillis(300));
                if (ultimo == null) {
                    break;
                }
                Thread.sleep(150);
            }
            assertThat(ultimo).as("la cuenta recupero sus lugares y la suscripcion se acepto").isNull();
        }
    }

    // --- Errores dentro del flujo (SG-13) y registro (SG-20) ---

    @Test
    @DisplayName("un error dentro del flujo sale enmascarado y su mensaje no llega ni al cliente ni al registro")
    void errorEnElFlujo_noFiltraElMensaje() throws Exception {
        try (var captura = new CapturaDeLogs("org.springframework.graphql.server.webmvc.GraphQlWebSocketHandler");
             var cliente = conectado(sesion().token())) {
            cliente.suscribir("1", "subscription { suscripcionQueFallaDePrueba }");

            var siguiente = cliente.siguienteSinPings(ESPERA);

            assertThat(siguiente).isNotNull();
            assertThat(siguiente.toString()).contains("ERROR_INTERNO").doesNotContain("secreto", "2221234567", "SELECT");
            assertThat(String.join("\n", captura.lineas())).doesNotContain("secreto", "2221234567", "SELECT");
        }
    }

    @Test
    @DisplayName("una operacion por WebSocket deja su linea de registro con la cuenta y el negocio de la sesion")
    void operacion_dejaLineaDeRegistro() throws Exception {
        var sesion = sesion();
        try (var captura = new CapturaDeLogs("auditoria.graphql"); var cliente = conectado(sesion.token())) {
            cliente.suscribir("1", "{ arbolDePrueba { valor } }");
            cliente.esperar("next", ESPERA);
            cliente.esperar("complete", ESPERA);

            var lineas = captura.lineas();
            assertThat(lineas).hasSize(1);
            assertThat(lineas.get(0)).contains("negocio=" + sesion.negocio()).contains("operacion=query:arbolDePrueba")
                    .contains("resultado=OK").doesNotContain("cuenta=- ").doesNotContain(sesion.token());
        }
    }

    @Test
    @DisplayName("el cierre normal de un cliente no deja la sesion abierta en el servidor")
    void cierreNormal() throws Exception {
        var cliente = conectado(sesion().token());

        cliente.close();

        assertThat(cliente.cierre(ESPERA)).isNotNull();
        assertThat(cliente.cierre(ESPERA).getCode()).isIn(CloseStatus.NORMAL.getCode(), CloseStatus.GOING_AWAY.getCode(),
                1005);
    }
}
