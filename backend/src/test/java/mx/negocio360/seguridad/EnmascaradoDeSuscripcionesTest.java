package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphqlErrorBuilder;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import mx.negocio360.compartido.graphql.EnmascaradoDeErroresInterceptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.support.DefaultExecutionGraphQlResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * SG-13 en las suscripciones: los errores no vienen en la primera respuesta sino dentro de cada resultado del flujo, y por
 * ahi tambien pueden filtrar lo que el motor dice del esquema o de los valores.
 */
class EnmascaradoDeSuscripcionesTest {

    private static final EnmascaradoDeErroresInterceptor INTERCEPTOR = new EnmascaradoDeErroresInterceptor();

    private static WebGraphQlResponse flujoDe(WebGraphQlRequest peticion, ExecutionResult... resultados) {
        return new WebGraphQlResponse(new DefaultExecutionGraphQlResponse(peticion.toExecutionInput(),
                ExecutionResultImpl.newExecutionResult().data(Flux.just(resultados)).build()));
    }

    private static ExecutionResult conError(String mensajeDelMotor) {
        return ExecutionResultImpl.newExecutionResult().data(null)
                .addError(GraphqlErrorBuilder.newError().message(mensajeDelMotor)
                        .errorType(graphql.ErrorType.ValidationError).build()).build();
    }

    @SuppressWarnings("unchecked")
    private static List<ExecutionResult> resultados(WebGraphQlResponse respuesta) {
        return Flux.from((org.reactivestreams.Publisher<ExecutionResult>) respuesta.getData()).collectList().block();
    }

    @Test
    @DisplayName("los errores del motor dentro de cada resultado del flujo salen como CONSULTA_INVALIDA")
    void erroresDelFlujo_seEnmascaran() {
        var peticion = new WebGraphQlRequest(URI.create("/graphql"), new HttpHeaders(), new LinkedMultiValueMap<>(), null,
                Map.of(), Map.of("query", "subscription { x }"), "id", Locale.ROOT);

        var respuesta = INTERCEPTOR.intercept(peticion, p -> Mono.just(flujoDe(p, conError("Field 'secreto' undefined"))))
                .block();

        var lista = resultados(respuesta);
        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).getErrors()).hasSize(1);
        assertThat(lista.get(0).getErrors().get(0).getMessage()).isEqualTo("CONSULTA_INVALIDA");
        assertThat(lista.get(0).toSpecification().toString()).doesNotContain("secreto");
    }

    @Test
    @DisplayName("un resultado sin errores del flujo pasa igual, con sus datos")
    void resultadoSinErrores_pasaIgual() {
        var peticion = new WebGraphQlRequest(URI.create("/graphql"), new HttpHeaders(), new LinkedMultiValueMap<>(), null,
                Map.of(), Map.of("query", "subscription { x }"), "id", Locale.ROOT);
        var bueno = ExecutionResultImpl.newExecutionResult().data(Map.of("x", "hola")).build();

        var respuesta = INTERCEPTOR.intercept(peticion, p -> Mono.just(flujoDe(p, bueno, conError("Field 'z'")))).block();

        var lista = resultados(respuesta);
        assertThat(lista).hasSize(2);
        assertThat(lista.get(0).<Map<String, Object>>getData()).containsEntry("x", "hola");
        assertThat(lista.get(1).getErrors().get(0).getMessage()).isEqualTo("CONSULTA_INVALIDA");
    }
}
