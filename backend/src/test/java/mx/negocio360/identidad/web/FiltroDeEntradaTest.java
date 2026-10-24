package mx.negocio360.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
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
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.SesionAutenticada;
import mx.negocio360.identidad.infraestructura.LimiteDeTasaEnMemoria;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * IDN-4: el filtro de entrada de {@code /graphql} (SG-01, SG-09, SG-10, SG-14). Autentica el token si viene, deja la
 * sesion como contexto y permisos, limita la tasa por IP y por dispositivo, y acota el metodo, el tipo y el tamano del
 * cuerpo antes de leerlo. Sin token la peticion sigue como anonima: los resolvedores piden la sesion que necesiten.
 */
class FiltroDeEntradaTest {

    private static final UUID CUENTA = UUID.fromString("0192f39f-3333-7000-8000-00000000f001");
    private static final UUID NEGOCIO = UUID.fromString("0192f39f-3333-7000-8000-00000000f002");
    private static final String TOKEN = "t".repeat(43);
    private static final String DISPOSITIVO = "0192f39f-3333-7000-8000-00000000f003";

    private final IdentidadApi identidad = mock(IdentidadApi.class);
    private final FilterChain cadena = mock(FilterChain.class);
    private final RelojFijo reloj = new RelojFijo();
    private FiltroDeEntrada filtro;

    /** Un reloj que se mueve a mano. */
    private static final class RelojFijo extends Clock {
        Instant ahora = Instant.parse("2026-10-16T16:00:00Z");

        @Override
        public java.time.ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    @BeforeEach
    void preparar() {
        filtro = new FiltroDeEntrada(identidad, new LimiteDeTasaEnMemoria(), reloj,
                new FiltroDeEntrada.Limites(3, 2, 1024));
        when(identidad.autenticar(TOKEN)).thenReturn(Optional.of(new SesionAutenticada(CUENTA, "Ana", NEGOCIO,
                "DUENO", Set.of("negocios.perfil.ver"), reloj.ahora.plusSeconds(1800))));
    }

    private static MockHttpServletRequest peticion(String cuerpo) {
        var p = new MockHttpServletRequest("POST", "/graphql");
        p.setContentType("application/json");
        p.setContent(cuerpo.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        p.setRemoteAddr("10.0.0.1");
        return p;
    }

    private MockHttpServletResponse ejecutar(MockHttpServletRequest p) throws Exception {
        var r = new MockHttpServletResponse();
        filtro.doFilter(p, r, cadena);
        return r;
    }

    private static EntradaGraphQl entradaDe(MockHttpServletRequest p) {
        return (EntradaGraphQl) p.getAttribute(EntradaGraphQl.ATRIBUTO);
    }

    // ---- sesion ----

    @Test
    @DisplayName("SG-01: con un token valido deja la cuenta, el contexto del negocio y los permisos de la sesion")
    void tokenValido_dejaLaSesion() throws Exception {
        var p = peticion("{}");
        p.addHeader("Authorization", "Bearer " + TOKEN);

        var r = ejecutar(p);

        assertThat(r.getStatus()).isEqualTo(200);
        verify(cadena).doFilter(p, r);
        var entrada = entradaDe(p);
        assertThat(entrada.exigirCuenta()).isEqualTo(CUENTA);
        assertThat(entrada.exigirContexto().negocioId()).isEqualTo(NEGOCIO);
        assertThat(entrada.autorizacion().puede("negocios.perfil.ver")).isTrue();
        assertThat(entrada.autorizacion().puede("negocios.perfil.editar")).isFalse();
    }

    @Test
    @DisplayName("sin token la peticion sigue como anonima: lo publico no necesita sesion")
    void sinToken_esAnonima() throws Exception {
        var p = peticion("{}");

        var r = ejecutar(p);

        assertThat(r.getStatus()).isEqualTo(200);
        verify(cadena).doFilter(p, r);
        assertThat(entradaDe(p).autenticada()).isFalse();
        verify(identidad, never()).autenticar(any());
    }

    @Test
    @DisplayName("un token que no sirve es 401 y NO se degrada a anonima; no dice por que")
    void tokenInvalido_es401() throws Exception {
        var p = peticion("{}");
        p.addHeader("Authorization", "Bearer " + "x".repeat(43));
        when(identidad.autenticar("x".repeat(43))).thenReturn(Optional.empty());

        var r = ejecutar(p);

        assertThat(r.getStatus()).isEqualTo(401);
        assertThat(r.getContentAsString()).contains("NO_AUTENTICADO").doesNotContain("xxxx");
        verify(cadena, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("una cabecera Authorization que no es Bearer, o repetida, tambien es 401")
    void authorizationMalFormada_es401() throws Exception {
        var basica = peticion("{}");
        basica.addHeader("Authorization", "Basic abc");
        var repetida = peticion("{}");
        repetida.addHeader("Authorization", "Bearer " + TOKEN);
        repetida.addHeader("Authorization", "Bearer " + TOKEN);
        var vacia = peticion("{}");
        vacia.addHeader("Authorization", "Bearer ");

        assertThat(ejecutar(basica).getStatus()).isEqualTo(401);
        assertThat(ejecutar(repetida).getStatus()).isEqualTo(401);
        assertThat(ejecutar(vacia).getStatus()).isEqualTo(401);
        verify(cadena, never()).doFilter(any(), any());
    }

    // ---- dispositivo ----

    @Test
    @DisplayName("el encabezado X-Dispositivo-Id valido viaja a los resolvedores, con o sin sesion")
    void dispositivoValido() throws Exception {
        var p = peticion("{}");
        p.addHeader("X-Dispositivo-Id", DISPOSITIVO);

        ejecutar(p);

        assertThat(entradaDe(p).dispositivo()).isEqualTo(DISPOSITIVO);
        assertThat(entradaDe(p).autenticada()).isFalse();
    }

    @Test
    @DisplayName("RS-11: un X-Dispositivo-Id que no es UUID, o repetido, es 400 DISPOSITIVO_INVALIDO")
    void dispositivoInvalido_es400() throws Exception {
        var malo = peticion("{}");
        malo.addHeader("X-Dispositivo-Id", "no-es-uuid");
        var repetido = peticion("{}");
        repetido.addHeader("X-Dispositivo-Id", DISPOSITIVO);
        repetido.addHeader("X-Dispositivo-Id", DISPOSITIVO);

        var r1 = ejecutar(malo);
        var r2 = ejecutar(repetido);

        assertThat(r1.getStatus()).isEqualTo(400);
        assertThat(r1.getContentAsString()).contains("DISPOSITIVO_INVALIDO").doesNotContain("no-es-uuid");
        assertThat(r2.getStatus()).isEqualTo(400);
        verify(cadena, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("SG-10: un dispositivo tiene su propio tope de peticiones, aparte del de la IP")
    void topePorDispositivo() throws Exception {
        for (var i = 0; i < 2; i++) {
            var p = peticion("{}");
            p.setRemoteAddr("10.0.0." + (10 + i));
            p.addHeader("X-Dispositivo-Id", DISPOSITIVO);
            assertThat(ejecutar(p).getStatus()).isEqualTo(200);
        }
        var tercera = peticion("{}");
        tercera.setRemoteAddr("10.0.0.99");
        tercera.addHeader("X-Dispositivo-Id", DISPOSITIVO);

        var r = ejecutar(tercera);

        assertThat(r.getStatus()).isEqualTo(429);
        assertThat(r.getHeader("Retry-After")).isNotBlank();
        var otro = peticion("{}");
        otro.setRemoteAddr("10.0.0.98");
        otro.addHeader("X-Dispositivo-Id", UUID.randomUUID().toString());
        assertThat(ejecutar(otro).getStatus()).isEqualTo(200);
    }

    // ---- llave de idempotencia (CT-14, QR-15, D-208) ----

    @Test
    @DisplayName("CT-14: la cabecera Idempotency-Key valida viaja a los resolvedores, con o sin sesion")
    void llaveValida() throws Exception {
        var anonima = peticion("{}");
        anonima.addHeader("Idempotency-Key", "pedido-0192f39f:mesa_5.a");
        var conSesion = peticion("{}");
        conSesion.addHeader("Authorization", "Bearer " + TOKEN);
        conSesion.addHeader("Idempotency-Key", "pedido-0192f39f");

        ejecutar(anonima);
        ejecutar(conSesion);

        assertThat(entradaDe(anonima).llave()).isEqualTo("pedido-0192f39f:mesa_5.a");
        assertThat(entradaDe(anonima).autenticada()).isFalse();
        assertThat(entradaDe(conSesion).llave()).isEqualTo("pedido-0192f39f");
        assertThat(entradaDe(conSesion).exigirCuenta()).isEqualTo(CUENTA);
    }

    @Test
    @DisplayName("sin Idempotency-Key la llave es nula; la operacion que la exige responde LLAVE_REQUERIDA")
    void sinLlave() throws Exception {
        var p = peticion("{}");

        ejecutar(p);

        assertThat(entradaDe(p).llave()).isNull();
    }

    @Test
    @DisplayName("CT-14: una Idempotency-Key corta, larga, con caracteres raros o repetida es 400 LLAVE_INVALIDA y no se repite")
    void llaveInvalida_es400() throws Exception {
        var corta = peticion("{}");
        corta.addHeader("Idempotency-Key", "abc");
        var larga = peticion("{}");
        larga.addHeader("Idempotency-Key", "a".repeat(129));
        var rara = peticion("{}");
        rara.addHeader("Idempotency-Key", "llave con espacios <script>");
        var repetida = peticion("{}");
        repetida.addHeader("Idempotency-Key", "llave-numero-uno");
        repetida.addHeader("Idempotency-Key", "llave-numero-dos");

        var direccion = 20;
        for (var p : new MockHttpServletRequest[] {corta, larga, rara, repetida}) {
            p.setRemoteAddr("10.0.0." + direccion++);
            var r = ejecutar(p);
            assertThat(r.getStatus()).isEqualTo(400);
            assertThat(r.getContentAsString()).contains("LLAVE_INVALIDA").doesNotContain("script").doesNotContain("abc");
        }
        verify(cadena, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("el limite superior de la llave es de 128 caracteres y el inferior de 8")
    void llave_limites() throws Exception {
        var minima = peticion("{}");
        minima.addHeader("Idempotency-Key", "a".repeat(8));
        var maxima = peticion("{}");
        maxima.addHeader("Idempotency-Key", "b".repeat(128));

        assertThat(ejecutar(minima).getStatus()).isEqualTo(200);
        assertThat(ejecutar(maxima).getStatus()).isEqualTo(200);
    }

    // ---- tasa por IP ----

    @Test
    @DisplayName("SG-10: una IP tiene un tope de peticiones por minuto; pasado el minuto vuelve a poder")
    void topePorIp() throws Exception {
        for (var i = 0; i < 3; i++) {
            assertThat(ejecutar(peticion("{}")).getStatus()).isEqualTo(200);
        }

        var cuarta = ejecutar(peticion("{}"));
        assertThat(cuarta.getStatus()).isEqualTo(429);
        assertThat(cuarta.getHeader("Retry-After")).isEqualTo("60");
        assertThat(cuarta.getContentAsString()).contains("DEMASIADAS_SOLICITUDES");

        reloj.ahora = reloj.ahora.plus(Duration.ofSeconds(61));
        assertThat(ejecutar(peticion("{}")).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("la IP es la de la conexion: un X-Forwarded-For no cambia de cupo")
    void ipNoSeTomaDeEncabezados() throws Exception {
        for (var i = 0; i < 3; i++) {
            var p = peticion("{}");
            p.addHeader("X-Forwarded-For", "203.0.113." + i);
            assertThat(ejecutar(p).getStatus()).isEqualTo(200);
        }
        var cuarta = peticion("{}");
        cuarta.addHeader("X-Forwarded-For", "203.0.113.200");

        assertThat(ejecutar(cuarta).getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("una peticion rechazada por tasa no llega a autenticar: no gasta trabajo de sesion")
    void tasaAntesQueSesion() throws Exception {
        for (var i = 0; i < 3; i++) {
            ejecutar(peticion("{}"));
        }
        var p = peticion("{}");
        p.addHeader("Authorization", "Bearer " + TOKEN);

        ejecutar(p);

        verify(identidad, never()).autenticar(TOKEN);
    }

    // ---- HTTP estricto ----

    @Test
    @DisplayName("SG-14: solo POST; GET y el resto son 405 con Allow")
    void soloPost() throws Exception {
        var get = new MockHttpServletRequest("GET", "/graphql");
        get.setRemoteAddr("10.0.0.5");

        var r = ejecutar(get);

        assertThat(r.getStatus()).isEqualTo(405);
        assertThat(r.getHeader("Allow")).isEqualTo("POST");
    }

    @Test
    @DisplayName("SG-14: el cuerpo debe ser JSON; otro tipo es 415")
    void soloJson() throws Exception {
        var p = peticion("{}");
        p.setContentType("text/plain");
        var formulario = peticion("{}");
        formulario.setContentType("application/x-www-form-urlencoded");
        var sinTipo = peticion("{}");
        sinTipo.setContentType(null);

        assertThat(ejecutar(p).getStatus()).isEqualTo(415);
        assertThat(ejecutar(formulario).getStatus()).isEqualTo(415);
        assertThat(ejecutar(sinTipo).getStatus()).isEqualTo(415);
    }

    @Test
    @DisplayName("SG-14: application/json con charset y application/graphql-response+json se aceptan")
    void jsonConParametros() throws Exception {
        var conCharset = peticion("{}");
        conCharset.setContentType("application/json; charset=UTF-8");
        var graphql = peticion("{}");
        graphql.setContentType("application/graphql-response+json");
        graphql.setRemoteAddr("10.0.0.7");

        assertThat(ejecutar(conCharset).getStatus()).isEqualTo(200);
        assertThat(ejecutar(graphql).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("SG-09: un cuerpo mas grande que el tope es 413 sin leerlo; sin longitud conocida es 411")
    void tamanoDelCuerpo() throws Exception {
        var grande = peticion("x".repeat(1025));
        var sinLongitud = peticion("{}");
        sinLongitud.setContent(null);
        sinLongitud.addHeader("Transfer-Encoding", "chunked");
        sinLongitud.setRemoteAddr("10.0.0.6");
        var justo = peticion("x".repeat(1024));
        justo.setRemoteAddr("10.0.0.8");

        assertThat(ejecutar(grande).getStatus()).isEqualTo(413);
        assertThat(ejecutar(sinLongitud).getStatus()).isEqualTo(411);
        assertThat(ejecutar(justo).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("SG-14: toda respuesta del filtro, aceptada o rechazada, sale sin cache y sin adivinar el tipo")
    void cabeceras() throws Exception {
        var aceptada = ejecutar(peticion("{}"));
        var rechazada = ejecutar(peticion("x".repeat(2000)));

        for (var r : new MockHttpServletResponse[] {aceptada, rechazada}) {
            assertThat(r.getHeader("Cache-Control")).isEqualTo("no-store");
            assertThat(r.getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        }
    }

    @Test
    @DisplayName("solo filtra /graphql: las rutas de acceso de Identidad no pasan por aqui")
    void soloGraphql() {
        var auth = new MockHttpServletRequest("POST", "/auth/codigo");
        var graphql = new MockHttpServletRequest("POST", "/graphql");

        assertThat(filtro.shouldNotFilter(auth)).isTrue();
        assertThat(filtro.shouldNotFilter(graphql)).isFalse();
    }

    @Test
    @DisplayName("el cuerpo de un rechazo no repite nada de lo que mando quien llamo")
    void rechazoSinEco() throws Exception {
        var p = peticion("{\"query\":\"secreto-del-cliente\"}");
        p.setContentType("text/plain");

        var r = ejecutar(p);

        assertThat(r.getContentAsString()).doesNotContain("secreto-del-cliente").doesNotContain("text/plain");
    }
}
