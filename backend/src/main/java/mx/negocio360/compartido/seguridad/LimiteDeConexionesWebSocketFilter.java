package mx.negocio360.compartido.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * SG-15: tope de conexiones WebSocket nuevas por IP. El apreton de manos pasa sin sesion (la autenticacion es el mensaje
 * connection_init), asi que sin este tope cualquiera podria abrir conexiones sin parar. Solo cuenta las subidas.
 */
public final class LimiteDeConexionesWebSocketFilter extends OncePerRequestFilter {

    private final LimitadorDeTasa conexiones;
    private final RequestMatcher ruta = PathPatternRequestMatcher.withDefaults().matcher("/graphql");

    public LimiteDeConexionesWebSocketFilter(LimitadorDeTasa conexiones) {
        this.conexiones = Objects.requireNonNull(conexiones, "conexiones");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        return !ruta.matches(peticion) || !SubidaWebSocket.es(peticion);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        var decision = conexiones.consumir(peticion.getRemoteAddr());
        if (!decision.permitido()) {
            RechazoPorTasa.escribir(respuesta, decision.reintentarEnSegundos());
            return;
        }
        cadena.doFilter(peticion, respuesta);
    }
}
