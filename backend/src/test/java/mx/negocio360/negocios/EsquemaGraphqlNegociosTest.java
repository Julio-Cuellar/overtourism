package mx.negocio360.negocios;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.language.IntValue;
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
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import mx.negocio360.negocios.aplicacion.LimitesPublicos;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Contrato del esquema GraphQL de Negocios (N0): panel, superficie publica y lo que nunca debe salir. */
class EsquemaGraphqlNegociosTest {

    private static GraphQLSchema esquema;

    @BeforeAll
    static void construirEsquema() throws IOException {
        var parser = new SchemaParser();
        var registro = new TypeDefinitionRegistry();
        // La parte comun y la de Negocios. compartido.graphqls cita ComandoInput, que hoy se define en clientes.graphqls
        // (D-55); por eso se carga tambien, pero solo se prueba lo de Negocios.
        for (var archivo : new String[] {"graphql/compartido.graphqls", "graphql/clientes.graphqls",
                "graphql/negocios.graphqls"}) {
            try (var lector = new InputStreamReader(new ClassPathResource(archivo).getInputStream(),
                    StandardCharsets.UTF_8)) {
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

    /** Tipos de objeto alcanzables desde una operacion (sin escalares ni enums, que si se pueden compartir). */
    private static Set<String> tiposAlcanzables(GraphQLFieldDefinition operacion) {
        var vistos = new HashSet<String>();
        var pendientes = new java.util.ArrayDeque<GraphQLNamedType>();
        pendientes.add((GraphQLNamedType) GraphQLTypeUtil.unwrapAll(operacion.getType()));
        while (!pendientes.isEmpty()) {
            var tipo = pendientes.poll();
            if (!(tipo instanceof GraphQLObjectType o) || !vistos.add(o.getName())) {
                continue;
            }
            o.getFieldDefinitions().forEach(c -> pendientes.add((GraphQLNamedType) GraphQLTypeUtil.unwrapAll(c.getType())));
        }
        return vistos;
    }

    @Test
    @DisplayName("el esquema se construye y trae las consultas de N0 a N3")
    void consultas_sonLasDeLasEtapasAutorizadas() {
        assertThat(campos(esquema.getQueryType())).contains("miNegocio", "sucursales", "horarioDeSucursal",
                "misAltasDeNegocio", "negociosPublicos", "negocioPublico", "resenas", "resenasDeNegocio");
    }

    @Test
    @DisplayName("las mutaciones del panel de N2 y N3 y el par de la plataforma")
    void mutaciones_sonLasDeLasEtapasAutorizadas() {
        assertThat(campos(esquema.getMutationType())).contains("actualizarNegocio", "definirSubcategorias",
                "definirAtributos", "definirDisponibilidad", "publicarNegocio", "actualizarSucursal", "definirHorario",
                "agregarExcepcionHorario", "quitarExcepcionHorario", "responderResena", "prepararComando", "confirmarComando");
    }

    @Test
    @DisplayName("W1: la unica suscripcion de P1 es altaDeNegocio")
    void suscripcion_esAltaDeNegocio() {
        assertThat(campos(esquema.getSubscriptionType())).containsExactly("altaDeNegocio");
    }

    @Test
    @DisplayName("SG-02: el panel no lleva negocioId en ningun tipo, campo ni argumento")
    void panel_noLlevaNegocioId() {
        var mal = new HashSet<String>();
        for (var operacion : esquema.getQueryType().getFieldDefinitions()) {
            revisar(operacion, mal);
        }
        for (var operacion : esquema.getMutationType().getFieldDefinitions()) {
            revisar(operacion, mal);
        }

        assertThat(mal).isEmpty();
    }

    private static void revisar(GraphQLFieldDefinition operacion, Set<String> mal) {
        // Excepciones acotadas a SG-02: las consultas publicas reciben el id del negocio como objeto consultado, y
        // misAltasDeNegocio filtra por la cuenta del token porque todavia no hay negocio activo.
        if (operacion.getName().equals("negocioPublico") || operacion.getName().equals("negociosPublicos")
                || operacion.getName().equals("misAltasDeNegocio") || operacion.getName().equals("resenasDeNegocio")) {
            return;
        }
        operacion.getArguments().forEach(a -> {
            if (a.getName().equalsIgnoreCase("negocioId")) {
                mal.add(operacion.getName() + "(" + a.getName() + ")");
            }
        });
        for (var nombre : tiposAlcanzables(operacion)) {
            if (campos(objeto(nombre)).stream().anyMatch(c -> c.equalsIgnoreCase("negocioId"))) {
                mal.add(nombre + ".negocioId");
            }
        }
    }

    @Test
    @DisplayName("PU-03: los tipos publicos no exponen contacto del dueno, estado interno ni version")
    void tiposPublicos_noExponenCamposPrivados() {
        var prohibidos = Set.of("telefonoContacto", "correoContacto", "solicitadoPor", "cuentaId", "empleados",
                "equipo", "version", "suspendidoPor", "estado", "contactoPublico", "esSimulado", "visitanteId",
                "responsabilidadDePersonalAceptada", "actualizadoEn");
        var publicos = new HashSet<String>();
        publicos.addAll(tiposAlcanzables(esquema.getQueryType().getFieldDefinition("negocioPublico")));
        publicos.addAll(tiposAlcanzables(esquema.getQueryType().getFieldDefinition("negociosPublicos")));
        publicos.addAll(tiposAlcanzables(esquema.getQueryType().getFieldDefinition("resenasDeNegocio")));

        assertThat(publicos).contains("NegocioPublico", "ResenaPublica");
        for (var nombre : publicos) {
            assertThat(campos(objeto(nombre))).as("campos de " + nombre).doesNotContainAnyElementsOf(prohibidos);
        }
    }

    @Test
    @DisplayName("PU-03: ningun tipo de objeto se comparte entre el panel y la superficie publica")
    void tiposPublicos_noSeComparten_conElPanel() {
        var publicos = new HashSet<>(tiposAlcanzables(esquema.getQueryType().getFieldDefinition("negociosPublicos")));
        publicos.addAll(tiposAlcanzables(esquema.getQueryType().getFieldDefinition("negocioPublico")));
        publicos.addAll(tiposAlcanzables(esquema.getQueryType().getFieldDefinition("resenasDeNegocio")));
        var panel = new HashSet<>(tiposAlcanzables(esquema.getQueryType().getFieldDefinition("miNegocio")));
        panel.addAll(tiposAlcanzables(esquema.getQueryType().getFieldDefinition("horarioDeSucursal")));
        panel.addAll(tiposAlcanzables(esquema.getQueryType().getFieldDefinition("sucursales")));
        panel.addAll(tiposAlcanzables(esquema.getQueryType().getFieldDefinition("resenas")));

        publicos.retainAll(panel);

        assertThat(publicos).isEmpty();
    }

    @Test
    @DisplayName("SG-07 y PU-05: negociosPublicos pide 20 por defecto, no acepta null y el tope esta en codigo")
    void negociosPublicos_primerosPorDefecto_esVeinte() {
        var argumento = esquema.getQueryType().getFieldDefinition("negociosPublicos").getArgument("primeros");

        assertThat(GraphQLTypeUtil.isNonNull(argumento.getType())).isTrue();
        var valorPorDefecto = (IntValue) argumento.getArgumentDefaultValue().getValue();
        assertThat(valorPorDefecto.getValue().intValue()).isEqualTo(LimitesPublicos.POR_PAGINA_INICIAL);
        assertThat(LimitesPublicos.POR_PAGINA_MAXIMA).isEqualTo(20);
        assertThat(LimitesPublicos.RADIO_MAXIMO_KM).isEqualTo(25);
        assertThat(LimitesPublicos.TEXTO_MAXIMO).isEqualTo(80);
    }

    @Test
    @DisplayName("NG-13 y DM-01: actualizarNegocio no acepta giro, estado, publicado ni esSimulado")
    void actualizarNegocio_noAceptaCamposProtegidos() {
        var entrada = (GraphQLInputObjectType) esquema.getType("ActualizarNegocioInput");
        var nombres = entrada.getFieldDefinitions().stream().map(GraphQLInputObjectField::getName).toList();

        assertThat(nombres).contains("version", "nombre", "descripcion", "telefonoContacto", "correoContacto",
                "contactoPublico");
        assertThat(nombres).doesNotContain("giro", "estado", "publicado", "esSimulado", "suspendidoPor", "id");
    }

    @Test
    @DisplayName("NG-10: toda edicion lleva la version que la persona vio")
    void ediciones_llevanVersion() {
        for (var nombre : new String[] {"ActualizarNegocioInput", "ActualizarSucursalInput", "DefinirAtributosInput"}) {
            var entrada = (GraphQLInputObjectType) esquema.getType(nombre);
            var version = entrada.getFieldDefinition("version");
            assertThat(version).as(nombre).isNotNull();
            assertThat(GraphQLTypeUtil.isNonNull(version.getType())).as(nombre).isTrue();
        }
    }

    @Test
    @DisplayName("RS-08: ningun tipo de reseña, del panel o publico, expone al autor ni su dispositivo")
    void resenas_sinAutor() {
        for (var nombre : new String[] {"Resena", "ResenaPublica"}) {
            assertThat(campos(objeto(nombre))).as(nombre).doesNotContain("visitanteId", "dispositivo", "autor",
                    "respondidaPor", "origen");
        }
    }

    @Test
    @DisplayName("RS-09 y RS-11: publicar una reseña es una variante de ComandoInput (preparar y confirmar), no una mutacion directa")
    void publicarResena_esPrepararYConfirmar() {
        var comando = (GraphQLInputObjectType) esquema.getType("ComandoInput");

        assertThat(comando.getFieldDefinitions().stream().map(GraphQLInputObjectField::getName)).contains("publicarResena");
        assertThat(campos(esquema.getMutationType())).doesNotContain("publicarResena", "ocultarResena");
        assertThat(campos(esquema.getQueryType())).doesNotContain("ocultarResena");
    }

    @Test
    @DisplayName("PU-05: las reseñas publicas piden 10 por defecto y no aceptan null")
    void resenasPublicas_primerosPorDefecto() {
        var argumento = esquema.getQueryType().getFieldDefinition("resenasDeNegocio").getArgument("primeros");

        assertThat(GraphQLTypeUtil.isNonNull(argumento.getType())).isTrue();
        assertThat(((IntValue) argumento.getArgumentDefaultValue().getValue()).getValue().intValue()).isEqualTo(10);
    }
}
