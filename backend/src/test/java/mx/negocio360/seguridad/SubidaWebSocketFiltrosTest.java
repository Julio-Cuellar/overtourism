package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import mx.negocio360.compartido.seguridad.LimitadorDeTasa;
import mx.negocio360.compartido.seguridad.LimiteDeConexionesWebSocketFilter;
import mx.negocio360.compartido.seguridad.PeticionGraphqlEstrictaFilter;
import mx.negocio360.compartido.seguridad.PropiedadesHttp;
import mx.negocio360.compartido.seguridad.SubidaWebSocket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * SG-15, el apreton de manos del WebSocket (un GET con {@code Upgrade}). Un navegador no puede mandar Authorization ahi,
 * asi que se deja pasar sin sesion y la autenticacion es el mensaje connection_init; pero el origen se verifica igual que
 * en HTTP, y las conexiones nuevas por IP tienen su propio tope.
 */
class SubidaWebSocketFiltrosTest {

    private static final String ORIGEN_BUENO = "https://panel.ejemplo.test";

    private final AtomicBoolean cadenaInvocada = new AtomicBoolean();
    private final PeticionGraphqlEstrictaFilter estricto = new PeticionGraphqlEstrictaFilter(
            new PropiedadesHttp(65_536, List.of(ORIGEN_BUENO)));
    private final RelojManual reloj = new RelojManual(Instant.parse("2026-10-16T12:00:00Z"));

    private static MockHttpServletRequest subida(String metodo, String upgrade, String conexion) {
        var peticion = new MockHttpServletRequest(metodo, "/graphql");
        peticion.setServletPath("/graphql");
        if (upgrade != null) {
            peticion.addHeader("Upgrade", upgrade);
        }
        if (conexion != null) {
            peticion.addHeader("Connection", conexion);
        }
        return peticion;
    }

    private MockHttpServletResponse estricto(MockHttpServletRequest peticion) throws Exception {
        var respuesta = new MockHttpServletResponse();
        estricto.doFilter(peticion, respuesta, (rq, rs) -> cadenaInvocada.set(true));
        return respuesta;
    }

    // --- Reconocer una subida ---

    @Test
    @DisplayName("una subida es un GET con Upgrade: websocket y Connection que incluya upgrade, sin importar mayusculas")
    void reconoceLaSubida() {
        assertThat(SubidaWebSocket.es(subida("GET", "websocket", "Upgrade"))).isTrue();
        assertThat(SubidaWebSocket.es(subida("GET", "WebSocket", "keep-alive, Upgrade"))).isTrue();
        assertThat(SubidaWebSocket.es(subida("GET", "websocket", "upgrade"))).isTrue();
        assertThat(SubidaWebSocket.es(subida("GET", "websocket", null))).isFalse();
        assertThat(SubidaWebSocket.es(subida("GET", null, "Upgrade"))).isFalse();
        assertThat(SubidaWebSocket.es(subida("GET", "h2c", "Upgrade"))).isFalse();
        assertThat(SubidaWebSocket.es(subida("POST", "websocket", "Upgrade"))).isFalse();
        assertThat(SubidaWebSocket.es(subida("GET", "websocket", "close"))).isFalse();
    }

    // --- La puerta HTTP deja pasar la subida (y solo la subida) ---

    @Test
    @DisplayName("la puerta HTTP deja pasar un GET de subida, sin exigir tipo ni cuerpo")
    void puerta_dejaPasarLaSubida() throws Exception {
        var respuesta = estricto(subida("GET", "websocket", "Upgrade"));

        assertThat(respuesta.getStatus()).isEqualTo(200);
        assertThat(cadenaInvocada).isTrue();
    }

    @Test
    @DisplayName("un GET con Upgrade incompleto o de otro protocolo sigue siendo 405")
    void puerta_otrosGetSiguenSiendo405() throws Exception {
        assertThat(estricto(subida("GET", "websocket", null)).getStatus()).isEqualTo(405);
        assertThat(estricto(subida("GET", "h2c", "Upgrade")).getStatus()).isEqualTo(405);
        assertThat(estricto(subida("GET", null, null)).getStatus()).isEqualTo(405);
        assertThat(cadenaInvocada).isFalse();
    }

    @Test
    @DisplayName("la subida verifica el origen como en HTTP: uno ajeno es 403, uno permitido o ninguno pasa")
    void puerta_verificaElOrigenEnLaSubida() throws Exception {
        var ajena = subida("GET", "websocket", "Upgrade");
        ajena.addHeader("Origin", "https://malo.test");
        assertThat(estricto(ajena).getStatus()).isEqualTo(403);
        assertThat(cadenaInvocada).isFalse();

        var buena = subida("GET", "websocket", "Upgrade");
        buena.addHeader("Origin", ORIGEN_BUENO);
        assertThat(estricto(buena).getStatus()).isEqualTo(200);
        assertThat(cadenaInvocada).isTrue();
    }

    @Test
    @DisplayName("un POST con Upgrade sigue las reglas de un POST: el cuerpo debe ser un objeto JSON")
    void puerta_postConUpgradeNoEsSubida() throws Exception {
        var peticion = subida("POST", "websocket", "Upgrade");
        peticion.setContentType("application/json");
        peticion.setContent("[]".getBytes(StandardCharsets.UTF_8));

        assertThat(estricto(peticion).getStatus()).isEqualTo(400);
    }

    // --- Tope de conexiones nuevas por IP ---

    private LimiteDeConexionesWebSocketFilter conexiones() {
        return new LimiteDeConexionesWebSocketFilter(new LimitadorDeTasa(30, Duration.ofMinutes(1), 1_000, reloj));
    }

    private MockHttpServletResponse pasar(LimiteDeConexionesWebSocketFilter filtro, MockHttpServletRequest peticion)
            throws Exception {
        var respuesta = new MockHttpServletResponse();
        filtro.doFilter(peticion, respuesta, (rq, rs) -> cadenaInvocada.set(true));
        return respuesta;
    }

    private static MockHttpServletRequest desde(String ip, MockHttpServletRequest peticion) {
        peticion.setRemoteAddr(ip);
        return peticion;
    }

    @Test
    @DisplayName("30 conexiones nuevas por minuto por IP pasan; la 31 recibe 429 con Retry-After")
    void conexiones_treintaYLaSiguienteSeNiega() throws Exception {
        var filtro = conexiones();
        for (var i = 0; i < 30; i++) {
            assertThat(pasar(filtro, desde("10.0.0.1", subida("GET", "websocket", "Upgrade"))).getStatus())
                    .as("conexion %d", i + 1).isEqualTo(200);
        }
        cadenaInvocada.set(false);

        var negada = pasar(filtro, desde("10.0.0.1", subida("GET", "websocket", "Upgrade")));

        assertThat(negada.getStatus()).isEqualTo(429);
        assertThat(negada.getContentAsString()).isEqualTo("{\"codigo\":\"DEMASIADAS_SOLICITUDES\"}");
        assertThat(negada.getHeader("Retry-After")).isEqualTo("2");
        assertThat(cadenaInvocada).isFalse();
        assertThat(pasar(filtro, desde("10.0.0.2", subida("GET", "websocket", "Upgrade"))).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("lo que no es una subida no gasta fichas de conexiones")
    void conexiones_soloLasSubidasCuentan() throws Exception {
        var filtro = conexiones();
        for (var i = 0; i < 100; i++) {
            pasar(filtro, desde("10.0.0.3", subida("POST", null, null)));
            pasar(filtro, desde("10.0.0.3", subida("GET", null, null)));
        }

        assertThat(pasar(filtro, desde("10.0.0.3", subida("GET", "websocket", "Upgrade"))).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("otra ruta no se toca aunque sea una subida")
    void conexiones_otraRutaNoSeToca() throws Exception {
        var filtro = conexiones();
        for (var i = 0; i < 100; i++) {
            var otra = new MockHttpServletRequest("GET", "/otra");
            otra.setServletPath("/otra");
            otra.addHeader("Upgrade", "websocket");
            otra.addHeader("Connection", "Upgrade");
            assertThat(pasar(filtro, desde("10.0.0.4", otra)).getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("las conexiones se recuperan con el tiempo: una ficha cada dos segundos")
    void conexiones_seRecuperan() throws Exception {
        var filtro = conexiones();
        for (var i = 0; i < 30; i++) {
            pasar(filtro, desde("10.0.0.5", subida("GET", "websocket", "Upgrade")));
        }
        assertThat(pasar(filtro, desde("10.0.0.5", subida("GET", "websocket", "Upgrade"))).getStatus()).isEqualTo(429);

        reloj.avanzar(Duration.ofSeconds(2));

        assertThat(pasar(filtro, desde("10.0.0.5", subida("GET", "websocket", "Upgrade"))).getStatus()).isEqualTo(200);
    }
}
