package mx.negocio360.negocios.web;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.LinkedHashMap;
import mx.negocio360.negocios.dominio.NegociosException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;

/**
 * Los errores de negocio de Negocios, con su codigo y su texto fijos (seccion 7.8, SG-13). Un error que pide algo que falta
 * ({@code PUBLICACION_INCOMPLETA}) dice que falta. Nunca repite lo que escribio la persona.
 */
public final class ResolvedorDeErroresDeNegocios extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable error, DataFetchingEnvironment entorno) {
        if (!(error instanceof NegociosException e)) {
            return null;
        }
        var extensiones = new LinkedHashMap<String, Object>();
        extensiones.put("codigo", e.codigo().name());
        if (!e.faltantes().isEmpty()) {
            extensiones.put("faltantes", e.faltantes());
        }
        return GraphqlErrorBuilder.newError(entorno).errorType(tipoDe(e)).message(e.codigo().mensaje())
                .extensions(extensiones).build();
    }

    private static ErrorType tipoDe(NegociosException e) {
        return switch (ErroresNegociosAdvice.estadoDe(e.codigo())) {
            case NOT_FOUND -> ErrorType.NOT_FOUND;
            case FORBIDDEN -> ErrorType.FORBIDDEN;
            default -> ErrorType.BAD_REQUEST;
        };
    }
}
