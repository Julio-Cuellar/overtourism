package mx.negocio360.compartido.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Objects;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * SG-10, segunda mitad: limita las peticiones de una cuenta con sesion valida. Va despues de la autenticacion porque
 * necesita saber quien es. Sin sesion no cuenta nada: de eso se ocupa el limite por IP.
 */
public final class LimiteDeTasaPorCuentaFilter extends OncePerRequestFilter {

    private final LimitadorDeTasa peticiones;
    private final RequestMatcher ruta = PathPatternRequestMatcher.withDefaults().matcher("/graphql");

    public LimiteDeTasaPorCuentaFilter(LimitadorDeTasa peticiones) {
        this.peticiones = Objects.requireNonNull(peticiones, "peticiones");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        return !ruta.matches(peticion);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() instanceof SesionAuthentication sesion) {
            var decision = peticiones.consumir(sesion.sesion().actor());
            if (!decision.permitido()) {
                RechazoPorTasa.escribir(respuesta, decision.reintentarEnSegundos());
                return;
            }
        }
        cadena.doFilter(peticion, respuesta);
    }
}
