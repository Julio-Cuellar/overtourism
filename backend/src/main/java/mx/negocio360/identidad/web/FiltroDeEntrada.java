package mx.negocio360.identidad.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.seguridad.LimitadorDeTasa;
import mx.negocio360.compartido.seguridad.SubidaWebSocket;
import mx.negocio360.compartido.seguridad.TokenOpaco;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.aplicacion.puertos.LimiteDeTasa;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * El filtro de entrada de {@code /graphql} (IDN-4, SG-01, SG-09, SG-10, SG-14). Antes de que nada lea el cuerpo:
 * <ol>
 *   <li>Solo {@code POST} con cuerpo JSON y de tamano conocido y acotado (405, 415, 411 y 413); un GET no puede ser una
 *       consulta, asi que no hay consultas por enlace.</li>
 *   <li>Un tope de peticiones por minuto por IP, la de la conexion: no se confia en encabezados reenviados (429).</li>
 *   <li>El encabezado {@code X-Dispositivo-Id}, si viene, debe ser un UUID y tiene su propio tope por minuto (400, 429).</li>
 *   <li>Si viene {@code Authorization: Bearer}, la sesion se autentica con Identidad; un token que no sirve es 401 y NUNCA
 *       se degrada a anonimo. Sin token la peticion sigue como anonima: la superficie publica no pide sesion y los
 *       resolvedores del panel exigen la suya.</li>
 * </ol>
 * La sesion queda como {@link EntradaGraphQl} en la peticion: de ahi sale el negocio, el actor y los permisos (SG-02). Todas
 * las respuestas, tambien los rechazos, salen sin cache y sin adivinar el tipo; el cuerpo de un rechazo es un codigo y un
 * texto fijos, sin repetir nada de lo que mando quien llamo.
 */
public final class FiltroDeEntrada extends OncePerRequestFilter {

    /** Topes del filtro: peticiones por minuto por IP y por dispositivo, y bytes del cuerpo. */
    public record Limites(int porIpPorMinuto, int porDispositivoPorMinuto, int maximoBytesDelCuerpo) {
        public Limites {
            if (porIpPorMinuto <= 0 || porDispositivoPorMinuto <= 0 || maximoBytesDelCuerpo <= 0) {
                throw new IllegalArgumentException("Los topes deben ser positivos");
            }
        }
    }

    /**
     * Controles que se suman a los de IDN-4 al unirlo con la puerta de la plataforma; cada uno es opcional (nulo = sin ese
     * control). {@code fallosPorIp} cuenta solo los tokens que no sirven y se consulta antes de autenticar, para que adivinar
     * tokens tenga un tope sin costo para Identidad; {@code peticionesPorCuenta} limita lo que puede pedir una cuenta.
     */
    public record Controles(LimitadorDeTasa fallosPorIp, LimitadorDeTasa peticionesPorCuenta) {
        static final Controles NINGUNO = new Controles(null, null);
    }

    private static final Pattern BEARER = Pattern.compile("(?i)^Bearer\\s+(\\S+)$");
    private static final Pattern TIPO_JSON = Pattern.compile("(?i)^application/(json|graphql-response\\+json)\\s*(;.*)?$");
    private static final Duration VENTANA = Duration.ofMinutes(1);
    private static final int LARGO_DE_UUID = 36;
    /** Un token valido mide 43 caracteres; uno mas largo ni se consulta (el WebSocket y el filtro de Bearer usan el mismo tope). */
    private static final int MAXIMO_DEL_TOKEN = 256;

    private final IdentidadApi identidad;
    private final LimiteDeTasa tasa;
    private final Clock reloj;
    private final Limites limites;
    private final Controles controles;

    public FiltroDeEntrada(IdentidadApi identidad, LimiteDeTasa tasa, Clock reloj, Limites limites) {
        this(identidad, tasa, reloj, limites, Controles.NINGUNO);
    }

    public FiltroDeEntrada(IdentidadApi identidad, LimiteDeTasa tasa, Clock reloj, Limites limites,
                           Controles controles) {
        this.identidad = Objects.requireNonNull(identidad, "identidad");
        this.tasa = Objects.requireNonNull(tasa, "tasa");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.limites = Objects.requireNonNull(limites, "limites");
        this.controles = Objects.requireNonNull(controles, "controles");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        return !"/graphql".equals(peticion.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        // El apreton de manos de un WebSocket no es una consulta: no lleva cuerpo ni sesion (el navegador no puede mandar
        // Authorization ahi). Sigue a los controles de la subida (origen, conexiones por IP); la sesion se pide en connection_init.
        if (SubidaWebSocket.es(peticion)) {
            cadena.doFilter(peticion, respuesta);
            return;
        }
        respuesta.setHeader("Cache-Control", "no-store");
        respuesta.setHeader("X-Content-Type-Options", "nosniff");

        // SG-10: la IP se cuenta primero, para que una avalancha de peticiones mal formadas tambien tenga tope.
        if (!tasa.intentar(TokenOpaco.hash("entrada:ip:" + peticion.getRemoteAddr()), limites.porIpPorMinuto(),
                VENTANA, reloj.instant())) {
            rechazar(respuesta, 429, "DEMASIADAS_SOLICITUDES", "Hiciste demasiadas solicitudes. Inténtalo más tarde.");
            return;
        }
        // SG-14: HTTP estricto.
        if (!"POST".equals(peticion.getMethod())) {
            respuesta.setHeader("Allow", "POST");
            rechazar(respuesta, 405, "METODO_NO_PERMITIDO", "Solo se acepta POST.");
            return;
        }
        var tipo = peticion.getContentType();
        if (tipo == null || !TIPO_JSON.matcher(tipo).matches()) {
            rechazar(respuesta, 415, "TIPO_NO_PERMITIDO", "El cuerpo debe ser JSON.");
            return;
        }
        // SG-09: el tamano se conoce antes de leer. Sin longitud (cuerpo en trozos) no se acepta.
        var largo = peticion.getContentLengthLong();
        if (largo < 0) {
            rechazar(respuesta, 411, "LONGITUD_REQUERIDA", "Falta la longitud del cuerpo.");
            return;
        }
        if (largo > limites.maximoBytesDelCuerpo()) {
            rechazar(respuesta, 413, "CUERPO_DEMASIADO_GRANDE", "El cuerpo es demasiado grande.");
            return;
        }

        var dispositivo = dispositivo(peticion);
        if (!dispositivo.valido()) {
            rechazar(respuesta, 400, "DISPOSITIVO_INVALIDO", "El identificador del dispositivo no es válido.");
            return;
        }
        var id = dispositivo.id();
        if (id != null && !tasa.intentar(TokenOpaco.hash("entrada:dispositivo:" + id), limites.porDispositivoPorMinuto(),
                VENTANA, reloj.instant())) {
            rechazar(respuesta, 429, "DEMASIADAS_SOLICITUDES", "Hiciste demasiadas solicitudes. Inténtalo más tarde.");
            return;
        }

        var encabezados = Collections.list(peticion.getHeaders("Authorization"));
        if (encabezados.isEmpty()) {
            peticion.setAttribute(EntradaGraphQl.ATRIBUTO, EntradaGraphQl.anonima(id));
            cadena.doFilter(peticion, respuesta);
            return;
        }
        // SG-10: una IP que ya agoto sus fallos ni siquiera llega a consultar a Identidad.
        var ip = peticion.getRemoteAddr();
        if (controles.fallosPorIp() != null && !controles.fallosPorIp().probar(ip).permitido()) {
            rechazar(respuesta, 429, "DEMASIADAS_SOLICITUDES", "Hiciste demasiadas solicitudes. Inténtalo más tarde.");
            return;
        }
        var coincidencia = encabezados.size() == 1 ? BEARER.matcher(encabezados.get(0)) : null;
        var sesion = coincidencia != null && coincidencia.matches() && coincidencia.group(1).length() <= MAXIMO_DEL_TOKEN
                ? identidad.autenticar(coincidencia.group(1))
                : java.util.Optional.<mx.negocio360.identidad.SesionAutenticada>empty();
        if (sesion.isEmpty()) {
            if (controles.fallosPorIp() != null) {
                controles.fallosPorIp().consumir(ip);
            }
            rechazar(respuesta, 401, "NO_AUTENTICADO", "Inicia sesión para continuar.");
            return;
        }
        var s = sesion.get();
        if (controles.peticionesPorCuenta() != null
                && !controles.peticionesPorCuenta().consumir(s.cuentaId().toString()).permitido()) {
            rechazar(respuesta, 429, "DEMASIADAS_SOLICITUDES", "Hiciste demasiadas solicitudes. Inténtalo más tarde.");
            return;
        }
        peticion.setAttribute(EntradaGraphQl.ATRIBUTO, EntradaGraphQl.de(s.cuentaId(), s.contextoNegocio().orElse(null),
                s.autorizacion(), id));
        cadena.doFilter(peticion, respuesta);
    }

    /** El encabezado de dispositivo ya leido: {@code valido} es falso si vino mal; {@code id} es nulo si no vino. */
    private record Dispositivo(boolean valido, String id) {
    }

    /** Lee {@code X-Dispositivo-Id}: ausente es valido y sin id; uno solo, UUID de 36 caracteres, se guarda en minusculas. */
    private static Dispositivo dispositivo(HttpServletRequest peticion) {
        var valores = Collections.list(peticion.getHeaders("X-Dispositivo-Id"));
        if (valores.isEmpty()) {
            return new Dispositivo(true, null);
        }
        if (valores.size() > 1 || valores.get(0).length() != LARGO_DE_UUID) {
            return new Dispositivo(false, null);
        }
        try {
            return new Dispositivo(true, UUID.fromString(valores.get(0)).toString().toLowerCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return new Dispositivo(false, null);
        }
    }

    private static void rechazar(HttpServletResponse respuesta, int estado, String codigo, String mensaje)
            throws IOException {
        if (estado == 429) {
            respuesta.setHeader("Retry-After", "60");
        }
        if (estado == 401) {
            respuesta.setHeader("WWW-Authenticate", "Bearer");
        }
        respuesta.setStatus(estado);
        respuesta.setContentType("application/json");
        respuesta.setCharacterEncoding(StandardCharsets.UTF_8.name());
        respuesta.getWriter().write("{\"errors\":[{\"message\":\"" + mensaje + "\",\"extensions\":{\"codigo\":\""
                + codigo + "\"}}]}");
    }
}
