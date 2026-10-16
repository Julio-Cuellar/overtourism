package mx.negocio360.compartido.graphql;

import graphql.ErrorClassification;
import graphql.ExecutionResult;
import graphql.ExecutionResultImpl;
import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.graphql.server.WebGraphQlInterceptor;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.graphql.server.WebGraphQlResponse;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * SG-13 y SG-12: lista blanca de errores. Solo salen tal cual los que lleva nuestro propio codigo
 * ({@code extensions.codigo} en mayusculas): limites, permisos y errores de negocio. Todo lo que produce el motor
 * (sintaxis, validacion, variables, un null en un campo que no lo admite) dice en su texto que campos y tipos existen o
 * que valor se mando, y la introspeccion cerrada no sirve de nada si los errores lo cuentan: sale como
 * {@code CONSULTA_INVALIDA} (sin posiciones ni texto) o {@code ERROR_INTERNO} (con referencia). Va pegado al manejador,
 * por dentro de los limites y del registro.
 */
public final class EnmascaradoDeErroresInterceptor implements WebGraphQlInterceptor, Ordered {

    public static final String CONSULTA_INVALIDA = "CONSULTA_INVALIDA";
    public static final int ORDEN = Ordered.HIGHEST_PRECEDENCE + 20;

    private static final Logger REGISTRO = LoggerFactory.getLogger(EnmascaradoDeErroresInterceptor.class);
    private static final Pattern CODIGO_PROPIO = Pattern.compile("[A-Z][A-Z0-9_]*");

    @Override
    public int getOrder() {
        return ORDEN;
    }

    @Override
    public Mono<WebGraphQlResponse> intercept(WebGraphQlRequest peticion, Chain cadena) {
        return cadena.next(peticion).map(EnmascaradoDeErroresInterceptor::enmascarar);
    }

    private static WebGraphQlResponse enmascarar(WebGraphQlResponse respuesta) {
        var resultado = respuesta;
        var errores = resultado.getExecutionResult().getErrors();
        if (!errores.isEmpty() && !errores.stream().allMatch(EnmascaradoDeErroresInterceptor::esPropio)) {
            List<GraphQLError> nuevos = errores.stream()
                    .map(error -> esPropio(error) ? error : sustituir(error)).toList();
            resultado = resultado.transform(constructor -> constructor.errors(nuevos));
        }
        // Una suscripcion trae sus errores dentro de cada resultado del flujo, no en la primera respuesta.
        Object datos = resultado.getData();
        if (datos instanceof Publisher<?> flujo) {
            resultado = resultado.transform(constructor -> constructor.data(
                    Flux.from(flujo).map(elemento -> enmascarar((ExecutionResult) elemento))));
        }
        return resultado;
    }

    private static ExecutionResult enmascarar(ExecutionResult resultado) {
        var errores = resultado.getErrors();
        if (errores.isEmpty() || errores.stream().allMatch(EnmascaradoDeErroresInterceptor::esPropio)) {
            return resultado;
        }
        List<GraphQLError> nuevos = errores.stream().map(error -> esPropio(error) ? error : sustituir(error)).toList();
        return ExecutionResultImpl.newExecutionResult().from(resultado).errors(nuevos).build();
    }

    private static boolean esPropio(GraphQLError error) {
        var extensiones = error.getExtensions();
        return extensiones != null && extensiones.get("codigo") instanceof String codigo
                && CODIGO_PROPIO.matcher(codigo).matches();
    }

    private static GraphQLError sustituir(GraphQLError error) {
        if (esDeLaConsulta(error.getErrorType())) {
            return GraphqlErrorBuilder.newError().errorType(ErrorType.BAD_REQUEST).message(CONSULTA_INVALIDA)
                    .extensions(Map.of("codigo", CONSULTA_INVALIDA)).build();
        }
        var referencia = ResolvedorDeErroresGraphql.nuevaReferencia();
        REGISTRO.error("ERROR_INTERNO referencia={} clasificacion={} ruta={}", referencia, error.getErrorType(),
                error.getPath());
        return GraphqlErrorBuilder.newError().errorType(ErrorType.INTERNAL_ERROR)
                .message(ResolvedorDeErroresGraphql.ERROR_INTERNO).path(error.getPath())
                .extensions(Map.of("codigo", ResolvedorDeErroresGraphql.ERROR_INTERNO, "referencia", referencia)).build();
    }

    /** Sintaxis, validacion, operacion no soportada o una peticion mal formada: culpa de la consulta, no del servidor. */
    private static boolean esDeLaConsulta(ErrorClassification tipo) {
        return tipo == graphql.ErrorType.InvalidSyntax || tipo == graphql.ErrorType.ValidationError
                || tipo == graphql.ErrorType.OperationNotSupported || tipo == ErrorType.BAD_REQUEST;
    }
}
