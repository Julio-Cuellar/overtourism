package mx.negocio360.compartido.graphql;

import graphql.execution.AbortExecutionException;
import graphql.execution.instrumentation.InstrumentationState;
import graphql.execution.instrumentation.SimplePerformantInstrumentation;
import graphql.execution.instrumentation.parameters.InstrumentationExecuteOperationParameters;
import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.Selection;
import graphql.language.SelectionSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Topes de una consulta, medidos antes de ejecutarla (SG-06, SG-08): profundidad, cuantos alias lleva y cuantos campos pide.
 * Los alias sirven para barrer datos con una sola peticion, y el numero de campos es el costo. Cuentan igual los fragmentos
 * con nombre y en linea. Si se pasa, no se ejecuta nada y el error es un texto fijo que no repite la consulta ni dice los
 * topes.
 */
public final class LimitesDeConsultaGraphQl extends SimplePerformantInstrumentation {

    public static final String CONSULTA_DEMASIADO_GRANDE = "CONSULTA_DEMASIADO_GRANDE";

    private final int maximaProfundidad;
    private final int maximoDeAlias;
    private final int maximoDeCampos;

    public LimitesDeConsultaGraphQl(int maximaProfundidad, int maximoDeAlias, int maximoDeCampos) {
        this.maximaProfundidad = maximaProfundidad;
        this.maximoDeAlias = maximoDeAlias;
        this.maximoDeCampos = maximoDeCampos;
    }

    @Override
    public graphql.execution.instrumentation.InstrumentationContext<graphql.ExecutionResult> beginExecuteOperation(
            InstrumentationExecuteOperationParameters parametros, InstrumentationState state) {
        var contexto = parametros.getExecutionContext();
        var medida = new Medida(contexto.getFragmentsByName());
        medida.recorrer(contexto.getOperationDefinition().getSelectionSet(), 1, new HashSet<>());
        if (medida.profundidad > maximaProfundidad || medida.alias > maximoDeAlias || medida.campos > maximoDeCampos) {
            throw new AbortExecutionException(CONSULTA_DEMASIADO_GRANDE);
        }
        return super.beginExecuteOperation(parametros, state);
    }

    /** Lo que mide un recorrido por la consulta. */
    private static final class Medida {
        private final Map<String, FragmentDefinition> fragmentos;
        int profundidad;
        int alias;
        int campos;

        Medida(Map<String, FragmentDefinition> fragmentos) {
            this.fragmentos = fragmentos;
        }

        void recorrer(SelectionSet seleccion, int nivel, Set<String> enCurso) {
            if (seleccion == null) {
                return;
            }
            for (Selection<?> s : seleccion.getSelections()) {
                if (s instanceof Field campo) {
                    campos++;
                    if (campo.getAlias() != null) {
                        alias++;
                    }
                    profundidad = Math.max(profundidad, nivel);
                    recorrer(campo.getSelectionSet(), nivel + 1, enCurso);
                } else if (s instanceof InlineFragment enLinea) {
                    recorrer(enLinea.getSelectionSet(), nivel, enCurso);
                } else if (s instanceof FragmentSpread uso) {
                    var definicion = fragmentos.get(uso.getName());
                    // Un fragmento que se cita a si mismo no es valido y GraphQL lo rechaza; aqui solo se evita ciclar.
                    if (definicion != null && enCurso.add(uso.getName())) {
                        recorrer(definicion.getSelectionSet(), nivel, enCurso);
                        enCurso.remove(uso.getName());
                    }
                }
            }
        }
    }
}
