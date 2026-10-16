package mx.negocio360.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.seguridad.LimitadorDeTasa;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.SesionAutenticada;
import mx.negocio360.identidad.infraestructura.LimiteDeTasaEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Lo que el filtro de entrada de {@code /graphql} suma al de IDN-4 al unirlo con las capas de la puerta de la plataforma
 * (SG-10, SG-15): el apreton de manos de un WebSocket no es una consulta y pasa a los controles de la subida; adivinar
 * tokens tiene un tope por IP que se aplica antes de preguntarle a Identidad; y cada cuenta tiene un tope por minuto.
 */
class FiltroDeEntradaControlesTest {

    private static final UUID CUENTA = UUID.fromString("0192f39f-3333-7000-8000-00000000f001");
    private static final UUID OTRA_CUENTA = UUID.fromString("0192f39f-3333-7000-8000-00000000f009");
    private static final UUID NEGOCIO = UUID.fromString("0192f39f-3333-7000-8000-00000000f002");
    private static final String TOKEN = "t".repeat(43);
    private static final String TOKEN_DE_OTRA = "o".repeat(43);

    private final IdentidadApi identidad = mock(IdentidadApi.class);
    private final FilterChain cadena = mock(FilterChain.class);
    private final Clock reloj = Clock.fixed(Instant.parse("2026-10-16T16:00:00Z"), ZoneOffset.UTC);
    private final LimitadorDeTasa fallosPorIp = new LimitadorDeTasa(2, Duration.ofMinutes(1), 100, reloj);
    private final LimitadorDeTasa peticionesPorCuenta = new LimitadorDeTasa(3, Duration.ofMinutes(1), 100, reloj);
    private FiltroDeEntrada filtro;

    @BeforeEach
    void preparar() {
        filtro = new FiltroDeEntrada(identidad, new LimiteDeTasaEnMemoria(), reloj,
                new FiltroDeEntrada.Limites(1000, 1000, 65536),
                new FiltroDeEntrada.Controles(fallosPorIp, peticionesPorCuenta));
        when(identidad.autenticar(TOKEN)).thenReturn(Optional.of(sesion(CUENTA)));
        when(identidad.autenticar(TOKEN_DE_OTRA)).thenReturn(Optional.of(sesion(OTRA_CUENTA)));
    }

    private static SesionAutenticada sesion(UUID cuenta) {
        return new SesionAutenticada(cuenta, "Ana", NEGOCIO, "DUENO", Set.of("negocios.perfil.ver"),
                Instant.parse("2026-10-16T17:00:00Z"));
    }

    private static MockHttpServletRequest consulta(String ip, String token) {
        var p = new MockHttpServletRequest("POST", "/graphql");
        p.setContentType("application/json");
        p.setContent("{}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        p.setRemoteAddr(ip);
        if (token != null) {
            p.addHeader("Authorization", "Bearer " + token);
        }
        return p;
    }

    private static MockHttpServletRequest apreton(String ip) {
        var p = new MockHttpServletRequest("GET", "/graphql");
        p.addHeader("Connection", "Upgrade");
        p.addHeader("Upgrade", "websocket");
        p.setRemoteAddr(ip);
        return p;
    }

    private MockHttpServletResponse ejecutar(MockHttpServletRequest p) throws Exception {
        var r = new MockHttpServletResponse();
        filtro.doFilter(p, r, cadena);
        return r;
    }

    // ---- WebSocket ----

    @Test
    @DisplayName("SG-15: el apreton de manos de un WebSocket pasa a los controles de la subida, no es un 405")
    void apreton_pasa() throws Exception {
        var p = apreton("10.0.0.1");

        var r = ejecutar(p);

        assertThat(r.getStatus()).isEqualTo(200);
        verify(cadena).doFilter(p, r);
        verify(identidad, never()).autenticar(any());
        assertThat(p.getAttribute(EntradaGraphQl.ATRIBUTO)).isNull();
    }

    @Test
    @DisplayName("un GET que no es un apreton de manos de WebSocket sigue siendo 405: no hay consultas por enlace")
    void getComun_siguiendoSiendo405() throws Exception {
        var p = new MockHttpServletRequest("GET", "/graphql");
        p.setRemoteAddr("10.0.0.1");

        var r = ejecutar(p);

        assertThat(r.getStatus()).isEqualTo(405);
        verify(cadena, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("un POST con cabeceras de subida no se confunde con un apreton de manos: se trata como consulta")
    void postConCabecerasDeSubida_esConsulta() throws Exception {
        var p = consulta("10.0.0.1", null);
        p.addHeader("Connection", "Upgrade");
        p.addHeader("Upgrade", "websocket");
        p.setContentType("text/plain");

        assertThat(ejecutar(p).getStatus()).isEqualTo(415);
    }

    // ---- fallos de autenticacion por IP ----

    @Test
    @DisplayName("SG-10: un token falso cuenta como fallo de la IP, y agotado el tope ni se le pregunta a Identidad")
    void tokensFalsos_agotanLaIp() throws Exception {
        when(identidad.autenticar("x".repeat(43))).thenReturn(Optional.empty());
        assertThat(ejecutar(consulta("10.0.0.7", "x".repeat(43))).getStatus()).isEqualTo(401);
        assertThat(ejecutar(consulta("10.0.0.7", "x".repeat(43))).getStatus()).isEqualTo(401);
        verify(identidad, times(2)).autenticar("x".repeat(43));

        var agotada = ejecutar(consulta("10.0.0.7", "x".repeat(43)));

        assertThat(agotada.getStatus()).isEqualTo(429);
        verify(identidad, times(2)).autenticar("x".repeat(43));
    }

    @Test
    @DisplayName("un token de mas de 256 caracteres no llega a Identidad: es un fallo de la IP, como en el WebSocket")
    void tokenDemasiadoLargo_noSeConsulta() throws Exception {
        var largo = "t".repeat(257);

        var r = ejecutar(consulta("10.0.0.7", largo));

        assertThat(r.getStatus()).isEqualTo(401);
        verify(identidad, never()).autenticar(largo);
        assertThat(fallosPorIp.claves()).isEqualTo(1);
    }

    @Test
    @DisplayName("un 401 dice como autenticarse: lleva WWW-Authenticate: Bearer, sin distinguir token falso de revocado")
    void respuesta401_llevaWwwAuthenticate() throws Exception {
        when(identidad.autenticar("x".repeat(43))).thenReturn(Optional.empty());

        var r = ejecutar(consulta("10.0.0.7", "x".repeat(43)));

        assertThat(r.getStatus()).isEqualTo(401);
        assertThat(r.getHeader("WWW-Authenticate")).isEqualTo("Bearer");
    }

    @Test
    @DisplayName("una IP agotada tampoco autentica un token bueno: sigue el 429 hasta que pase la ventana")
    void ipAgotada_niUnTokenBuenoPasa() throws Exception {
        when(identidad.autenticar("x".repeat(43))).thenReturn(Optional.empty());
        ejecutar(consulta("10.0.0.7", "x".repeat(43)));
        ejecutar(consulta("10.0.0.7", "x".repeat(43)));

        var r = ejecutar(consulta("10.0.0.7", TOKEN));

        assertThat(r.getStatus()).isEqualTo(429);
        verify(identidad, never()).autenticar(TOKEN);
    }

    @Test
    @DisplayName("los fallos son por IP: otra IP no se ve afectada")
    void fallos_sonPorIp() throws Exception {
        when(identidad.autenticar("x".repeat(43))).thenReturn(Optional.empty());
        ejecutar(consulta("10.0.0.7", "x".repeat(43)));
        ejecutar(consulta("10.0.0.7", "x".repeat(43)));

        assertThat(ejecutar(consulta("10.0.0.8", TOKEN)).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("lo que no es un fallo no gasta el tope: peticiones anonimas y con token bueno no agotan la IP")
    void exitosYAnonimas_noGastanElTope() throws Exception {
        for (var i = 0; i < 10; i++) {
            // tres con token bueno (el tope por cuenta es 3) y el resto anonimas
            assertThat(ejecutar(consulta("10.0.0.9", i < 3 ? TOKEN_DE_OTRA : null)).getStatus()).isEqualTo(200);
        }
        when(identidad.autenticar("x".repeat(43))).thenReturn(Optional.empty());
        assertThat(ejecutar(consulta("10.0.0.9", "x".repeat(43))).getStatus()).isEqualTo(401);
    }

    // ---- tasa por cuenta ----

    @Test
    @DisplayName("SG-10: una cuenta tiene un tope de peticiones por minuto; la siguiente recibe 429 y no llega a GraphQL")
    void tasaPorCuenta() throws Exception {
        for (var i = 0; i < 3; i++) {
            assertThat(ejecutar(consulta("10.0.0.1", TOKEN)).getStatus()).isEqualTo(200);
        }
        var p = consulta("10.0.0.1", TOKEN);

        var r = ejecutar(p);

        assertThat(r.getStatus()).isEqualTo(429);
        assertThat(r.getHeader("Retry-After")).isNotNull();
        verify(cadena, times(3)).doFilter(any(), any());
    }

    @Test
    @DisplayName("la tasa es por cuenta y no por IP ni por token: otra cuenta desde la misma IP sigue pasando")
    void tasa_esPorCuenta() throws Exception {
        for (var i = 0; i < 4; i++) {
            ejecutar(consulta("10.0.0.1", TOKEN));
        }

        assertThat(ejecutar(consulta("10.0.0.1", TOKEN_DE_OTRA)).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("el rechazo por tasa de cuenta no es un fallo de autenticacion: no gasta el tope de la IP")
    void tasaDeCuenta_noEsFallo() throws Exception {
        for (var i = 0; i < 8; i++) {
            ejecutar(consulta("10.0.0.1", TOKEN));
        }

        assertThat(fallosPorIp.probar("10.0.0.1").permitido()).isTrue();
        assertThat(fallosPorIp.claves()).isZero();
    }

    @Test
    @DisplayName("las peticiones anonimas no tienen tope por cuenta: su limite es el de la IP y el del dispositivo")
    void anonimas_sinTopeDeCuenta() throws Exception {
        for (var i = 0; i < 20; i++) {
            assertThat(ejecutar(consulta("10.0.0.1", null)).getStatus()).isEqualTo(200);
        }
        assertThat(peticionesPorCuenta.claves()).isZero();
    }
}
