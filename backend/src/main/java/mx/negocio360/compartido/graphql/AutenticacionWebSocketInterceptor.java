package mx.negocio360.compartido.graphql;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import mx.negocio360.compartido.seguridad.Autenticador;
import mx.negocio360.compartido.seguridad.LimitadorDeTasa;
import org.springframework.core.Ordered;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.server.WebSocketGraphQlInterceptor;
import org.springframework.graphql.server.WebSocketGraphQlRequest;
import org.springframework.graphql.server.WebSocketSessionInfo;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

/**
 * SG-15: la autenticacion de una conexion WebSocket es el mensaje {@code connection_init} con
 * {@code {"Authorization": "Bearer <token>"}} (un navegador no puede mandar cabeceras en el apreton de manos). Cualquier
 * problema es un error, que el manejador convierte en el cierre 4401, y cuenta como intento fallido de la IP, la misma
 * que limita los de HTTP: una IP agotada ni siquiera llega a consultar a Identidad.
 *
 * <p>Basta una sesion valida, aunque todavia no tenga negocio activo: la suscripcion {@code altaDeNegocio} existe justo para
 * quien acaba de registrarse. Lo que pide negocio activo lo niega la puerta de autorizacion de cada operacion.
 *
 * <p>Se guarda el <b>token</b>, no la sesion, y cada operacion nueva lo vuelve a validar: una sesion revocada, un permiso
 * quitado o un negocio suspendido se notan en la siguiente operacion, no cuando se cierre la conexion. (Una suscripcion
 * que ya esta abierta sigue hasta que termine; es un limite conocido.) Tambien aplica el tope de peticiones por cuenta a
 * cada operacion y deja la sesion en el contexto de Reactor, de donde la toman la autorizacion y los resolvedores.
 */
public final class AutenticacionWebSocketInterceptor implements WebSocketGraphQlInterceptor, Ordered {

    public static final String CLAVE_DEL_TOKEN = "n360.token";
    public static final String NO_AUTENTICADO = "NO_AUTENTICADO";
    public static final String DEMASIADAS_SOLICITUDES = "DEMASIADAS_SOLICITUDES";
    /** Antes que todos los demas: lo que escribe en el contexto de Reactor lo ven los que van por dentro. */
    public static final int ORDEN = Ordered.HIGHEST_PRECEDENCE;

    private static final Pattern BEARER = Pattern.compile("(?i)^Bearer\\s+(\\S+)$");
    private static final int MAX_CARACTERES_DEL_TOKEN = 256;

    private final Autenticador autenticador;
    private final LimitadorDeTasa fallosPorIp;
    private final LimitadorDeTasa peticionesPorCuenta;

    public AutenticacionWebSocketInterceptor(Autenticador autenticador, LimitadorDeTasa fallosPorIp,
                                             LimitadorDeTasa peticionesPorCuenta) {
        this.autenticador = Objects.requireNonNull(autenticador, "autenticador");
        this.fallosPorIp = Objects.requireNonNull(fallosPorIp, "fallosPorIp");
        this.peticionesPorCuenta = Objects.requireNonNull(peticionesPorCuenta, "peticionesPorCuenta");
    }

    @Override
    public int getOrder() {
        return ORDEN;
    }

    @Override
    public Mono<Object> handleConnectionInitialization(WebSocketSessionInfo sesion, Map<String, Object> carga) {
        return Mono.defer(() -> {
            var ip = ipDe(sesion);
            if (!fallosPorIp.probar(ip).permitido()) {
                return Mono.error(new ConexionRechazada());
            }
            var token = tokenDe(carga);
            var valida = token != null && autenticador.autenticar(token).isPresent();
            if (!valida) {
                fallosPorIp.consumir(ip);
                return Mono.error(new ConexionRechazada());
            }
            sesion.getAttributes().put(CLAVE_DEL_TOKEN, token);
            return Mono.empty();
        });
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest peticion, Chain cadena) {
        if (!(peticion instanceof WebSocketGraphQlRequest socket)) {
            return cadena.next(peticion);
        }
        // SG-10: una sesion revocada que sigue mandando operaciones gasta fichas de la IP, igual que un token falso; agotadas,
        // ni siquiera se le pregunta a Identidad.
        var ip = ipDe(socket.getSessionInfo());
        if (!fallosPorIp.probar(ip).permitido()) {
            return Mono.just(RechazoGraphql.de(peticion, NO_AUTENTICADO));
        }
        var token = socket.getSessionInfo().getAttributes().get(CLAVE_DEL_TOKEN);
        var sesion = token instanceof String texto ? autenticador.autenticar(texto).orElse(null) : null;
        if (sesion == null) {
            fallosPorIp.consumir(ip);
            return Mono.just(RechazoGraphql.de(peticion, NO_AUTENTICADO));
        }
        if (!peticionesPorCuenta.consumir(sesion.actor()).permitido()) {
            return Mono.just(RechazoGraphql.de(peticion, DEMASIADAS_SOLICITUDES));
        }
        // La puerta de autorizacion y los resolvedores leen la entrada del contexto de GraphQL, no del hilo.
        var entradaGraphQl = EntradaGraphQl.deSesion(sesion, null);
        peticion.configureExecutionInput((entrada, constructor) -> constructor
                .graphQLContext(contextoGraphql -> contextoGraphql.put(EntradaGraphQl.CLAVE, entradaGraphQl)).build());
        return cadena.next(peticion).contextWrite(Context.of(EntradaGraphQl.CLAVE, entradaGraphQl));
    }

    private static String tokenDe(Map<String, Object> carga) {
        for (var entrada : carga.entrySet()) {
            if ("authorization".equalsIgnoreCase(entrada.getKey()) && entrada.getValue() instanceof String valor) {
                var coincidencia = BEARER.matcher(valor);
                if (coincidencia.matches() && coincidencia.group(1).length() <= MAX_CARACTERES_DEL_TOKEN) {
                    return coincidencia.group(1);
                }
                return null;
            }
        }
        return null;
    }

    private static String ipDe(WebSocketSessionInfo sesion) {
        InetSocketAddress direccion = sesion.getRemoteAddress();
        if (direccion == null) {
            return "desconocida";
        }
        return direccion.getAddress() != null ? direccion.getAddress().getHostAddress() : direccion.getHostString();
    }

    /** Sin detalle: el cliente no debe saber si faltaba el token, era falso o su IP estaba agotada. */
    private static final class ConexionRechazada extends RuntimeException {
        private static final long serialVersionUID = 1L;

        ConexionRechazada() {
            super("NO_AUTENTICADO", null, false, false);
        }
    }
}
