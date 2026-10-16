package mx.negocio360.compartido.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * SG-01: autentica cada peticion con el token de {@code Authorization: Bearer} (SI-05, no hay cookies) y deja la
 * sesion en el contexto de seguridad para el resto de la cadena.
 *
 * <ul>
 *   <li>Sin cabecera: sigue sin sesion; la cadena de filtros decide (401 en lo que no es publico).
 *   <li>Cabecera mal formada, repetida o con un token absurdamente largo: 401, sin preguntarle a Identidad.
 *   <li>Token que Identidad no reconoce: el mismo 401, sea falso, vencido o revocado (AS-08).
 * </ul>
 *
 * <p>No toca las rutas publicas: validan su propio token y reinterpretarlo aqui cambiaria su comportamiento.
 */
public final class BearerTokenFilter extends OncePerRequestFilter {

    /** Los tokens reales miden 43 caracteres (32 bytes en base 64 url); mas de esto no puede ser uno. */
    static final int MAX_CARACTERES_DEL_TOKEN = 256;
    private static final Pattern BEARER = Pattern.compile("(?i)^Bearer\\s+(\\S+)$");

    private final Autenticador autenticador;
    private final RequestMatcher rutasPublicas;
    private final RequestAttributeSecurityContextRepository repositorio = new RequestAttributeSecurityContextRepository();
    private final EntradaNoAutenticada entrada = new EntradaNoAutenticada();

    public BearerTokenFilter(Autenticador autenticador, List<String> patronesPublicos) {
        this.autenticador = Objects.requireNonNull(autenticador, "autenticador");
        var matchers = patronesPublicos.stream()
                .map(patron -> (RequestMatcher) PathPatternRequestMatcher.withDefaults().matcher(patron)).toList();
        this.rutasPublicas = matchers.isEmpty() ? peticion -> false : new OrRequestMatcher(matchers);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        return rutasPublicas.matches(peticion);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        var cabeceras = Collections.list(peticion.getHeaders(HttpHeaders.AUTHORIZATION));
        if (cabeceras.isEmpty()) {
            cadena.doFilter(peticion, respuesta);
            return;
        }
        var token = cabeceras.size() == 1 ? tokenDe(cabeceras.get(0)) : null;
        var sesion = token == null ? null : autenticador.autenticar(token).orElse(null);
        if (sesion == null) {
            rechazar(peticion, respuesta);
            return;
        }
        var contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(new SesionAuthentication(sesion));
        SecurityContextHolder.setContext(contexto);
        // Sin esto, el despacho asincrono de una ruta REST llegaria sin sesion a la autorizacion y se negaria.
        repositorio.saveContext(contexto, peticion, respuesta);
        cadena.doFilter(peticion, respuesta);
    }

    private static String tokenDe(String cabecera) {
        var coincidencia = BEARER.matcher(cabecera);
        if (!coincidencia.matches() || coincidencia.group(1).length() > MAX_CARACTERES_DEL_TOKEN) {
            return null;
        }
        return coincidencia.group(1);
    }

    private void rechazar(HttpServletRequest peticion, HttpServletResponse respuesta) throws IOException {
        SecurityContextHolder.clearContext();
        entrada.commence(peticion, respuesta, new InsufficientAuthenticationException("Token no valido"));
    }
}
