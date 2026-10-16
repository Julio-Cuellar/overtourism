package mx.negocio360.compartido.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.ExecutionInput;
import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SG-06 y SG-08: profundidad, alias y numero de campos de una consulta tienen tope, y el error no dice mas de lo necesario. */
class LimitesDeConsultaGraphQlTest {

    private static final String ESQUEMA = """
            type Query { nodo: Nodo! raiz: String }
            type Nodo { hijo: Nodo nombre: String lista: [Nodo!]! }
            """;

    private static GraphQL motor(int profundidad, int alias, int campos) {
        var registro = new SchemaParser().parse(ESQUEMA);
        var esquema = new SchemaGenerator().makeExecutableSchema(registro, RuntimeWiring.newRuntimeWiring()
                .type("Query", t -> t.dataFetcher("nodo", e -> new Object()).dataFetcher("raiz", e -> "r"))
                .type("Nodo", t -> t.dataFetcher("hijo", e -> new Object()).dataFetcher("nombre", e -> "n")
                        .dataFetcher("lista", e -> java.util.List.of(new Object()))).build());
        return GraphQL.newGraphQL(esquema).instrumentation(new LimitesDeConsultaGraphQl(profundidad, alias, campos))
                .build();
    }

    private static String errores(GraphQL motor, String consulta) {
        var resultado = motor.execute(ExecutionInput.newExecutionInput(consulta).build());
        return resultado.getErrors().isEmpty() ? "" : resultado.getErrors().get(0).getMessage();
    }

    @Test
    @DisplayName("una consulta dentro de los topes se ejecuta")
    void dentroDeLosTopes() {
        assertThat(errores(motor(5, 3, 20), "{ nodo { hijo { nombre } } raiz }")).isEmpty();
    }

    @Test
    @DisplayName("SG-06: mas profundidad que el tope se rechaza antes de ejecutar")
    void profundidad() {
        var consulta = "{ nodo { hijo { hijo { hijo { nombre } } } } }";

        assertThat(errores(motor(3, 10, 100), consulta)).isEqualTo(LimitesDeConsultaGraphQl.CONSULTA_DEMASIADO_GRANDE);
        assertThat(errores(motor(5, 10, 100), consulta)).isEmpty();
    }

    @Test
    @DisplayName("SG-08: mas alias que el tope se rechaza (los alias sirven para barrer con una sola peticion)")
    void alias() {
        var consulta = "{ a: raiz b: raiz c: raiz d: raiz }";

        assertThat(errores(motor(5, 3, 100), consulta)).isEqualTo(LimitesDeConsultaGraphQl.CONSULTA_DEMASIADO_GRANDE);
        assertThat(errores(motor(5, 4, 100), consulta)).isEmpty();
    }

    @Test
    @DisplayName("SG-06: el costo es el numero de campos pedidos; repetirlos con alias tambien cuenta")
    void campos() {
        var consulta = "{ nodo { nombre hijo { nombre } lista { nombre } } }";

        assertThat(errores(motor(10, 10, 5), consulta)).isEqualTo(LimitesDeConsultaGraphQl.CONSULTA_DEMASIADO_GRANDE);
        assertThat(errores(motor(10, 10, 8), consulta)).isEmpty();
    }

    @Test
    @DisplayName("los fragmentos cuentan: no sirven para esconder profundidad ni campos")
    void fragmentos() {
        var consulta = "query { nodo { ...A } } fragment A on Nodo { hijo { ...B } } fragment B on Nodo { hijo { nombre } }";

        assertThat(errores(motor(3, 10, 100), consulta)).isEqualTo(LimitesDeConsultaGraphQl.CONSULTA_DEMASIADO_GRANDE);
        assertThat(errores(motor(4, 10, 100), consulta)).isEmpty();
    }

    @Test
    @DisplayName("los fragmentos en linea tambien cuentan")
    void fragmentosEnLinea() {
        var consulta = "{ nodo { ... on Nodo { hijo { ... on Nodo { hijo { nombre } } } } } }";

        assertThat(errores(motor(3, 10, 100), consulta)).isEqualTo(LimitesDeConsultaGraphQl.CONSULTA_DEMASIADO_GRANDE);
    }

    @Test
    @DisplayName("el mensaje es fijo: no repite la consulta ni dice cual fue el tope")
    void mensajeFijo() {
        var mensaje = errores(motor(1, 1, 1), "{ a: raiz b: raiz nodo { nombre } }");

        assertThat(mensaje).isEqualTo("CONSULTA_DEMASIADO_GRANDE").doesNotContain("raiz").doesNotContain("1");
    }
}
