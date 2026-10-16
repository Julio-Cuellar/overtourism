package mx.negocio360.compartido.graphql;

import graphql.ErrorClassification;
import graphql.ExecutionResult;
import graphql.GraphqlErrorBuilder;
import graphql.execution.AbortExecutionException;
import graphql.execution.instrumentation.InstrumentationContext;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * SG-06 y SG-08: antes de ejecutar nada, la operacion ya validada se mide y se rechaza si pasa algun limite. Antes de
 * medir deja en el contexto un resumen de la operacion (tipo y campos de la raiz) para el registro (SG-20). El error es
 * fijo: no dice cuanto vale el limite ni que parte de la consulta lo causo. No se ejecuta ningun resolvedor.
 */
public final class LimitesDeConsultaInstrumentation extends SimplePerformantInstrumentation {

    public static final String CAMPOS_RAIZ = "DEMASIADOS_CAMPOS_RAIZ";
    public static final String ALIAS = "DEMASIADOS_ALIAS";
    public static final String PROFUNDIDAD = "CONSULTA_MUY_PROFUNDA";
    public static final String COMPLEJIDAD = "CONSULTA_MUY_COSTOSA";

    private final PropiedadesGraphql limites;

    public LimitesDeConsultaInstrumentation(PropiedadesGraphql limites) {
        this.limites = Objects.requireNonNull(limites, "limites");
    }

    @Override
    public InstrumentationContext<ExecutionResult> beginExecuteOperation(
            InstrumentationExecuteOperationParameters parametros, InstrumentationState estado) {
        var contexto = parametros.getExecutionContext();
        // SG-20: el registro necesita saber que operacion era aunque se rechace por un limite.
        contexto.getGraphQLContext().put(ClavesDeContexto.OPERACION, resumen(contexto.getOperationDefinition(),
                MedidorDeConsulta.camposDeRaiz(contexto.getOperationDefinition(), contexto.getFragmentsByName())));
        var medida = MedidorDeConsulta.medir(contexto.getOperationDefinition(), contexto.getFragmentsByName());
        var excedido = excedido(medida);
        if (excedido != null) {
            throw new AbortExecutionException(List.of(error(excedido)));
        }
        return super.beginExecuteOperation(parametros, estado);
    }

    private static String resumen(graphql.language.OperationDefinition operacion, java.util.Set<String> campos) {
        return operacion.getOperation().name().toLowerCase(java.util.Locale.ROOT) + ":" + String.join(",", campos);
    }

    private String excedido(MedidaDeConsulta medida) {
        if (medida.camposRaiz() > limites.camposRaizMaximos()) {
            return CAMPOS_RAIZ;
        }
        if (medida.alias() > limites.aliasMaximos()) {
            return ALIAS;
        }
        if (medida.profundidad() > limites.profundidadMaxima()) {
            return PROFUNDIDAD;
        }
        if (medida.complejidad() > limites.complejidadMaxima()) {
            return COMPLEJIDAD;
        }
        return null;
    }

    static graphql.GraphQLError error(String codigo) {
        return GraphqlErrorBuilder.newError().message(codigo).errorType(ErrorClassification.errorClassification("LIMITE"))
                .extensions(Map.of("codigo", codigo)).build();
    }
}
