package mx.negocio360.seguridad;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.WebSocketHttpHeaders;

/**
 * Solo pruebas: un cliente del protocolo graphql-transport-ws contra un servidor real. Un apreton de manos rechazado lanza
 * una excepcion cuyo mensaje trae el codigo HTTP.
 */
public final class ClienteWebSocket implements AutoCloseable {

    private static final ObjectMapper JSON = new ObjectMapper();

    private final WebSocketSession sesion;
    private final BlockingQueue<String> mensajes;
    private final CompletableFuture<CloseStatus> cierre;

    private ClienteWebSocket(WebSocketSession sesion, BlockingQueue<String> mensajes,
                             CompletableFuture<CloseStatus> cierre) {
        this.sesion = sesion;
        this.mensajes = mensajes;
        this.cierre = cierre;
    }

    /** @param origen cabecera Origin, o nulo para no mandarla (como una app nativa) */
    public static ClienteWebSocket conectar(int puerto, String origen) throws Exception {
        var mensajes = new LinkedBlockingQueue<String>();
        var cierre = new CompletableFuture<CloseStatus>();
        var manejador = new TextWebSocketHandler() {
            @Override
            protected void handleTextMessage(WebSocketSession s, TextMessage mensaje) {
                mensajes.add(mensaje.getPayload());
            }

            @Override
            public void afterConnectionClosed(WebSocketSession s, CloseStatus estado) {
                cierre.complete(estado);
            }
        };
        var cabeceras = new WebSocketHttpHeaders();
        cabeceras.setSecWebSocketProtocol("graphql-transport-ws");
        if (origen != null) {
            cabeceras.setOrigin(origen);
        }
        var sesion = new StandardWebSocketClient()
                .execute(manejador, cabeceras, URI.create("ws://localhost:" + puerto + "/graphql"))
                .get(5, TimeUnit.SECONDS);
        return new ClienteWebSocket(sesion, mensajes, cierre);
    }

    /** El texto de la excepcion de un apreton rechazado, con todas sus causas, para buscar el codigo HTTP. */
    public static String motivoDelRechazo(int puerto, String origen) {
        try (var cliente = conectar(puerto, origen)) {
            return null;
        } catch (Exception e) {
            var texto = new StringBuilder();
            for (Throwable t = e; t != null; t = t.getCause()) {
                texto.append(t.getMessage()).append(" | ");
            }
            return texto.toString();
        }
    }

    public void enviar(String texto) throws Exception {
        sesion.sendMessage(new TextMessage(texto));
    }

    public void iniciar(String token) throws Exception {
        enviar("{\"type\":\"connection_init\",\"payload\":{\"Authorization\":\"Bearer " + token + "\"}}");
    }

    public void suscribir(String id, String consulta) throws Exception {
        enviar(JSON.writeValueAsString(java.util.Map.of("id", id, "type", "subscribe", "payload",
                java.util.Map.of("query", consulta))));
    }

    public void completar(String id) throws Exception {
        enviar("{\"id\":\"" + id + "\",\"type\":\"complete\"}");
    }

    /** El siguiente mensaje, o nulo si no llega en ese tiempo. */
    public JsonNode siguiente(Duration espera) throws Exception {
        var texto = mensajes.poll(espera.toMillis(), TimeUnit.MILLISECONDS);
        return texto == null ? null : JSON.readTree(texto);
    }

    /** El siguiente mensaje que no sea un ping del servidor. */
    public JsonNode siguienteSinPings(Duration espera) throws Exception {
        var limite = System.nanoTime() + espera.toNanos();
        while (System.nanoTime() < limite) {
            var mensaje = siguiente(Duration.ofMillis(Math.max(1, (limite - System.nanoTime()) / 1_000_000)));
            if (mensaje == null || !"ping".equals(mensaje.path("type").asText())) {
                return mensaje;
            }
        }
        return null;
    }

    /** Espera el mensaje de ese tipo, descartando pings. Falla si no llega. */
    public JsonNode esperar(String tipo, Duration espera) throws Exception {
        var mensaje = siguienteSinPings(espera);
        if (mensaje == null || !tipo.equals(mensaje.path("type").asText())) {
            throw new AssertionError("Se esperaba " + tipo + " y llego " + mensaje);
        }
        return mensaje;
    }

    /** Como cerro el servidor la conexion, o nulo si no la cerro en ese tiempo. */
    public CloseStatus cierre(Duration espera) throws Exception {
        try {
            return cierre.get(espera.toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            return null;
        }
    }

    @Override
    public void close() throws Exception {
        if (sesion.isOpen()) {
            sesion.close();
        }
    }
}
