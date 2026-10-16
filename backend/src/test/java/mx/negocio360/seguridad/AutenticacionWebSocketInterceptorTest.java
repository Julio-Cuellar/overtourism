package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import graphql.ExecutionResultImpl;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import mx.negocio360.compartido.graphql.AutenticacionWebSocketInterceptor;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.seguridad.Autenticador;
import mx.negocio360.compartido.seguridad.LimitadorDeTasa;
import mx.negocio360.compartido.seguridad.SesionDePeticion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.server.WebSocketGraphQlRequest;
import org.springframework.graphql.support.DefaultExecutionGraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import reactor.core.publisher.Mono;

/**
 * SG-15: la autenticacion de una conexion WebSocket es el mensaje connection_init (un navegador no puede mandar
 * cabeceras en el apreton de manos). Falla cerrado: cualquier problema es un error, que el manejador convierte en el cierre
 * 4401. Cada operacion nueva vuelve a validar el token, asi que una sesion revocada se nota en la siguiente.
 */
class AutenticacionWebSocketInterceptorTest {

    private static final java.util.UUID NEGOCIO = SesionDePrueba.NEGOCIO;

    private final RelojManual reloj = new RelojManual(Instant.parse("2026-10-16T12:00:00Z"));
    private final AtomicInteger consultas = new AtomicInteger();
    private final AtomicReference<String> sesionActual = new AtomicReference<>("token-bueno");
    private final LimitadorDeTasa fallosPorIp = new LimitadorDeTasa(3, Duration.ofMinutes(1), 100, reloj);
    private final LimitadorDeTasa peticionesPorCuenta = new LimitadorDeTasa(2, Duration.ofMinutes(1), 100, reloj);

    private final Autenticador autenticador = token -> {
        consultas.incrementAndGet();
        if (!token.equals(sesionActual.get())) {
            return Optional.empty();
        }
        return Optional.of(new SesionDePeticion(SesionDePrueba.CUENTA.toString(), NEGOCIO, Set.of("a.b.c")));
    };
    private final AutenticacionWebSocketInterceptor interceptor = new AutenticacionWebSocketInterceptor(autenticador,
            fallosPorIp, peticionesPorCuenta);

    private static Map<String, Object> carga(String clave, Object valor) {
        var carga = new HashMap<String, Object>();
        carga.put(clave, valor);
        return carga;
    }

    private Mono<Object> iniciar(SesionWebSocketFalsa sesion, Map<String, Object> carga) {
        return interceptor.handleConnectionInitialization(sesion, carga);
    }

    // --- connection_init ---

    @Test
    @DisplayName("un token bueno en Authorization abre la conexion: no hay error y se guarda el token para validarlo despues")
    void initConTokenBueno_abre() {
        var sesion = new SesionWebSocketFalsa("10.0.0.1");

        iniciar(sesion, carga("Authorization", "Bearer token-bueno")).block();

        assertThat(sesion.getAttributes()).containsEntry(AutenticacionWebSocketInterceptor.CLAVE_DEL_TOKEN, "token-bueno");
    }

    @Test
    @DisplayName("la clave Authorization no distingue mayusculas ni el esquema Bearer")
    void initSinImportarMayusculas() {
        var sesion = new SesionWebSocketFalsa("10.0.0.1");

        iniciar(sesion, carga("authorization", "bearer token-bueno")).block();

        assertThat(sesion.getAttributes()).containsKey(AutenticacionWebSocketInterceptor.CLAVE_DEL_TOKEN);
    }

    @ParameterizedTest(name = "[{0}]")
    @ValueSource(strings = {"Basic abc", "Bearer", "Bearer   ", "Bearer a b", "Token token-bueno", "Bearertoken-bueno",
            "", "Bearer token-falso"})
    @DisplayName("una carga mal formada o con un token que Identidad no reconoce es un error, y no guarda nada")
    void initInvalido_esError(String valor) {
        var sesion = new SesionWebSocketFalsa("10.0.0.1");

        assertThatThrownBy(() -> iniciar(sesion, carga("Authorization", valor)).block())
                .isInstanceOf(RuntimeException.class);

        assertThat(sesion.getAttributes()).doesNotContainKey(AutenticacionWebSocketInterceptor.CLAVE_DEL_TOKEN);
    }

    @Test
    @DisplayName("sin Authorization, con un valor que no es texto o con un token enorme, es un error sin consultar a Identidad")
    void initSinTokenOEnorme_esErrorSinConsultar() {
        var sesion = new SesionWebSocketFalsa("10.0.0.1");

        assertThatThrownBy(() -> iniciar(sesion, Map.of()).block()).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> iniciar(sesion, carga("Authorization", 12345)).block())
                .isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> iniciar(sesion, carga("Authorization", "Bearer " + "x".repeat(300))).block())
                .isInstanceOf(RuntimeException.class);

        assertThat(consultas).hasValue(0);
    }

    @Test
    @DisplayName("una sesion valida sin negocio activo SI puede conectarse (altaDeNegocio existe para quien aun no tiene negocio)")
    void initSinNegocio_seAcepta() {
        var sinNegocio = new AutenticacionWebSocketInterceptor(
                token -> Optional.of(new SesionDePeticion(SesionDePrueba.CUENTA.toString(), null, Set.of())), fallosPorIp,
                peticionesPorCuenta);
        var sesion = new SesionWebSocketFalsa("10.0.0.1");

        sinNegocio.handleConnectionInitialization(sesion, carga("Authorization", "Bearer token-bueno")).block();

        assertThat(sesion.getAttributes()).containsKey(AutenticacionWebSocketInterceptor.CLAVE_DEL_TOKEN);
    }

    @Test
    @DisplayName("sin negocio activo la operacion lleva una entrada sin contexto: lo que pide negocio lo niega la puerta")
    void operacionSinNegocio_llevaUnaEntradaSinContexto() {
        var sinNegocio = new AutenticacionWebSocketInterceptor(
                token -> Optional.of(new SesionDePeticion(SesionDePrueba.CUENTA.toString(), null, Set.of("a.b.c"))),
                fallosPorIp, peticionesPorCuenta);
        var sesion = new SesionWebSocketFalsa("10.0.0.1");
        sinNegocio.handleConnectionInitialization(sesion, carga("Authorization", "Bearer token-bueno")).block();

        var respuesta = sinNegocio.intercept(operacion(sesion), p -> Mono.just(vacia(p))).block();

        var entrada = (EntradaGraphQl) respuesta.getExecutionInput().getGraphQLContext().<Object>get(EntradaGraphQl.CLAVE);
        assertThat(entrada.autenticada()).isTrue();
        assertThat(entrada.negocio()).isEmpty();
        assertThat(entrada.autorizacion().puede("a.b.c")).isFalse();
    }

    @Test
    @DisplayName("los intentos fallidos gastan fichas de la IP; agotadas, ni se consulta a Identidad")
    void initFallido_gastaFichasDeLaIp() {
        for (var i = 0; i < 3; i++) {
            assertThatThrownBy(() -> iniciar(new SesionWebSocketFalsa("10.0.0.9"), carga("Authorization", "Bearer malo"))
                    .block()).isInstanceOf(RuntimeException.class);
        }
        var antes = consultas.get();

        assertThatThrownBy(() -> iniciar(new SesionWebSocketFalsa("10.0.0.9"),
                carga("Authorization", "Bearer token-bueno")).block()).isInstanceOf(RuntimeException.class);

        assertThat(consultas).hasValue(antes);
        iniciar(new SesionWebSocketFalsa("10.0.0.10"), carga("Authorization", "Bearer token-bueno")).block();
    }

    @Test
    @DisplayName("un intento bueno no gasta fichas de fallos")
    void initBueno_noGastaFichas() {
        for (var i = 0; i < 20; i++) {
            iniciar(new SesionWebSocketFalsa("10.0.0.11"), carga("Authorization", "Bearer token-bueno")).block();
        }
    }

    // --- Cada operacion ---

    private static WebSocketGraphQlRequest operacion(SesionWebSocketFalsa sesion) {
        return new WebSocketGraphQlRequest(URI.create("/graphql"), new HttpHeaders(), new LinkedMultiValueMap<>(),
                Map.of(), Map.of("query", "{ x }"), "id", Locale.ROOT, sesion);
    }

    private static WebGraphQlResponse vacia(WebGraphQlRequest peticion) {
        return new WebGraphQlResponse(new DefaultExecutionGraphQlResponse(peticion.toExecutionInput(),
                ExecutionResultImpl.newExecutionResult().data(Map.of()).build()));
    }

    private SesionWebSocketFalsa conectada() {
        var sesion = new SesionWebSocketFalsa("10.0.0.1");
        iniciar(sesion, carga("Authorization", "Bearer token-bueno")).block();
        return sesion;
    }

    @Test
    @DisplayName("cada operacion vuelve a validar el token y deja la entrada en el contexto de Reactor de lo que sigue")
    void operacion_revalidaYDejaLaSesion() {
        var sesion = conectada();
        var vista = new AtomicReference<EntradaGraphQl>();
        WebGraphQlInterceptor.Chain cadena = p -> Mono.deferContextual(contexto -> {
            vista.set(contexto.get(EntradaGraphQl.CLAVE));
            return Mono.just(vacia(p));
        });
        var antes = consultas.get();

        var respuesta = interceptor.intercept(operacion(sesion), cadena).block();

        assertThat(respuesta.getExecutionResult().getErrors()).isEmpty();
        assertThat(consultas.get()).isEqualTo(antes + 1);
        assertThat(vista.get().exigirCuenta()).isEqualTo(SesionDePrueba.CUENTA);
        assertThat(vista.get().exigirContexto().negocioId()).isEqualTo(NEGOCIO);
    }

    @Test
    @DisplayName("la entrada tambien viaja en el contexto de GraphQL: la puerta de autorizacion corre antes de que Spring "
            + "restaure nada en el hilo, asi que no puede depender del hilo")
    void operacion_dejaLaSesionEnElContextoDeGraphQL() {
        var sesion = conectada();

        var respuesta = interceptor.intercept(operacion(sesion), p -> Mono.just(vacia(p))).block();

        var enContexto = respuesta.getExecutionInput().getGraphQLContext().<Object>get(EntradaGraphQl.CLAVE);
        assertThat(enContexto).isInstanceOf(EntradaGraphQl.class);
        var entrada = (EntradaGraphQl) enContexto;
        assertThat(entrada.exigirCuenta()).isEqualTo(SesionDePrueba.CUENTA);
        assertThat(entrada.exigirContexto().negocioId()).isEqualTo(NEGOCIO);
        assertThat(entrada.autorizacion().puede("a.b.c")).isTrue();
    }

    @Test
    @DisplayName("SG-10: con la sesion revocada, cada operacion fallida gasta fichas de la IP; agotadas, ni se consulta a Identidad")
    void operacion_sesionRevocada_gastaFichasDeLaIp() {
        var sesion = conectada();
        sesionActual.set("otro-token");
        for (var i = 0; i < 3; i++) {
            var respuesta = interceptor.intercept(operacion(sesion), p -> Mono.just(vacia(p))).block();
            assertThat(respuesta.getExecutionResult().getErrors()).hasSize(1);
        }
        var antes = consultas.get();

        var agotada = interceptor.intercept(operacion(sesion), p -> Mono.just(vacia(p))).block();

        assertThat(agotada.getExecutionResult().getErrors()).hasSize(1);
        assertThat(agotada.getExecutionResult().getErrors().get(0).getExtensions()).containsEntry("codigo", "NO_AUTENTICADO");
        assertThat(consultas).hasValue(antes);
    }

    @Test
    @DisplayName("las operaciones buenas no gastan fichas de fallos de la IP")
    void operacion_buena_noGastaFichasDeFallos() {
        var sesion = conectada();

        for (var i = 0; i < 2; i++) {
            interceptor.intercept(operacion(sesion), p -> Mono.just(vacia(p))).block();
        }

        assertThat(fallosPorIp.claves()).isZero();
    }

    @Test
    @DisplayName("si la sesion se revoco despues de conectar, la siguiente operacion recibe NO_AUTENTICADO sin ejecutarse")
    void operacion_sesionRevocada() {
        var sesion = conectada();
        sesionActual.set("otro-token");
        var ejecuciones = new AtomicInteger();

        var respuesta = interceptor.intercept(operacion(sesion), p -> {
            ejecuciones.incrementAndGet();
            return Mono.just(vacia(p));
        }).block();

        assertThat(ejecuciones).hasValue(0);
        assertThat(respuesta.getExecutionResult().getErrors()).hasSize(1);
        assertThat(respuesta.getExecutionResult().getErrors().get(0).getMessage()).isEqualTo("NO_AUTENTICADO");
        assertThat(respuesta.getExecutionResult().getErrors().get(0).getExtensions())
                .containsEntry("codigo", "NO_AUTENTICADO");
    }

    @Test
    @DisplayName("una conexion que nunca paso por connection_init no puede operar")
    void operacion_sinInit() {
        var respuesta = interceptor.intercept(operacion(new SesionWebSocketFalsa("10.0.0.1")),
                p -> Mono.just(vacia(p))).block();

        assertThat(respuesta.getExecutionResult().getErrors().get(0).getMessage()).isEqualTo("NO_AUTENTICADO");
    }

    @Test
    @DisplayName("las operaciones por WebSocket tambien cuentan para el tope por cuenta: la tercera recibe DEMASIADAS_SOLICITUDES")
    void operacion_topePorCuenta() {
        var sesion = conectada();

        interceptor.intercept(operacion(sesion), p -> Mono.just(vacia(p))).block();
        interceptor.intercept(operacion(sesion), p -> Mono.just(vacia(p))).block();
        var tercera = interceptor.intercept(operacion(sesion), p -> Mono.just(vacia(p))).block();

        assertThat(tercera.getExecutionResult().getErrors().get(0).getMessage()).isEqualTo("DEMASIADAS_SOLICITUDES");
    }

    @Test
    @DisplayName("una peticion HTTP pasa sin tocarse: este interceptor es solo del WebSocket")
    void http_pasaSinTocarse() {
        var http = new WebGraphQlRequest(URI.create("/graphql"), new HttpHeaders(), new LinkedMultiValueMap<>(), null,
                Map.of(), Map.of("query", "{ x }"), "id", Locale.ROOT);

        var respuesta = interceptor.intercept(http, p -> Mono.just(vacia(p))).block();

        assertThat(respuesta.getExecutionResult().getErrors()).isEmpty();
        assertThat(consultas).hasValue(0);
    }
}
