package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import graphql.GraphQL;
import graphql.schema.GraphQLSchema;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.util.List;
import mx.negocio360.compartido.graphql.OperacionDeclarada;
import mx.negocio360.compartido.graphql.OperacionesGraphql;
import mx.negocio360.compartido.graphql.TablaDeOperaciones;
import mx.negocio360.compartido.graphql.TipoDeOperacion;
import mx.negocio360.compartido.graphql.VerificadorDeOperaciones;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.graphql.execution.GraphQlSource;

/**
 * SG-21: lo que impide el arranque es que alguien llame a la verificacion. Spring llama a
 * {@code afterSingletonsInstantiated} con todo el contexto armado; si lanza, la aplicacion no arranca.
 */
class VerificadorDeOperacionesTest {

    private static GraphQlSource fuente(String sdl) {
        GraphQLSchema esquema = new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl),
                RuntimeWiring.MOCKED_WIRING);
        return new GraphQlSource() {
            @Override
            public GraphQL graphQl() {
                return GraphQL.newGraphQL(esquema).build();
            }

            @Override
            public GraphQLSchema schema() {
                return esquema;
            }
        };
    }

    private static TablaDeOperaciones tablaCon(String... campos) {
        var operaciones = java.util.Arrays.stream(campos)
                .map(campo -> OperacionDeclarada.deSesion(TipoDeOperacion.QUERY, campo)).toList();
        return new TablaDeOperaciones(List.of((OperacionesGraphql) () -> operaciones), List.of());
    }

    @Test
    @DisplayName("un campo del esquema sin declarar hace fallar el arranque")
    void campoSinDeclarar_fallaElArranque() {
        var verificador = new VerificadorDeOperaciones(tablaCon("a"), fuente("type Query { a: String b: String }"));

        assertThatThrownBy(verificador::afterSingletonsInstantiated).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Query.b");
    }

    @Test
    @DisplayName("con todo declarado el arranque sigue")
    void todoDeclarado_arranca() {
        var verificador = new VerificadorDeOperaciones(tablaCon("a", "b"), fuente("type Query { a: String b: String }"));

        assertThatCode(verificador::afterSingletonsInstantiated).doesNotThrowAnyException();
    }
}
