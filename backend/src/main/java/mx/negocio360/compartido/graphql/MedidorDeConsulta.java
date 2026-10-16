package mx.negocio360.compartido.graphql;

import graphql.language.Field;
import graphql.language.FragmentDefinition;
import graphql.language.FragmentSpread;
import graphql.language.InlineFragment;
import graphql.language.OperationDefinition;
import graphql.language.SelectionSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Mide una operacion GraphQL ya analizada y validada (SG-06 y SG-08).
 *
 * <p>Se mide aqui y no con los instrumentos de {@code graphql-java} porque estos recorren el documento expandiendo
 * cada fragmento cada vez que se cita: con 30 fragmentos que se citan dos veces cada uno (unos 2 KB de texto) hacen
 * 2^30 pasos. Aqui cada fragmento se mide una sola vez y se reutiliza, asi que el tiempo es lineal en el tamano del
 * documento, y las cifras se saturan en {@link #TOPE} en vez de desbordarse.
 */
public final class MedidorDeConsulta {

    /** Maximo de cualquier cifra medida; mucho mayor que cualquier limite razonable. */
    public static final int TOPE = 1_000_000;
    private static final int MAX_NOMBRES_DE_RAIZ = 20;

    private MedidorDeConsulta() {
    }

    public static MedidaDeConsulta medir(OperationDefinition operacion, Map<String, FragmentDefinition> fragmentos) {
        var parcial = new Medicion(fragmentos).conjunto(operacion.getSelectionSet());
        return new MedidaDeConsulta(parcial.profundidad, parcial.complejidad, parcial.alias, parcial.campos);
    }

    /**
     * Nombres de los campos de la raiz de la operacion (no los alias), en el orden en que aparecen, sin repetir y
     * expandiendo fragmentos una sola vez. Como la operacion ya paso la validacion, son nombres del esquema.
     */
    public static Set<String> camposDeRaiz(OperationDefinition operacion, Map<String, FragmentDefinition> fragmentos) {
        var nombres = new LinkedHashSet<String>();
        recolectar(operacion.getSelectionSet(), fragmentos, nombres, new HashSet<>());
        return nombres;
    }

    private static void recolectar(SelectionSet selecciones, Map<String, FragmentDefinition> fragmentos,
                                   Set<String> nombres, Set<String> visitados) {
        if (selecciones == null) {
            return;
        }
        for (var seleccion : selecciones.getSelections()) {
            if (seleccion instanceof Field campo) {
                if (nombres.size() < MAX_NOMBRES_DE_RAIZ) {
                    nombres.add(campo.getName());
                }
            } else if (seleccion instanceof InlineFragment enLinea) {
                recolectar(enLinea.getSelectionSet(), fragmentos, nombres, visitados);
            } else if (seleccion instanceof FragmentSpread uso && visitados.add(uso.getName())) {
                var definicion = fragmentos.get(uso.getName());
                if (definicion != null) {
                    recolectar(definicion.getSelectionSet(), fragmentos, nombres, visitados);
                }
            }
        }
    }

    private static int sumar(int a, int b) {
        return (int) Math.min(TOPE, (long) a + b);
    }

    /** Lo medido en un conjunto de selecciones, con la profundidad contada desde ese conjunto. */
    private record Parcial(int profundidad, int complejidad, int alias, int campos) {
        static final Parcial CERO = new Parcial(0, 0, 0, 0);
    }

    private static final class Medicion {

        private final Map<String, FragmentDefinition> fragmentos;
        private final Map<String, Parcial> medidos = new HashMap<>();
        private final Set<String> enCurso = new HashSet<>();

        Medicion(Map<String, FragmentDefinition> fragmentos) {
            this.fragmentos = fragmentos;
        }

        Parcial conjunto(SelectionSet selecciones) {
            if (selecciones == null) {
                return Parcial.CERO;
            }
            var profundidad = 0;
            var complejidad = 0;
            var alias = 0;
            var campos = 0;
            for (var seleccion : selecciones.getSelections()) {
                if (seleccion instanceof Field campo) {
                    var hijos = conjunto(campo.getSelectionSet());
                    campos = sumar(campos, 1);
                    complejidad = sumar(complejidad, sumar(1, hijos.complejidad));
                    alias = sumar(alias, sumar(campo.getAlias() == null ? 0 : 1, hijos.alias));
                    profundidad = Math.max(profundidad, sumar(1, hijos.profundidad));
                } else {
                    // Un fragmento, en linea o con nombre, no agrega un nivel: sus campos quedan a la misma altura.
                    var interno = seleccion instanceof InlineFragment enLinea ? conjunto(enLinea.getSelectionSet())
                            : seleccion instanceof FragmentSpread uso ? fragmento(uso.getName()) : Parcial.CERO;
                    campos = sumar(campos, interno.campos);
                    complejidad = sumar(complejidad, interno.complejidad);
                    alias = sumar(alias, interno.alias);
                    profundidad = Math.max(profundidad, interno.profundidad);
                }
            }
            return new Parcial(profundidad, complejidad, alias, campos);
        }

        private Parcial fragmento(String nombre) {
            var conocido = medidos.get(nombre);
            if (conocido != null) {
                return conocido;
            }
            var definicion = fragmentos.get(nombre);
            // Un ciclo lo rechaza la validacion antes de llegar aqui; si llegara, no se sigue dando vueltas.
            if (definicion == null || !enCurso.add(nombre)) {
                return Parcial.CERO;
            }
            var medido = conjunto(definicion.getSelectionSet());
            enCurso.remove(nombre);
            medidos.put(nombre, medido);
            return medido;
        }
    }
}
