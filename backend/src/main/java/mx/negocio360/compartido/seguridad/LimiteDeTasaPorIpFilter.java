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
 * SG-10, primera mitad: limita los intentos sin sesion valida desde una misma IP, antes de autenticar. No cuenta todas
 * las peticiones sino las que terminan en 401 (sin token, o con uno falso, vencido o revocado), que es lo que hace
 * quien prueba tokens; una IP agotada recibe 429 sin que se consulte a Identidad. En /graphql, donde una peticion sin token
 * es anonima y valida, esos intentos los cuenta {@code FiltroDeEntrada}; este filtro sirve a las rutas REST autenticadas.
 *
 * <p>La IP es la de la conexion, no la de {@code X-Forwarded-For} (cualquiera puede escribir esa cabecera). Detras de un
 * proxy hay que configurar que la IP real llegue a la conexion. Y si una IP es compartida (una oficina, una red
 * celular), alguien que acumula fallos la bloquea tambien para sus companeros, aun con sesion valida: con 20 por minuto
 * solo ocurre ante un abuso.
 */
public final class LimiteDeTasaPorIpFilter extends OncePerRequestFilter {

    private final LimitadorDeTasa fallos;
    private final RequestMatcher ruta = PathPatternRequestMatcher.withDefaults().matcher("/graphql");

    public LimiteDeTasaPorIpFilter(LimitadorDeTasa fallos) {
        this.fallos = Objects.requireNonNull(fallos, "fallos");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        return !ruta.matches(peticion);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        var ip = peticion.getRemoteAddr();
        var estado = fallos.probar(ip);
        if (!estado.permitido()) {
            RechazoPorTasa.escribir(respuesta, estado.reintentarEnSegundos());
            return;
        }
        cadena.doFilter(peticion, respuesta);
        if (respuesta.getStatus() == HttpServletResponse.SC_UNAUTHORIZED) {
            fallos.consumir(ip);
        }
    }
}
