package mx.negocio360.compartido.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.Locale;

/**
 * Reconoce el apreton de manos de un WebSocket: un GET con {@code Upgrade: websocket} y una cabecera {@code Connection}
 * que incluya {@code upgrade}. Un navegador no puede mandar {@code Authorization} en ese GET, asi que la autenticacion
 * es el mensaje connection_init y no el apreton (SG-15); lo demas de /graphql sigue exigiendo sesion.
 */
public final class SubidaWebSocket {

    private SubidaWebSocket() {
    }

    public static boolean es(HttpServletRequest peticion) {
        if (!"GET".equals(peticion.getMethod())) {
            return false;
        }
        var upgrade = peticion.getHeader("Upgrade");
        if (upgrade == null || !upgrade.strip().equalsIgnoreCase("websocket")) {
            return false;
        }
        return Collections.list(peticion.getHeaders("Connection")).stream()
                .flatMap(valor -> java.util.Arrays.stream(valor.split(",")))
                .anyMatch(token -> token.strip().toLowerCase(Locale.ROOT).equals("upgrade"));
    }
}
