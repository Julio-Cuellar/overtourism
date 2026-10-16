package mx.negocio360.compartido.graphql;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import java.util.List;
import org.springframework.graphql.execution.SubscriptionExceptionResolverAdapter;

/**
 * SG-13 y SG-20 en las suscripciones: cuando el flujo de eventos falla, Spring GraphQL manda al cliente los errores que
 * este resolvedor produzca y, si ninguno resuelve la excepcion, la escribe en el registro con su mensaje completo (que
 * puede traer datos). Este las resuelve todas con la misma regla que los resolvedores de consultas.
 */
public final class ResolvedorDeErroresDeSuscripcion extends SubscriptionExceptionResolverAdapter {

    private final ClasificadorDeErrores clasificador;

    public ResolvedorDeErroresDeSuscripcion(List<TraductorDeErrores> traductores) {
        this.clasificador = new ClasificadorDeErrores(traductores);
    }

    @Override
    protected GraphQLError resolveToSingleError(Throwable excepcion) {
        var c = clasificador.clasificar(excepcion, "suscripcion");
        return GraphqlErrorBuilder.newError().errorType(c.tipo()).message(c.mensaje()).extensions(c.extensiones())
                .build();
    }
}
