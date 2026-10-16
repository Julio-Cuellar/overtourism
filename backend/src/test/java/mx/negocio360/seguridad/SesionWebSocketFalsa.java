package mx.negocio360.seguridad;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.graphql.server.WebSocketSessionInfo;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

/** Solo pruebas: la informacion de una conexion WebSocket, con su IP y los atributos que guardan los interceptores. */
final class SesionWebSocketFalsa implements WebSocketSessionInfo {

    private final Map<String, Object> atributos = new HashMap<>();
    private final InetSocketAddress direccion;

    SesionWebSocketFalsa(String ip) {
        this.direccion = ip == null ? null : new InetSocketAddress(ip, 5000);
    }

    @Override
    public String getId() {
        return "sesion-falsa";
    }

    @Override
    public Map<String, Object> getAttributes() {
        return atributos;
    }

    @Override
    public URI getUri() {
        return URI.create("ws://localhost/graphql");
    }

    @Override
    public HttpHeaders getHeaders() {
        return new HttpHeaders();
    }

    @Override
    public Mono<Principal> getPrincipal() {
        return Mono.empty();
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return direccion;
    }
}
