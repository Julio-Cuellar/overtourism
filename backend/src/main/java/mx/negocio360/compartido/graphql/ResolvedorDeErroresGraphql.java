package mx.negocio360.compartido.graphql;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;

/**
 * SG-13: lo que falla en un resolvedor sale como un codigo y un texto fijos, nunca con el mensaje de la excepcion.
 * <ul>
 *   <li>Sin permiso o sin sesion: {@code PERMISO_DENEGADO} y {@code NO_AUTENTICADO}; con el permiso pero sin la
 *       confirmacion reforzada de una operacion sensible: {@code CONFIRMACION_REQUERIDA}.
 *   <li>Un error de negocio: lo traduce el traductor del modulo (codigo, texto de su plantilla, id del recurso).
 *   <li>Lo demas: {@code ERROR_INTERNO} con una referencia al azar. La misma referencia queda en el registro con las
 *       clases y los primeros marcos de la pila, sin el mensaje (SG-20), para que soporte encuentre la causa sin que el
 *       cliente reciba nada.
 * </ul>
 * La regla vive en {@link ClasificadorDeErrores}; los errores de una suscripcion los resuelve
 * {@link ResolvedorDeErroresDeSuscripcion} con la misma.
 */
public final class ResolvedorDeErroresGraphql extends DataFetcherExceptionResolverAdapter {

    public static final String PERMISO_DENEGADO = "PERMISO_DENEGADO";
    public static final String CONFIRMACION_REQUERIDA = "CONFIRMACION_REQUERIDA";
    public static final String NO_AUTENTICADO = "NO_AUTENTICADO";
    public static final String ERROR_INTERNO = "ERROR_INTERNO";

    private final ClasificadorDeErrores clasificador;

    public ResolvedorDeErroresGraphql(List<TraductorDeErrores> traductores) {
        this.clasificador = new ClasificadorDeErrores(traductores);
    }

    @Override
    protected GraphQLError resolveToSingleError(Throwable excepcion, DataFetchingEnvironment entorno) {
        var c = clasificador.clasificar(excepcion, entorno.getField().getName());
        return GraphqlErrorBuilder.newError(entorno).errorType(c.tipo()).message(c.mensaje()).extensions(c.extensiones())
                .build();
    }

    static String nuevaReferencia() {
        return ClasificadorDeErrores.nuevaReferencia();
    }
}
