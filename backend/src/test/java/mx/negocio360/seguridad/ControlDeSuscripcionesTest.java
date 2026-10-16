package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import graphql.ExecutionResultImpl;
import mx.negocio360.compartido.graphql.ControlDeSuscripciones;
import mx.negocio360.compartido.graphql.ControlDeSuscripcionesInterceptor;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.server.WebSocketGraphQlRequest;
import org.springframework.graphql.support.DefaultExecutionGraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.context.Context;

/** SG-15: el tope de operaciones abiertas por cuenta. Una suscripcion cuenta mientras su flujo siga abierto. */
class ControlDeSuscripcionesTest {

    // --- El contador ---

    @Test
    @DisplayName("deja abrir hasta el tope por cuenta y niega la siguiente; liberar devuelve el lugar")
    void contador_hastaElTope() {
        var control = new ControlDeSuscripciones(2);

        assertThat(control.adquirir("a")).isTrue();
        assertThat(control.adquirir("a")).isTrue();
        assertThat(control.adquirir("a")).isFalse();
        control.liberar("a");
        assertThat(control.adquirir("a")).isTrue();
        assertThat(control.activas("a")).isEqualTo(2);
    }

    @Test
    @DisplayName("cada cuenta tiene su propio tope y la memoria no crece: una cuenta en cero se olvida")
    void contador_cuentasIndependientesYMemoriaAcotada() {
        var control = new ControlDeSuscripciones(1);
        control.adquirir("a");

        assertThat(control.adquirir("b")).isTrue();
        control.liberar("a");
        control.liberar("b");

        assertThat(control.cuentas()).isZero();
        control.liberar("a");
        assertThat(control.activas("a")).isZero();
        assertThat(control.cuentas()).isZero();
    }

    @Test
    @DisplayName("con hilos en paralelo nunca se pasa del tope")
    void contador_enParalelo() throws Exception {
        var control = new ControlDeSuscripciones(10);
        var concedidas = new AtomicInteger();
        var hilos = new Thread[8];
        for (var i = 0; i < hilos.length; i++) {
            hilos[i] = new Thread(() -> {
                for (var j = 0; j < 50; j++) {
                    if (control.adquirir("compartida")) {
                        concedidas.incrementAndGet();
                    }
                }
            });
            hilos[i].start();
        }
        for (var hilo : hilos) {
            hilo.join();
        }

        assertThat(concedidas).hasValue(10);
    }

    @Test
    @DisplayName("un tope imposible falla al construirse")
    void tope_invalido() {
        assertThatThrownBy(() -> new ControlDeSuscripciones(0)).isInstanceOf(IllegalArgumentException.class);
    }

    // --- El interceptor ---

    private static WebSocketGraphQlRequest peticionWebSocket() {
        return new WebSocketGraphQlRequest(URI.create("/graphql"), new HttpHeaders(), new LinkedMultiValueMap<>(),
                Map.of(), Map.of("query", "subscription { x }"), "id", Locale.ROOT, new SesionWebSocketFalsa("10.0.0.1"));
    }

    private static WebGraphQlResponse respuesta(WebGraphQlRequest peticion, Object datos) {
        return new WebGraphQlResponse(new DefaultExecutionGraphQlResponse(peticion.toExecutionInput(),
                ExecutionResultImpl.newExecutionResult().data(datos).build()));
    }

    /** El id de cuenta (el que usa el tope) de una cuenta de la prueba. */
    private static String clave(String cuenta) {
        return java.util.UUID.nameUUIDFromBytes(cuenta.getBytes(java.nio.charset.StandardCharsets.UTF_8)).toString();
    }

    private static Context conSesion(String cuenta) {
        var entrada = SesionDePrueba.entrada(java.util.UUID.fromString(clave(cuenta)), SesionDePrueba.NEGOCIO);
        return Context.of(EntradaGraphQl.CLAVE, entrada);
    }

    private static WebGraphQlInterceptor.Chain cadena(java.util.function.Function<WebGraphQlRequest, Mono<WebGraphQlResponse>> f) {
        return f::apply;
    }

    @Test
    @DisplayName("una suscripcion ocupa un lugar mientras su flujo siga abierto y lo libera al terminar")
    void interceptor_suscripcion_ocupaYLibera() {
        var control = new ControlDeSuscripciones(1);
        var interceptor = new ControlDeSuscripcionesInterceptor(control);
        var fuente = Sinks.many().multicast().<Object>directBestEffort();
        var peticion = peticionWebSocket();

        var respuesta = interceptor.intercept(peticion, cadena(p -> Mono.just(respuesta(p, fuente.asFlux()))))
                .contextWrite(conSesion("cuenta-1")).block();

        assertThat(control.activas(clave("cuenta-1"))).isEqualTo(1);
        var suscripcion = Flux.from((org.reactivestreams.Publisher<?>) respuesta.getData()).subscribe();
        assertThat(control.activas(clave("cuenta-1"))).isEqualTo(1);

        fuente.tryEmitComplete();

        assertThat(control.activas(clave("cuenta-1"))).isZero();
        suscripcion.dispose();
    }

    @Test
    @DisplayName("cancelar el flujo (el cliente manda complete o se cierra la conexion) tambien libera el lugar")
    void interceptor_cancelar_libera() {
        var control = new ControlDeSuscripciones(1);
        var interceptor = new ControlDeSuscripcionesInterceptor(control);
        var peticion = peticionWebSocket();

        var respuesta = interceptor.intercept(peticion, cadena(p -> Mono.just(respuesta(p, Flux.never()))))
                .contextWrite(conSesion("cuenta-1")).block();
        var suscripcion = Flux.from((org.reactivestreams.Publisher<?>) respuesta.getData()).subscribe();
        assertThat(control.activas(clave("cuenta-1"))).isEqualTo(1);

        suscripcion.dispose();

        assertThat(control.activas(clave("cuenta-1"))).isZero();
    }

    @Test
    @DisplayName("al llegar al tope la operacion se rechaza con TOPE_DE_SUSCRIPCIONES sin ejecutarla")
    void interceptor_alTope_rechaza() {
        var control = new ControlDeSuscripciones(1);
        var interceptor = new ControlDeSuscripcionesInterceptor(control);
        var ejecuciones = new AtomicInteger();
        WebGraphQlInterceptor.Chain cadena = cadena(p -> {
            ejecuciones.incrementAndGet();
            return Mono.just(respuesta(p, Flux.never()));
        });
        interceptor.intercept(peticionWebSocket(), cadena).contextWrite(conSesion("cuenta-1")).block();

        var rechazada = interceptor.intercept(peticionWebSocket(), cadena).contextWrite(conSesion("cuenta-1")).block();

        assertThat(ejecuciones).hasValue(1);
        assertThat(rechazada.getExecutionResult().getErrors()).hasSize(1);
        assertThat(rechazada.getExecutionResult().getErrors().get(0).getMessage()).isEqualTo("TOPE_DE_SUSCRIPCIONES");
        assertThat(rechazada.getExecutionResult().getErrors().get(0).getExtensions())
                .containsEntry("codigo", "TOPE_DE_SUSCRIPCIONES");
        assertThat(control.activas(clave("cuenta-1"))).isEqualTo(1);
    }

    @Test
    @DisplayName("una consulta o mutacion por WebSocket (sin flujo) libera su lugar de inmediato")
    void interceptor_consultaSimple_liberaAlInstante() {
        var control = new ControlDeSuscripciones(1);
        var interceptor = new ControlDeSuscripcionesInterceptor(control);

        for (var i = 0; i < 5; i++) {
            var respuesta = interceptor.intercept(peticionWebSocket(),
                    cadena(p -> Mono.just(respuesta(p, Map.of("x", "y"))))).contextWrite(conSesion("cuenta-1")).block();
            assertThat(respuesta.getExecutionResult().getErrors()).isEmpty();
        }

        assertThat(control.activas(clave("cuenta-1"))).isZero();
    }

    @Test
    @DisplayName("si la cadena falla, el lugar se libera")
    void interceptor_siFalla_libera() {
        var control = new ControlDeSuscripciones(1);
        var interceptor = new ControlDeSuscripcionesInterceptor(control);

        assertThatThrownBy(() -> interceptor.intercept(peticionWebSocket(), cadena(p -> Mono.error(new IllegalStateException())))
                .contextWrite(conSesion("cuenta-1")).block()).isInstanceOf(IllegalStateException.class);

        assertThat(control.activas(clave("cuenta-1"))).isZero();
    }

    @Test
    @DisplayName("si la cadena termina sin respuesta, el lugar se libera: no se pierde un lugar del tope por nada")
    void interceptor_cadenaVacia_liberaElLugar() {
        var control = new ControlDeSuscripciones(1);
        var interceptor = new ControlDeSuscripcionesInterceptor(control);

        var respuesta = interceptor.intercept(peticionWebSocket(), cadena(p -> Mono.empty()))
                .contextWrite(conSesion("cuenta-1")).block();

        assertThat(respuesta).isNull();
        assertThat(control.activas(clave("cuenta-1"))).isZero();
    }

    @Test
    @DisplayName("una peticion HTTP no pasa por el tope: es solo del WebSocket")
    void interceptor_http_noSeToca() {
        var control = new ControlDeSuscripciones(1);
        var interceptor = new ControlDeSuscripcionesInterceptor(control);
        var http = new WebGraphQlRequest(URI.create("/graphql"), new HttpHeaders(), new LinkedMultiValueMap<>(), null,
                Map.of(), Map.of("query", "{ x }"), "id", Locale.ROOT);

        for (var i = 0; i < 3; i++) {
            interceptor.intercept(http, cadena(p -> Mono.just(respuesta(p, Map.of())))).contextWrite(conSesion("c")).block();
        }

        assertThat(control.cuentas()).isZero();
    }
}
