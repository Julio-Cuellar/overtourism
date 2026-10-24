package mx.negocio360.restaurantes.web;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.schema.GraphQLFieldDefinition;
import graphql.schema.GraphQLFieldsContainer;
import graphql.schema.GraphQLObjectType;
import graphql.schema.GraphQLSchema;
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
import mx.negocio360.compartido.graphql.ComandoDeclarado;
import mx.negocio360.compartido.graphql.OperacionDeclarada;
import mx.negocio360.compartido.graphql.OperacionesDePlataforma;
import mx.negocio360.compartido.graphql.OperacionesGraphql;
import mx.negocio360.compartido.graphql.TablaDeOperaciones;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/** Contrato del esquema GraphQL de Restaurantes (R1): operaciones, nada de negocioId en el panel y nada interno en lo publico. */
class EsquemaGraphqlRestaurantesTest {

    private static GraphQLSchema esquema;

    @BeforeAll
    static void construirEsquema() throws IOException {
        var parser = new SchemaParser();
        var registro = new TypeDefinitionRegistry();
        var recursos = new PathMatchingResourcePatternResolver().getResources("classpath*:graphql/**/*.graphqls");
        recursos = Arrays.stream(recursos).filter(r -> "compartido.graphqls".equals(r.getFilename())
                || "restaurantes.graphqls".equals(r.getFilename())).toArray(org.springframework.core.io.Resource[]::new);
        assertThat(recursos).as("archivos .graphqls de compartido y restaurantes").hasSize(2);
        for (var recurso : recursos) {
            try (var lector = new InputStreamReader(recurso.getInputStream(), StandardCharsets.UTF_8)) {
                registro.merge(parser.parse(lector));
            }
        }
        // ComandoInput lo define el despachador de comandos de la plataforma (D-125); aqui basta un tipo vacio para armar el esquema.
        registro.merge(parser.parse("input ComandoInput { _sinVariantes: String }"));
        esquema = new SchemaGenerator().makeExecutableSchema(registro, RuntimeWiring.MOCKED_WIRING);
    }

    private static Set<String> campos(GraphQLFieldsContainer tipo) {
        return tipo.getFieldDefinitions().stream().map(GraphQLFieldDefinition::getName).collect(Collectors.toSet());
    }

    private static GraphQLObjectType objeto(String nombre) {
        return (GraphQLObjectType) esquema.getType(nombre);
    }

    @Test
    @DisplayName("las consultas y mutaciones del esquema son exactamente las que Restaurantes declara hasta R4")
    void operaciones() {
        assertThat(campos(esquema.getQueryType())).containsExactlyInAnyOrder("categoriasDeMenu", "platillos", "platillo",
                "resumenDeMenu", "menuPublico", "receta", "costoDeReceta", "consumosFallidos", "salon", "qrDeMesa");
        assertThat(campos(esquema.getMutationType())).contains("crearCategoria", "editarCategoria", "crearPlatillo",
                "editarPlatillo", "marcarAgotado", "publicarPlatillo", "cambiarModoDeInventario", "desactivarReceta",
                "crearZona", "darDeBajaZona", "crearMesa", "editarMesa", "darDeBajaMesa", "bloquearMesa", "abrirServicio",
                "cerrarServicio", "configurarModoDeServicio", "regenerarQrDeMesa", "pausarQrDeMesa", "habilitarQr");
    }

    @Test
    @DisplayName("SG-02: ninguna operacion del panel recibe el negocio como argumento; solo menuPublico, que es publico por diseno")
    void sinNegocioIdEnElPanel() {
        for (var campo : esquema.getQueryType().getFieldDefinitions()) {
            if (!campo.getName().equals("menuPublico")) {
                assertThat(campo.getArguments()).as(campo.getName()).noneMatch(a -> a.getName().equalsIgnoreCase("negocioId"));
            }
        }
        for (var campo : esquema.getMutationType().getFieldDefinitions()) {
            assertThat(campo.getArguments()).as(campo.getName()).noneMatch(a -> a.getName().equalsIgnoreCase("negocioId"));
        }
        assertThat(esquema.getQueryType().getFieldDefinition("menuPublico").getArguments()).extracting("name")
                .containsExactlyInAnyOrder("negocioId", "idioma");
    }

    @Test
    @DisplayName("PB-03: los tipos del menu publico solo tienen los campos permitidos")
    void camposPublicos() {
        assertThat(campos(objeto("RestMenuPublico"))).containsExactlyInAnyOrder("negocioId", "etiqueta", "idioma",
                "categorias", "sinCategoria", "actualizadoEn", "aviso");
        assertThat(campos(objeto("RestCategoriaPublica"))).containsExactlyInAnyOrder("id", "nombre", "platillos");
        assertThat(campos(objeto("RestPlatilloPublico"))).containsExactlyInAnyOrder("id", "nombre", "descripcion", "precio",
                "etiquetas", "agotado", "traducido");
    }

    @Test
    @DisplayName("el platillo del panel no expone el negocio ni datos internos")
    void platilloDelPanel() {
        assertThat(campos(objeto("RestPlatillo"))).containsExactlyInAnyOrder("id", "nombre", "descripcion", "precio",
                "categoriaId", "modo", "agotado", "temporadaDesde", "temporadaHasta", "publicado", "activo", "etiquetas",
                "tieneReceta", "ordenableHoy", "version");
    }

    @Test
    @DisplayName("RC-10: la receta que ve quien tiene receta.ver no tiene ningun campo de costo")
    void recetaSinCostos() {
        assertThat(campos(objeto("RestReceta"))).containsExactlyInAnyOrder("platilloId", "version", "rendimiento", "activa",
                "vigenteDesde", "insumos");
        assertThat(campos(objeto("RestInsumo"))).containsExactlyInAnyOrder("articuloId", "nombre", "cantidad", "unidad",
                "mermaPct");
    }

    @Test
    @DisplayName("RC-05 y RC-06: el costo teorico trae el costo de cada insumo, el margen y los insumos sin costo")
    void costoDeReceta() {
        assertThat(campos(objeto("RestCostoReceta"))).containsExactlyInAnyOrder("platilloId", "version", "rendimiento",
                "precio", "costoPorPorcion", "margen", "margenPct", "incompleto", "insumosSinCosto", "insumos");
        assertThat(campos(objeto("RestCostoInsumo"))).containsExactlyInAnyOrder("articuloId", "nombre", "cantidadBase",
                "unidadBase", "mermaPct", "cpp", "costo");
    }

    @Test
    @DisplayName("CM-06 y CM-08: el consumo fallido trae el id de la venta, el codigo, su texto y los platillos, y nada mas")
    void consumoFallido() {
        assertThat(campos(objeto("RestConsumoFallido"))).containsExactlyInAnyOrder("id", "ventaId", "codigo", "mensaje",
                "lineas", "ocurridoEn", "creadoEn");
        assertThat(campos(objeto("RestLineaFallida"))).containsExactlyInAnyOrder("platilloId", "cantidad");
        assertThat(campos(objeto("RestConsumoFallidoConexion"))).containsExactlyInAnyOrder("nodos", "siguiente");
    }

    @Test
    @DisplayName("D-209: ComandoInput gana guardarReceta y darDeBajaPlatillo, y sus entradas no llevan el negocio (SG-02)")
    void variantesDeComando() {
        var comando = (graphql.schema.GraphQLInputObjectType) esquema.getType("ComandoInput");
        assertThat(comando.getFieldDefinitions()).extracting("name").contains("guardarReceta", "darDeBajaPlatillo");

        var receta = (graphql.schema.GraphQLInputObjectType) esquema.getType("RestGuardarRecetaInput");
        var insumo = (graphql.schema.GraphQLInputObjectType) esquema.getType("RestRecetaInsumoInput");
        var baja = (graphql.schema.GraphQLInputObjectType) esquema.getType("RestBajaPlatilloInput");
        assertThat(receta.getFieldDefinitions()).extracting("name").containsExactlyInAnyOrder("platilloId", "rendimiento",
                "insumos");
        assertThat(insumo.getFieldDefinitions()).extracting("name").containsExactlyInAnyOrder("articuloId", "cantidad",
                "unidad", "mermaPct");
        assertThat(baja.getFieldDefinitions()).extracting("name").containsExactly("platilloId");
    }

    @Test
    @DisplayName("SG-21: con las operaciones de la plataforma y las de Restaurantes, la tabla coincide con el esquema")
    void tablaDeOperaciones() {
        List<PermisoDeclarado> catalogo = new PermisosDeRestaurantes().permisos();
        // La variante de relleno del tipo ComandoInput de esta prueba se declara como publica solo para que la tabla cuadre.
        OperacionesGraphql relleno = new OperacionesGraphql() {
            @Override
            public List<OperacionDeclarada> operaciones() {
                return List.of();
            }

            @Override
            public List<ComandoDeclarado> comandos() {
                return List.of(ComandoDeclarado.publico("_sinVariantes"));
            }
        };
        var tabla = new TablaDeOperaciones(List.of(new OperacionesDePlataforma(), new OperacionesDeRestaurantes(), relleno),
                catalogo);

        tabla.verificar(esquema);
    }
}
