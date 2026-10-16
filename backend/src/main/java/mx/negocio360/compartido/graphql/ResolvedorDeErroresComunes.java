package mx.negocio360.compartido.graphql;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import mx.negocio360.compartido.seguridad.NoAutenticadoException;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import org.springframework.core.convert.ConversionException;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.validation.BindException;

/**
 * Errores comunes de cualquier resolvedor (SG-13): sin sesion, sin permiso y argumentos que no se pueden leer. Cada uno sale
 * con un codigo y un texto fijos; nunca con la clase de la excepcion, el permiso que falto ni lo que escribio quien llamo.
 */
public final class ResolvedorDeErroresComunes extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable error, DataFetchingEnvironment entorno) {
        if (error instanceof NoAutenticadoException e) {
            return construir(entorno, ErrorType.UNAUTHORIZED, e.codigo(), "Inicia sesión para continuar.");
        }
        if (error instanceof PermisoDenegadoException) {
            return construir(entorno, ErrorType.FORBIDDEN, "PERMISO_DENEGADO", "No tienes permiso para hacer esto.");
        }
        if (error instanceof BindException || error instanceof ConversionException || error instanceof IllegalArgumentException) {
            return construir(entorno, ErrorType.BAD_REQUEST, "CONSULTA_INVALIDA",
                    "La consulta pide más de lo permitido o está mal formada.");
        }
        return null;
    }

    private static GraphQLError construir(DataFetchingEnvironment entorno, ErrorType tipo, String codigo,
                                          String mensaje) {
        return GraphqlErrorBuilder.newError(entorno).errorType(tipo).message(mensaje)
                .extensions(Map.of("codigo", codigo)).build();
    }
}
