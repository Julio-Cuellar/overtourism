package mx.negocio360.compartido.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * La puerta HTTP de {@code /graphql} (SG-14 y SG-09). Hoy corre despues del filtro de entrada ({@code FiltroDeEntrada}), que ya
 * valida metodo, tipo y tamano y autentica; lo que solo se verifica aqui es el origen. Antes de autenticar, en origen: lo que no cumple se rechaza sin gastar
 * trabajo en Identidad ni en el motor de GraphQL, y siempre con una respuesta fija.
 *
 * <ol>
 *   <li>{@code Origin}: si viene, debe ser uno de los permitidos (403). Un navegador siempre lo envia en un POST
 *       entre sitios; las apps nativas no. Sin origenes configurados no pasa ninguno.
 *   <li>Solo POST (405 con {@code Allow: POST}).
 *   <li>Solo {@code application/json}, en UTF-8 (415).
 *   <li>Cuerpo de hasta el tope (413). Si el cliente anuncia mas, ni se lee; si no anuncia longitud (chunked) se
 *       corta al pasar el tope, sin cargarlo entero (el filtro de entrada, que corre antes, ya pide la longitud: 411).
 *   <li>Un solo objeto JSON (400): los lotes (arreglos de consultas) no existen.
 * </ol>
 *
 * <p>La subida a WebSocket de {@code /graphql} (un GET con {@code Upgrade}, SG-15) solo pasa por la verificacion del
 * origen; el tipo y el cuerpo no aplican. Sus mensajes ya no son HTTP: los limitan los interceptores de GraphQL.
 */
public final class PeticionGraphqlEstrictaFilter extends OncePerRequestFilter {

    static final String RUTA = "/graphql";
    private static final byte[] LLAVE = {'{'};

    private final int cuerpoMaximo;
    private final Set<String> origenesPermitidos;
    private final RequestMatcher ruta = PathPatternRequestMatcher.withDefaults().matcher(RUTA);

    public PeticionGraphqlEstrictaFilter(PropiedadesHttp propiedades) {
        Objects.requireNonNull(propiedades, "propiedades");
        this.cuerpoMaximo = propiedades.cuerpoMaximoBytes();
        this.origenesPermitidos = Set.copyOf(propiedades.origenesPermitidos());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        return !ruta.matches(peticion);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        if (!origenPermitido(peticion)) {
            rechazar(respuesta, HttpServletResponse.SC_FORBIDDEN, "ORIGEN_NO_PERMITIDO");
            return;
        }
        if (SubidaWebSocket.es(peticion)) {
            // El apreton de manos de un WebSocket: sin tipo ni cuerpo. Su origen ya se verifico arriba.
            cadena.doFilter(peticion, respuesta);
            return;
        }
        if (!"POST".equals(peticion.getMethod())) {
            respuesta.setHeader(HttpHeaders.ALLOW, "POST");
            rechazar(respuesta, HttpServletResponse.SC_METHOD_NOT_ALLOWED, "METODO_NO_PERMITIDO");
            return;
        }
        if (!esJsonUtf8(peticion.getContentType())) {
            rechazar(respuesta, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE, "TIPO_NO_SOPORTADO");
            return;
        }
        if (peticion.getContentLengthLong() > cuerpoMaximo) {
            rechazar(respuesta, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "CUERPO_MUY_GRANDE");
            return;
        }
        // Se lee un byte mas que el tope: si llega, el cuerpo lo excede aunque el cliente no haya dicho su longitud.
        var cuerpo = peticion.getInputStream().readNBytes(cuerpoMaximo + 1);
        if (cuerpo.length > cuerpoMaximo) {
            rechazar(respuesta, HttpServletResponse.SC_REQUEST_ENTITY_TOO_LARGE, "CUERPO_MUY_GRANDE");
            return;
        }
        if (!empiezaConObjeto(cuerpo)) {
            rechazar(respuesta, HttpServletResponse.SC_BAD_REQUEST, "PETICION_INVALIDA");
            return;
        }
        cadena.doFilter(new CuerpoYaLeidoRequest(peticion, cuerpo), respuesta);
    }

    private boolean origenPermitido(HttpServletRequest peticion) {
        var origenes = Collections.list(peticion.getHeaders(HttpHeaders.ORIGIN));
        if (origenes.isEmpty()) {
            return true;
        }
        return origenes.size() == 1 && origenesPermitidos.contains(origenes.get(0).strip().toLowerCase(Locale.ROOT));
    }

    private static boolean esJsonUtf8(String tipo) {
        if (tipo == null || tipo.isBlank()) {
            return false;
        }
        try {
            var media = MediaType.parseMediaType(tipo);
            if (!MediaType.APPLICATION_JSON.equalsTypeAndSubtype(media)) {
                return false;
            }
            var juego = media.getCharset();
            return juego == null || StandardCharsets.UTF_8.equals(juego);
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** El primer caracter que no es espacio en blanco debe ser {@code {}: ni arreglos, ni texto, ni numeros. */
    private static boolean empiezaConObjeto(byte[] cuerpo) {
        for (var b : cuerpo) {
            if (b == ' ' || b == '\t' || b == '\n' || b == '\r') {
                continue;
            }
            return b == LLAVE[0];
        }
        return false;
    }

    private static void rechazar(HttpServletResponse respuesta, int estado, String codigo) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType("application/json");
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.getWriter().write("{\"codigo\":\"" + codigo + "\"}");
    }
}
