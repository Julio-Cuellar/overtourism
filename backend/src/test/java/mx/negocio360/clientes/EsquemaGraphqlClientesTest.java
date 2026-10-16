package mx.negocio360.clientes;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.language.IntValue;
import graphql.schema.GraphQLEnumType;
import graphql.schema.GraphQLEnumValueDefinition;
import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLInputObjectField;
import graphql.schema.GraphQLInputObjectType;
import graphql.schema.GraphQLNamedType;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
import graphql.schema.GraphQLTypeUtil;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import graphql.schema.idl.TypeDefinitionRegistry;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import mx.negocio360.clientes.consultas.LimitesDeConsulta;
import mx.negocio360.clientes.eventos.EstadoConsentimiento;
import mx.negocio360.clientes.eventos.Finalidad;
import mx.negocio360.clientes.eventos.MedioConsentimiento;
import mx.negocio360.clientes.eventos.OrigenCliente;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** Contrato del esquema GraphQL de Clientes: operaciones, limites y datos que nunca deben salir. */
class EsquemaGraphqlClientesTest {

    private static GraphQLSchema esquema;

    @BeforeAll
    static void construirEsquema() throws IOException {
        var parser = new SchemaParser();
        var registro = new TypeDefinitionRegistry();
        // Solo la parte comun y la de Clientes: cada modulo prueba su propio esquema (Negocios tiene el suyo).
        var recursos = new PathMatchingResourcePatternResolver().getResources("classpath*:graphql/**/*.graphqls");
        recursos = Arrays.stream(recursos).filter(r -> "compartido.graphqls".equals(r.getFilename())
                || "clientes.graphqls".equals(r.getFilename())).toArray(org.springframework.core.io.Resource[]::new);
        assertThat(recursos).as("archivos .graphqls de compartido y clientes").hasSize(2);
        for (var recurso : recursos) {
            try (var lector = new InputStreamReader(recurso.getInputStream(), StandardCharsets.UTF_8)) {
                registro.merge(parser.parse(lector));
            }
        }
        esquema = new SchemaGenerator().makeExecutableSchema(registro, RuntimeWiring.MOCKED_WIRING);
    }

    private static Set<String> campos(GraphQLFieldsContainer tipo) {
        return tipo.getFieldDefinitions().stream().map(GraphQLFieldDefinition::getName).collect(Collectors.toSet());
    }

    private static GraphQLObjectType objeto(String nombre) {
        return (GraphQLObjectType) esquema.getType(nombre);
    }

    private static Stream<GraphQLNamedType> tiposDelModelo() {
        return esquema.getAllTypesAsList().stream().filter(t -> !t.getName().startsWith("__"));
    }

    private static String nombreDelTipo(GraphQLFieldDefinition campo) {
        return ((GraphQLNamedType) GraphQLTypeUtil.unwrapAll(campo.getType())).getName();
    }

    @Test
    @DisplayName("el esquema se construye y tiene consultas y mutaciones")
    void esquema_alConstruirse_tieneConsultasYMutaciones() {
        assertThat(esquema.getQueryType()).isNotNull();
        assertThat(esquema.getMutationType()).isNotNull();
    }

    @Test
    @DisplayName("las consultas de Clientes son exactamente las del inventario de operaciones")
    void consultas_deClientes_sonLasDelInventario() {
        assertThat(campos(esquema.getQueryType())).containsExactlyInAnyOrder("clientes", "cliente",
                "historialCliente", "consentimientosCliente", "sugerirClientes", "panoramaClientes",
                "avisoPrivacidadVigente", "solicitudesArco");
    }

    @Test
    @DisplayName("las mutaciones son solo el par de la plataforma (D-55)")
    void mutaciones_sonSoloPrepararYConfirmar() {
        assertThat(campos(esquema.getMutationType())).containsExactlyInAnyOrder("prepararComando",
                "confirmarComando");
    }

    @Test
    @DisplayName("SG-02: ningun tipo, campo o argumento menciona el negocio")
    void nombres_delEsquema_noMencionanElNegocio() {
        var nombres = tiposDelModelo().flatMap(tipo -> {
            Stream<String> propios = Stream.of(tipo.getName());
            Stream<String> deObjeto = tipo instanceof GraphQLObjectType objeto
                    ? objeto.getFieldDefinitions().stream().flatMap(c -> Stream.concat(Stream.of(c.getName()),
                            c.getArguments().stream().map(a -> a.getName())))
                    : Stream.empty();
            Stream<String> deEntrada = tipo instanceof GraphQLInputObjectType entrada
                    ? entrada.getFieldDefinitions().stream().map(GraphQLInputObjectField::getName)
                    : Stream.empty();
            return Stream.of(propios, deObjeto, deEntrada).flatMap(s -> s);
        }).toList();

        assertThat(nombres).isNotEmpty().noneMatch(n -> n.toLowerCase().contains("negocio"));
    }

    @Test
    @DisplayName("SG-17: ComandoInput acepta exactamente una de las ocho variantes de Clientes")
    void comandoInput_esOneOfConLasOchoVariantes() {
        var comando = (GraphQLInputObjectType) esquema.getType("ComandoInput");

        assertThat(comando.isOneOf()).isTrue();
        assertThat(comando.getFieldDefinitions().stream().map(GraphQLInputObjectField::getName).toList())
                .containsExactlyInAnyOrder("registrarCliente", "editarCliente", "registrarConsentimiento",
                        "anonimizarCliente", "publicarAvisoPrivacidad", "registrarSolicitudArco",
                        "ampliarSolicitudArco", "resolverSolicitudArco");
    }

    @ParameterizedTest
    @ValueSource(strings = {"clientes", "historialCliente", "solicitudesArco"})
    @DisplayName("SG-07: las listas paginadas piden 20 por defecto y no aceptan null")
    void listasPaginadas_primerosPorDefecto_esVeinteYNoNulo(String consulta) {
        var argumento = esquema.getQueryType().getFieldDefinition(consulta).getArgument("primeros");

        assertThat(GraphQLTypeUtil.isNonNull(argumento.getType())).isTrue();
        assertThat(argumento.getArgumentDefaultValue().isSet()).isTrue();
        var valorPorDefecto = (IntValue) argumento.getArgumentDefaultValue().getValue();
        assertThat(valorPorDefecto.getValue().intValue()).isEqualTo(LimitesDeConsulta.POR_PAGINA_INICIAL);
    }

    @Test
    @DisplayName("SG-07: solo dos consultas devuelven una lista plana, y ambas tienen tope en el resolvedor")
    void listasPlanas_soloLasDosAcotadasPorElContrato() {
        var planas = esquema.getQueryType().getFieldDefinitions().stream()
                .filter(c -> GraphQLTypeUtil.isList(GraphQLTypeUtil.unwrapNonNull(c.getType())))
                .map(GraphQLFieldDefinition::getName)
                .toList();

        assertThat(planas).containsExactlyInAnyOrder("consentimientosCliente", "sugerirClientes");
    }

    static Stream<Arguments> enumsCompartidosConJava() {
        return Stream.of(
                Arguments.of("Finalidad", Finalidad.class),
                Arguments.of("EstadoConsentimiento", EstadoConsentimiento.class),
                Arguments.of("MedioConsentimiento", MedioConsentimiento.class),
                Arguments.of("OrigenCliente", OrigenCliente.class));
    }

    @ParameterizedTest
    @MethodSource("enumsCompartidosConJava")
    @DisplayName("los enums de Java y los del esquema tienen los mismos valores")
    void enums_javaYGraphql_tienenLosMismosValores(String nombre, Class<? extends Enum<?>> enumJava) {
        var enGraphql = ((GraphQLEnumType) esquema.getType(nombre)).getValues().stream()
                .map(GraphQLEnumValueDefinition::getName).toList();
        var enJava = Arrays.stream(enumJava.getEnumConstants()).map(Enum::name).toList();

        assertThat(enGraphql).containsExactlyInAnyOrderElementsOf(enJava);
    }

    @ParameterizedTest
    @ValueSource(strings = {"ClienteResumen", "ClienteSugerido"})
    @DisplayName("SC-02: listas y sugerencias solo muestran el telefono enmascarado")
    void listas_noExponenDatosPersonalesCompletos(String tipo) {
        var nombres = campos(objeto(tipo));

        assertThat(nombres).contains("telefonoEnmascarado");
        assertThat(nombres).doesNotContain("telefono", "correo", "fechaNacimiento", "notas", "apellidos");
    }

    @Test
    @DisplayName("la ficha completa tiene los datos de D-59")
    void cliente_fichaCompleta_tieneLosDatosDelAviso() {
        assertThat(campos(objeto("Cliente"))).contains("nombre", "apellidos", "telefono", "correo",
                "fechaNacimiento", "notas", "origen", "avisoVersion", "version");
    }

    @Test
    @DisplayName("D-59: no hay domicilio ni RFC")
    void cliente_sinFinalidad_noTieneDomicilioNiRfc() {
        assertThat(campos(objeto("Cliente"))).doesNotContain("domicilio", "rfc");
        assertThat(campos(objeto("ClienteResumen"))).doesNotContain("domicilio", "rfc");
    }

    @Test
    @DisplayName("el dinero del esquema es Decimal")
    void dinero_delEsquema_esDecimal() {
        assertThat(nombreDelTipo(objeto("ClienteResumen").getFieldDefinition("totalComprado"))).isEqualTo("Decimal");
        assertThat(nombreDelTipo(objeto("ClienteResumen").getFieldDefinition("ticketPromedio"))).isEqualTo("Decimal");
        assertThat(nombreDelTipo(objeto("CompraVista").getFieldDefinition("monto"))).isEqualTo("Decimal");
    }

    @Test
    @DisplayName("ningun campo del esquema usa Float")
    void esquema_ningunCampo_usaFloat() {
        List<String> conFloat = tiposDelModelo()
                .filter(t -> t instanceof GraphQLObjectType)
                .map(t -> (GraphQLObjectType) t)
                .flatMap(t -> t.getFieldDefinitions().stream().filter(c -> nombreDelTipo(c).equals("Float"))
                        .map(c -> t.getName() + "." + c.getName()))
                .toList();

        assertThat(conFloat).isEmpty();
    }
}
