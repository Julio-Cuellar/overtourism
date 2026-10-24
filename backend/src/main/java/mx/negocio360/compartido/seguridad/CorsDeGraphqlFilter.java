package mx.negocio360.compartido.seguridad;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * CORS de {@code /graphql} (QR-21, D-211, SG-14) para que una web en otro origen, como la pagina publica del comensal,
 * pueda llamarlo. Se acota a la lista que ya existe, {@code seguridad.http.origenes-permitidos}; sin lista, ningun navegador
 * de otro origen pasa. Corre antes que el filtro de entrada, asi que lo que no cumple se rechaza sin autenticar:
 * <ul>
 *   <li>Sin {@code Origin} (apps nativas, pruebas) todo sigue igual y no se agrega ninguna cabecera.</li>
 *   <li>Un {@code Origin} que no esta en la lista, repetido o {@code null}: 403 {@code ORIGEN_NO_PERMITIDO}, sin cabeceras
 *       CORS, tambien en el prevuelo.</li>
 *   <li>El prevuelo de un origen permitido ({@code OPTIONS} con {@code Access-Control-Request-Method}) se contesta aqui con
 *       204, sin sesion: solo {@code POST} y los encabezados {@code Authorization}, {@code Content-Type},
 *       {@code Idempotency-Key} y {@code X-Dispositivo-Id}; otra cosa es 403 {@code PREVUELO_NO_PERMITIDO}. Tiene su propio
 *       tope por IP (429).</li>
 *   <li>Un POST de un origen permitido sigue su camino y su respuesta, tambien la de un rechazo, lleva el origen para que el
 *       navegador la deje leer, con {@code Vary: Origin} y {@code Retry-After} expuesto.</li>
 * </ul>
 * No hay cookies ni credenciales del navegador: el token viaja en {@code Authorization} (SI-05), por eso nunca se manda
 * {@code Access-Control-Allow-Credentials}. Las subidas de WebSocket no se tocan aqui: su origen lo verifica el mismo filtro
 * y las conexiones las limita su propio filtro.
 */
public final class CorsDeGraphqlFilter extends OncePerRequestFilter {

    static final String RUTA = "/graphql";
    private static final String METODO = "POST";
    private static final Set<String> ENCABEZADOS = Set.of("authorization", "content-type", "idempotency-key", "x-dispositivo-id");
    private static final String ENCABEZADOS_PERMITIDOS = "Authorization, Content-Type, Idempotency-Key, X-Dispositivo-Id";
    private static final String VIGENCIA_SEGUNDOS = "600";

    private final Set<String> origenesPermitidos;
    private final LimitadorDeTasa prevuelosPorIp;

    /**
     * Arma el filtro.
     *
     * @param propiedades    de ahi salen los origenes permitidos
     * @param prevuelosPorIp tope de prevuelos por IP (la IP de la conexion, no la de una cabecera reenviada)
     */
    public CorsDeGraphqlFilter(PropiedadesHttp propiedades, LimitadorDeTasa prevuelosPorIp) {
        Objects.requireNonNull(propiedades, "propiedades");
        this.origenesPermitidos = Set.copyOf(propiedades.origenesPermitidos());
        this.prevuelosPorIp = Objects.requireNonNull(prevuelosPorIp, "prevuelosPorIp");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        return !RUTA.equals(peticion.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        var origenes = Collections.list(peticion.getHeaders(HttpHeaders.ORIGIN));
        if (origenes.isEmpty()) {
            cadena.doFilter(peticion, respuesta);
            return;
        }
        var origen = origenes.size() == 1 ? origenes.get(0).strip().toLowerCase(Locale.ROOT) : null;
        if (origen == null || !origenesPermitidos.contains(origen)) {
            rechazar(respuesta, HttpServletResponse.SC_FORBIDDEN, "ORIGEN_NO_PERMITIDO");
            return;
        }
        if (SubidaWebSocket.es(peticion)) {
            cadena.doFilter(peticion, respuesta);
            return;
        }
        if (esPrevuelo(peticion)) {
            responderPrevuelo(peticion, respuesta, origen);
            return;
        }
        respuesta.addHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        respuesta.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origen);
        respuesta.setHeader(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, "Retry-After");
        cadena.doFilter(peticion, respuesta);
    }

    private static boolean esPrevuelo(HttpServletRequest peticion) {
        return "OPTIONS".equals(peticion.getMethod()) && peticion.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD) != null;
    }

    private void responderPrevuelo(HttpServletRequest peticion, HttpServletResponse respuesta, String origen)
            throws IOException {
        var decision = prevuelosPorIp.consumir(peticion.getRemoteAddr());
        if (!decision.permitido()) {
            RechazoPorTasa.escribir(respuesta, decision.reintentarEnSegundos());
            return;
        }
        if (!METODO.equalsIgnoreCase(peticion.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD).strip())
                || !encabezadosPermitidos(peticion)) {
            rechazar(respuesta, HttpServletResponse.SC_FORBIDDEN, "PREVUELO_NO_PERMITIDO");
            return;
        }
        respuesta.setStatus(HttpServletResponse.SC_NO_CONTENT);
        respuesta.addHeader(HttpHeaders.VARY, HttpHeaders.ORIGIN);
        respuesta.addHeader(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD);
        respuesta.addHeader(HttpHeaders.VARY, HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
        respuesta.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, origen);
        respuesta.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS, METODO);
        respuesta.setHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, ENCABEZADOS_PERMITIDOS);
        respuesta.setHeader(HttpHeaders.ACCESS_CONTROL_MAX_AGE, VIGENCIA_SEGUNDOS);
    }

    /** Verdadero si todos los encabezados que pide el navegador estan en la lista corta de la plataforma. */
    private static boolean encabezadosPermitidos(HttpServletRequest peticion) {
        var pedidos = Collections.list(peticion.getHeaders(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS));
        return pedidos.stream().flatMap(valor -> Arrays.stream(valor.split(",")))
                .map(valor -> valor.strip().toLowerCase(Locale.ROOT)).filter(valor -> !valor.isEmpty())
                .collect(Collectors.toSet()).stream().allMatch(ENCABEZADOS::contains);
    }

    private static void rechazar(HttpServletResponse respuesta, int estado, String codigo) throws IOException {
        respuesta.setStatus(estado);
        respuesta.setContentType("application/json");
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.getWriter().write("{\"codigo\":\"" + codigo + "\"}");
    }
}
