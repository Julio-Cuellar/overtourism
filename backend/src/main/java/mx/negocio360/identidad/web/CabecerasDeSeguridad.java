package mx.negocio360.identidad.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Cabeceras de las rutas de acceso (SI-17): nada de lo que responden se guarda en caché (llevan tokens, códigos y
 * datos personales) ni se reinterpreta como otro tipo de contenido. Se pone antes de ejecutar la petición para que
 * también salgan en los errores.
 */
public final class CabecerasDeSeguridad extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        respuesta.setHeader("Cache-Control", "no-store");
        respuesta.setHeader("X-Content-Type-Options", "nosniff");
        cadena.doFilter(peticion, respuesta);
    }
}
