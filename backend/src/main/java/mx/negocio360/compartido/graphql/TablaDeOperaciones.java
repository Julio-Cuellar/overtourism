package mx.negocio360.compartido.graphql;

import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;

/**
 * El inventario de operaciones de la plataforma (SG-21) y de lo que exige cada una (SG-03 y SG-04): los campos de la raiz
 * del esquema, el permiso de cada uno y el de cada variante de comando. Se arma con lo que declara cada modulo.
 *
 * <p>{@link #verificar} compara la tabla con el esquema y falla si no coinciden en algo: un campo que nadie declaro (o una
 * variante), una declaracion de algo que no existe, un permiso que ningun modulo declara. Se llama al arrancar, asi que
 * una operacion agregada al esquema y olvidada en la tabla no puede quedar abierta ni fuera del inventario.
 */
public final class TablaDeOperaciones {

    private final Map<String, OperacionDeclarada> operaciones = new LinkedHashMap<>();
    private final Map<String, ComandoDeclarado> variantes = new LinkedHashMap<>();
    private final Map<String, PermisoDeclarado> catalogo;

    public TablaDeOperaciones(List<OperacionesGraphql> fuentes, Collection<PermisoDeclarado> catalogo) {
        this.catalogo = catalogo.stream().collect(Collectors.toMap(PermisoDeclarado::codigo, p -> p, (a, b) -> a,
                LinkedHashMap::new));
        var repetidas = new TreeSet<String>();
        for (var fuente : fuentes) {
            for (var operacion : fuente.operaciones()) {
                if (operaciones.putIfAbsent(clave(operacion.tipo(), operacion.campo()), operacion) != null) {
                    repetidas.add(operacion.nombre());
                }
            }
            for (var comando : fuente.comandos()) {
                if (variantes.putIfAbsent(comando.variante(), comando) != null) {
                    repetidas.add("la variante " + comando.variante());
                }
            }
        }
        if (!repetidas.isEmpty()) {
            throw new IllegalStateException("Operaciones de GraphQL declaradas dos veces: " + String.join(", ", repetidas));
        }
    }

    private static String clave(TipoDeOperacion tipo, String campo) {
        return tipo + ":" + campo;
    }

    public Optional<OperacionDeclarada> operacion(TipoDeOperacion tipo, String campo) {
        return Optional.ofNullable(operaciones.get(clave(tipo, campo)));
    }

    /** El permiso de una variante; vacio si no existe o si es publica. */
    public Optional<String> permisoDeVariante(String variante) {
        return Optional.ofNullable(variantes.get(variante)).map(ComandoDeclarado::permiso);
    }

    /** Si la variante esta declarada y es publica: cualquiera puede ejecutarla, sin sesion. */
    public boolean esVariantePublica(String variante) {
        var comando = variantes.get(variante);
        return comando != null && comando.publico();
    }

    /** Si la tabla conoce la variante, sea publica o con permiso. */
    public boolean conoceLaVariante(String variante) {
        return variantes.containsKey(variante);
    }

    /** Si el permiso esta declarado como sensible; un permiso que no se conoce no lo es. */
    public boolean sensible(String permiso) {
        var declarado = catalogo.get(permiso);
        return declarado != null && declarado.sensible();
    }

    public List<OperacionDeclarada> operaciones() {
        return List.copyOf(operaciones.values());
    }

    /** Las variantes que piden permiso, con el permiso de cada una. */
    public Map<String, String> variantes() {
        var conPermiso = new LinkedHashMap<String, String>();
        variantes.forEach((variante, comando) -> {
            if (!comando.publico()) {
                conPermiso.put(variante, comando.permiso());
            }
        });
        return Map.copyOf(conPermiso);
    }

    /** Las variantes que cualquiera puede ejecutar, sin sesion. */
    public java.util.Set<String> variantesPublicas() {
        return variantes.values().stream().filter(ComandoDeclarado::publico).map(ComandoDeclarado::variante)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /**
     * Comprueba que la tabla y el esquema coincidan. Reporta todos los problemas juntos.
     *
     * @throws IllegalStateException si algo no coincide; los mensajes nombran campos, variantes y permisos, no datos
     */
    public void verificar(GraphQLSchema esquema) {
        var problemas = new ArrayList<String>();
        var raices = new EnumMap<TipoDeOperacion, GraphQLObjectType>(TipoDeOperacion.class);
        raices.put(TipoDeOperacion.QUERY, esquema.getQueryType());
        raices.put(TipoDeOperacion.MUTATION, esquema.getMutationType());
        raices.put(TipoDeOperacion.SUBSCRIPTION, esquema.getSubscriptionType());
        var variantesDelEsquema = new LinkedHashSet<String>();
        var hayComandos = false;

        for (var tipo : TipoDeOperacion.values()) {
            var raiz = raices.get(tipo);
            var declaradas = operaciones.values().stream().filter(o -> o.tipo() == tipo).toList();
            var camposDelEsquema = raiz == null ? Set.<String>of()
                    : raiz.getFieldDefinitions().stream().map(c -> c.getName()).collect(Collectors.toCollection(TreeSet::new));
            var nombreDelTipo = raiz == null ? tipo.etiqueta() : raiz.getName();
            for (var campo : camposDelEsquema) {
                if (declaradas.stream().noneMatch(o -> o.campo().equals(campo))) {
                    problemas.add("falta declarar " + nombreDelTipo + "." + campo);
                }
            }
            for (var declarada : declaradas) {
                if (raiz == null) {
                    problemas.add("se declaro " + declarada.nombre() + " pero el esquema no tiene un tipo " + tipo.etiqueta());
                } else if (!camposDelEsquema.contains(declarada.campo())) {
                    problemas.add("se declaro " + declarada.nombre() + " pero no existe en el esquema");
                } else if (declarada.argumentoDeComando() != null) {
                    hayComandos = true;
                    verificarComando(raiz, declarada, variantesDelEsquema, problemas);
                }
                if (declarada.permiso() != null && !catalogo.containsKey(declarada.permiso())) {
                    problemas.add("el permiso " + declarada.permiso() + " de " + declarada.nombre()
                            + " no lo declara ningun modulo");
                }
            }
        }
        if (hayComandos) {
            for (var variante : variantesDelEsquema) {
                if (!variantes.containsKey(variante)) {
                    problemas.add("falta declarar la variante " + variante + " del comando");
                }
            }
        }
        for (var entrada : variantes.entrySet()) {
            if (hayComandos && !variantesDelEsquema.contains(entrada.getKey())) {
                problemas.add("se declaro la variante " + entrada.getKey() + " que ningun comando del esquema tiene");
            } else if (!hayComandos) {
                problemas.add("se declaro la variante " + entrada.getKey() + " pero ninguna operacion recibe comandos");
            }
            if (!entrada.getValue().publico() && !catalogo.containsKey(entrada.getValue().permiso())) {
                problemas.add("el permiso " + entrada.getValue().permiso() + " de la variante " + entrada.getKey()
                        + " no lo declara ningun modulo");
            }
        }
        if (!problemas.isEmpty()) {
            throw new IllegalStateException("Operaciones de GraphQL mal declaradas: " + String.join("; ", problemas));
        }
    }

    private static void verificarComando(GraphQLObjectType raiz, OperacionDeclarada declarada,
                                         Set<String> variantesDelEsquema, List<String> problemas) {
        var argumento = raiz.getFieldDefinition(declarada.campo()).getArgument(declarada.argumentoDeComando());
        if (argumento == null) {
            problemas.add(declarada.nombre() + " no tiene el argumento " + declarada.argumentoDeComando());
            return;
        }
        var tipoDelArgumento = GraphQLTypeUtil.unwrapAll(argumento.getType());
        if (!(tipoDelArgumento instanceof GraphQLInputObjectType entrada)) {
            problemas.add("el argumento " + declarada.argumentoDeComando() + " de " + declarada.nombre()
                    + " no es un tipo de entrada con variantes");
            return;
        }
        entrada.getFieldDefinitions().forEach(variante -> variantesDelEsquema.add(variante.getName()));
    }
}
