package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import mx.negocio360.compartido.graphql.LimitesDeConsultaInstrumentation;
import mx.negocio360.compartido.graphql.PropiedadesGraphql;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SG-06 y SG-08 sobre un esquema minimo. Son los casos de la prueba que traia el filtro de entrada del equipo para su
 * propio medidor de consultas (profundidad, alias, numero de campos, fragmentos con nombre y en linea, mensaje fijo),
 * ejecutados contra el medidor unico de la plataforma. El tope de campos de aquel es, aqui, la complejidad.
 */
class LimitesDeConsultaDelRemotoTest {

    private static final String ESQUEMA = """
            type Query { nodo: Nodo! raiz: String }
            type Nodo { hijo: Nodo nombre: String lista: [Nodo!]! }
            """;

    private static GraphQL motor(int profundidad, int alias, int complejidad) {
        var registro = new SchemaParser().parse(ESQUEMA);
        var esquema = new SchemaGenerator().makeExecutableSchema(registro, RuntimeWiring.newRuntimeWiring()
                .type("Query", t -> t.dataFetcher("nodo", e -> new Object()).dataFetcher("raiz", e -> "r"))
                .type("Nodo", t -> t.dataFetcher("hijo", e -> new Object()).dataFetcher("nombre", e -> "n")
                        .dataFetcher("lista", e -> java.util.List.of(new Object()))).build());
        var limites = new PropiedadesGraphql(profundidad, complejidad, alias, 100, null, null);
        return GraphQL.newGraphQL(esquema).instrumentation(new LimitesDeConsultaInstrumentation(limites)).build();
    }

    private static String error(GraphQL motor, String consulta) {
        var resultado = motor.execute(ExecutionInput.newExecutionInput(consulta).build());
        return resultado.getErrors().isEmpty() ? "" : resultado.getErrors().get(0).getMessage();
    }

    @Test
    @DisplayName("una consulta dentro de los topes se ejecuta")
    void dentroDeLosTopes() {
        assertThat(error(motor(5, 3, 20), "{ nodo { hijo { nombre } } raiz }")).isEmpty();
    }

    @Test
    @DisplayName("SG-06: mas profundidad que el tope se rechaza antes de ejecutar")
    void profundidad() {
        var consulta = "{ nodo { hijo { hijo { hijo { nombre } } } } }";

        assertThat(error(motor(3, 10, 100), consulta)).isEqualTo(LimitesDeConsultaInstrumentation.PROFUNDIDAD);
        assertThat(error(motor(5, 10, 100), consulta)).isEmpty();
    }

    @Test
    @DisplayName("SG-08: mas alias que el tope se rechaza (los alias sirven para barrer con una sola peticion)")
    void alias() {
        var consulta = "{ a: raiz b: raiz c: raiz d: raiz }";

        assertThat(error(motor(5, 3, 100), consulta)).isEqualTo(LimitesDeConsultaInstrumentation.ALIAS);
        assertThat(error(motor(5, 4, 100), consulta)).isEmpty();
    }

    @Test
    @DisplayName("SG-06: el costo es el numero de campos pedidos; repetirlos con alias tambien cuenta")
    void campos() {
        var consulta = "{ nodo { nombre hijo { nombre } lista { nombre } } }";

        assertThat(error(motor(10, 10, 5), consulta)).isEqualTo(LimitesDeConsultaInstrumentation.COMPLEJIDAD);
        assertThat(error(motor(10, 10, 8), consulta)).isEmpty();
    }

    @Test
    @DisplayName("los fragmentos cuentan: no sirven para esconder profundidad ni campos")
    void fragmentos() {
        var consulta = "query { nodo { ...A } } fragment A on Nodo { hijo { ...B } } fragment B on Nodo { hijo { nombre } }";

        assertThat(error(motor(3, 10, 100), consulta)).isEqualTo(LimitesDeConsultaInstrumentation.PROFUNDIDAD);
        assertThat(error(motor(4, 10, 100), consulta)).isEmpty();
    }

    @Test
    @DisplayName("los fragmentos en linea tambien cuentan")
    void fragmentosEnLinea() {
        var consulta = "{ nodo { ... on Nodo { hijo { ... on Nodo { hijo { nombre } } } } } }";

        assertThat(error(motor(3, 10, 100), consulta)).isEqualTo(LimitesDeConsultaInstrumentation.PROFUNDIDAD);
    }

    @Test
    @DisplayName("el mensaje es fijo: no repite la consulta ni dice cual fue el tope")
    void mensajeFijo() {
        var mensaje = error(motor(1, 1, 1), "{ a: raiz b: raiz nodo { nombre } }");

        assertThat(mensaje).isEqualTo(LimitesDeConsultaInstrumentation.ALIAS).doesNotContain("raiz").doesNotContain("1");
    }
}
