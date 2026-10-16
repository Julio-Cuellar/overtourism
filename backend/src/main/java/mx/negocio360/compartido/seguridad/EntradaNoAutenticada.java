package mx.negocio360.compartido.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * SG-01 y SG-13: toda peticion sin sesion valida recibe la misma respuesta, sea porque falta el token, es falso, vencio
 * o fue revocado (AS-08). El cuerpo es fijo: no dice por que ni nombra la ruta.
 */
public final class EntradaNoAutenticada implements AuthenticationEntryPoint {

    public static final String CUERPO = "{\"codigo\":\"NO_AUTENTICADO\"}";

    @Override
    public void commence(HttpServletRequest peticion, HttpServletResponse respuesta,
                         AuthenticationException causa) throws IOException {
        respuesta.setStatus(HttpStatus.UNAUTHORIZED.value());
        respuesta.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        respuesta.setContentType(MediaType.APPLICATION_JSON_VALUE);
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.getWriter().write(CUERPO);
    }
}
