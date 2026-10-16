package mx.negocio360.canal.web;

import java.util.Map;
import java.util.Objects;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

/**
 * Antes de cada peticion GraphQL por HTTP, resuelve la sesion con {@link ContextoDeSesion} y deja el contexto en el
 * {@code GraphQLContext} bajo {@link ContextoGraphQl#CLAVE}. Sin sesion valida no pone nada, y cada resolvedor
 * responde {@code NO_AUTENTICADO}.
 */
public final class InterceptorDeSesion implements WebGraphQlInterceptor {

    private final ContextoDeSesion sesiones;

    /**
     * Arma el interceptor.
     *
     * @param sesiones resolutor de la sesion
     */
    public InterceptorDeSesion(ContextoDeSesion sesiones) {
        this.sesiones = Objects.requireNonNull(sesiones, "sesiones");
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest peticion, Chain cadena) {
        sesiones.resolver(peticion.getHeaders().getFirst(HttpHeaders.AUTHORIZATION)).ifPresent(contexto ->
                peticion.configureExecutionInput((entrada, constructor) ->
                        constructor.graphQLContext(Map.of(ContextoGraphQl.CLAVE, contexto)).build()));
        return cadena.next(peticion);
    }
}
