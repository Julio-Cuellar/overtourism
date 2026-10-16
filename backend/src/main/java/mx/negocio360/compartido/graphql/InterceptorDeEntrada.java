package mx.negocio360.compartido.graphql;

import java.util.Map;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import reactor.core.publisher.Mono;

/**
 * Pasa al contexto de GraphQL la entrada que dejo el filtro de {@code /graphql} (IDN-4): ahi la leen los resolvedores. Sin
 * entrada en la peticion (no paso por el filtro) deja una anonima, que no tiene nada: falla cerrado.
 */
public final class InterceptorDeEntrada implements WebGraphQlInterceptor {

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest peticion, Chain cadena) {
        var puesta = peticion.getAttributes().get(EntradaGraphQl.ATRIBUTO);
        var entrada = puesta instanceof EntradaGraphQl e ? e : EntradaGraphQl.anonima(null);
        peticion.configureExecutionInput((entradaDeEjecucion, constructor) -> constructor
                .graphQLContext(Map.of(EntradaGraphQl.CLAVE, entrada)).build());
        return cadena.next(peticion);
    }
}
