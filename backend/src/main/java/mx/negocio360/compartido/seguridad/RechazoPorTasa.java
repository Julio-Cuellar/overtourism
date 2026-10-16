package mx.negocio360.compartido.seguridad;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** La respuesta de SG-10: 429 con el tiempo de espera y un cuerpo fijo que no dice que limite se paso. */
final class RechazoPorTasa {

    static final String CUERPO = "{\"codigo\":\"DEMASIADAS_SOLICITUDES\"}";

    private RechazoPorTasa() {
    }

    static void escribir(HttpServletResponse respuesta, long reintentarEnSegundos) throws IOException {
        respuesta.setStatus(429);
        respuesta.setHeader("Retry-After", Long.toString(reintentarEnSegundos));
        respuesta.setContentType("application/json");
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.getWriter().write(CUERPO);
    }
}
