package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.ExecutionResultImpl;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import mx.negocio360.compartido.graphql.AutenticacionWebSocketInterceptor;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.graphql.InterceptorDeEntrada;
import mx.negocio360.compartido.seguridad.LimitadorDeTasa;
import mx.negocio360.compartido.seguridad.SesionDePeticion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.server.WebSocketGraphQlRequest;
import org.springframework.graphql.support.DefaultExecutionGraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import reactor.core.publisher.Mono;

/**
 * El interceptor de entrada de HTTP no debe tocar el contexto de una operacion por WebSocket: la entrada de esa operacion la
 * dejo la autenticacion de la conexion, y una anonima de HTTP no puede tomar su lugar, sin importar el orden en que Spring
 * aplique los interceptores.
 */
class InterceptorDeEntradaWebSocketTest {

    private final RelojManual reloj = new RelojManual(Instant.parse("2026-10-16T12:00:00Z"));
    private final AutenticacionWebSocketInterceptor autenticacion = new AutenticacionWebSocketInterceptor(
            token -> Optional.of(new SesionDePeticion(SesionDePrueba.CUENTA.toString(), SesionDePrueba.NEGOCIO, Set.of("a.b"))),
            new LimitadorDeTasa(5, Duration.ofMinutes(1), 100, reloj),
            new LimitadorDeTasa(5, Duration.ofMinutes(1), 100, reloj));

    private static WebSocketGraphQlRequest operacion(SesionWebSocketFalsa sesion) {
        return new WebSocketGraphQlRequest(URI.create("/graphql"), new HttpHeaders(), new LinkedMultiValueMap<>(),
                Map.of(), Map.of("query", "{ x }"), "id", Locale.ROOT, sesion);
    }

    private static WebGraphQlResponse vacia(WebGraphQlRequest peticion) {
        return new WebGraphQlResponse(new DefaultExecutionGraphQlResponse(peticion.toExecutionInput(),
                ExecutionResultImpl.newExecutionResult().data(Map.of()).build()));
    }

    @Test
    @DisplayName("solo: una operacion por WebSocket sale sin que este interceptor ponga ninguna entrada en el contexto")
    void porWebSocket_noPoneNada() {
        var respuesta = new InterceptorDeEntrada()
                .intercept(operacion(new SesionWebSocketFalsa("10.0.0.1")), p -> Mono.just(vacia(p))).block();

        assertThat(respuesta.getExecutionInput().getGraphQLContext().<Object>get(EntradaGraphQl.CLAVE)).isNull();
    }

    @Test
    @DisplayName("en cadena, por dentro de la autenticacion del WebSocket: la entrada autenticada llega intacta")
    void porWebSocket_laEntradaAutenticadaNoSePisa() {
        var sesion = new SesionWebSocketFalsa("10.0.0.1");
        var carga = new HashMap<String, Object>();
        carga.put("Authorization", "Bearer token-bueno");
        autenticacion.handleConnectionInitialization(sesion, carga).block();

        var respuesta = autenticacion.intercept(operacion(sesion),
                p -> new InterceptorDeEntrada().intercept(p, c -> Mono.just(vacia(c)))).block();

        var entrada = (EntradaGraphQl) respuesta.getExecutionInput().getGraphQLContext().<Object>get(EntradaGraphQl.CLAVE);
        assertThat(entrada.autenticada()).isTrue();
        assertThat(entrada.exigirContexto().negocioId()).isEqualTo(SesionDePrueba.NEGOCIO);
    }

    @Test
    @DisplayName("por HTTP sigue llevando al contexto la entrada del filtro, y una anonima si no hay ninguna")
    void porHttp_siguePonendoLaEntrada() {
        var http = new WebGraphQlRequest(URI.create("/graphql"), new HttpHeaders(), new LinkedMultiValueMap<>(), null,
                SesionDePrueba.atributos(), Map.of("query", "{ x }"), "id", Locale.ROOT);
        SesionDePrueba.entrar();
        try {
            var conEntrada = new WebGraphQlRequest(URI.create("/graphql"), new HttpHeaders(), new LinkedMultiValueMap<>(),
                    null, SesionDePrueba.atributos(), Map.of("query", "{ x }"), "id", Locale.ROOT);
            var respuesta = new InterceptorDeEntrada().intercept(conEntrada, p -> Mono.just(vacia(p))).block();
            assertThat(((EntradaGraphQl) respuesta.getExecutionInput().getGraphQLContext().<Object>get(EntradaGraphQl.CLAVE))
                    .autenticada()).isTrue();
        } finally {
            SesionDePrueba.salir();
        }
        var sinEntrada = new WebGraphQlRequest(URI.create("/graphql"), new HttpHeaders(), new LinkedMultiValueMap<>(), null,
                Map.of(), Map.of("query", "{ x }"), "id", Locale.ROOT);
        var anonima = new InterceptorDeEntrada().intercept(sinEntrada, p -> Mono.just(vacia(p))).block();
        assertThat(((EntradaGraphQl) anonima.getExecutionInput().getGraphQLContext().<Object>get(EntradaGraphQl.CLAVE))
                .autenticada()).isFalse();
        assertThat(http).isNotNull();
    }
}
