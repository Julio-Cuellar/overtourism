package mx.negocio360.compartido.graphql;

import graphql.ExecutionResultImpl;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.springframework.graphql.support.DefaultExecutionGraphQlResponse;

/** Una respuesta de GraphQL que solo trae un error propio, con su codigo fijo; la operacion no se ejecuto. */
final class RechazoGraphql {

    private RechazoGraphql() {
    }

    static WebGraphQlResponse de(WebGraphQlRequest peticion, String codigo) {
        var resultado = ExecutionResultImpl.newExecutionResult().addError(LimitesDeConsultaInstrumentation.error(codigo))
                .build();
        return new WebGraphQlResponse(new DefaultExecutionGraphQlResponse(peticion.toExecutionInput(), resultado));
    }
}
