package mx.negocio360;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import graphql.schema.GraphQLScalarType;
import java.util.Locale;
import mx.negocio360.compartido.graphql.DecimalCoercing;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.execution.GraphQlSource;
import org.springframework.graphql.support.DefaultExecutionGraphQlRequest;
import org.springframework.test.web.servlet.MockMvc;

/** La aplicacion arranca con los esquemas de los modulos y con la superficie de GraphQL cerrada (SG-12). */
@SpringBootTest
@AutoConfigureMockMvc
class ArranqueYSeguridadTest {

    @Autowired
    private GraphQlSource fuente;

    @Autowired
    private ExecutionGraphQlService servicio;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("arranca con el esquema de Clientes y expone sus consultas")
    void arranque_conLosEsquemasDeLosModulos_exponeLasConsultasDeClientes() {
        var consulta = fuente.schema().getQueryType();

        assertThat(consulta.getFieldDefinition("clientes")).isNotNull();
        assertThat(consulta.getFieldDefinition("sugerirClientes")).isNotNull();
    }

    @Test
    @DisplayName("Decimal usa el escalar propio, no uno generico")
    void esquema_escalarDecimal_usaElCoercingPropio() {
        var decimal = (GraphQLScalarType) fuente.schema().getType("Decimal");

        assertThat(decimal.getCoercing()).isInstanceOf(DecimalCoercing.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"{ __schema { queryType { name } } }", "{ __type(name: \"Query\") { name } }"})
    @DisplayName("SG-12: la introspeccion esta desactivada y el error lo dice")
    void introspeccion_consultaDelEsquema_seRechazaPorIntrospeccion(String consulta) {
        var peticion = new DefaultExecutionGraphQlRequest(consulta, null, null, null, "prueba-introspeccion",
                Locale.ROOT);

        var respuesta = servicio.execute(peticion).block();

        assertThat(respuesta).isNotNull();
        assertThat(respuesta.getErrors()).isNotEmpty()
                .anyMatch(error -> error.getMessage().toLowerCase(Locale.ROOT).contains("introspection"));
    }

    @Test
    @DisplayName("SG-12: GraphiQL y la impresion del esquema no estan publicados")
    void rutas_graphiqlYEsquemaImpreso_noExisten() throws Exception {
        mockMvc.perform(get("/graphiql")).andExpect(status().isNotFound());
        mockMvc.perform(get("/graphql/schema")).andExpect(status().isNotFound());
    }
}
