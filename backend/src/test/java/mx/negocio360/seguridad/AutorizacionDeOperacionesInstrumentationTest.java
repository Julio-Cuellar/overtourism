package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.schema.idl.RuntimeWiring;
import graphql.schema.idl.SchemaGenerator;
import graphql.schema.idl.SchemaParser;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import mx.negocio360.compartido.graphql.AutorizacionDeOperacionesInstrumentation;
import mx.negocio360.compartido.graphql.ComandoDeclarado;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.graphql.OperacionDeclarada;
import mx.negocio360.compartido.graphql.OperacionesGraphql;
import mx.negocio360.compartido.graphql.TablaDeOperaciones;
import mx.negocio360.compartido.graphql.TipoDeOperacion;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SG-03: la puerta de autorizacion sola, sobre un esquema minimo. Solo vigila los campos de la raiz; un campo de la raiz
 * que la tabla no conoce se niega (falla cerrado) aunque el arranque no lo haya detectado.
 */
class AutorizacionDeOperacionesInstrumentationTest {

    private final AtomicInteger ejecuciones = new AtomicInteger();

    private GraphQL motor() {
        var sdl = "type Query { publico: String protegido: String sinEntrada: String hijo: Hijo abierto: String "
                + "deCuenta: String } "
                + "type Mutation { comando(input: Cmd!): String } "
                + "input Cmd @oneOf { abierta: String cerrada: String } "
                + "type Hijo { dato: String secreto: String }";
        var cableado = RuntimeWiring.newRuntimeWiring().type("Query", tipo -> tipo
                        .dataFetcher("publico", entorno -> contar("p")).dataFetcher("protegido", entorno -> contar("x"))
                        .dataFetcher("sinEntrada", entorno -> contar("s"))
                        .dataFetcher("abierto", entorno -> contar("o")).dataFetcher("deCuenta", entorno -> contar("c"))
                        .dataFetcher("hijo", entorno -> Map.of("dato", "d", "secreto", "s")))
                .type("Mutation", tipo -> tipo.dataFetcher("comando", entorno -> contar("m")))
                .build();
        var esquema = new SchemaGenerator().makeExecutableSchema(new SchemaParser().parse(sdl), cableado);
        var operaciones = List.of(
                OperacionDeclarada.deSesion(TipoDeOperacion.QUERY, "publico"),
                OperacionDeclarada.de(TipoDeOperacion.QUERY, "protegido", "prueba.recurso.ver"),
                OperacionDeclarada.deSesion(TipoDeOperacion.QUERY, "hijo"),
                OperacionDeclarada.publica(TipoDeOperacion.QUERY, "abierto"),
                OperacionDeclarada.deCuenta(TipoDeOperacion.QUERY, "deCuenta"),
                OperacionDeclarada.porComando(TipoDeOperacion.MUTATION, "comando", "input"));
        var comandos = List.of(ComandoDeclarado.publico("abierta"),
                new ComandoDeclarado("cerrada", "prueba.recurso.ver"));
        var fuente = new OperacionesGraphql() {
            @Override
            public List<OperacionDeclarada> operaciones() {
                return operaciones;
            }

            @Override
            public List<ComandoDeclarado> comandos() {
                return comandos;
            }
        };
        var tabla = new TablaDeOperaciones(List.of(fuente),
                List.of(new PermisoDeclarado("prueba.recurso.ver", "Ver", false)));
        return GraphQL.newGraphQL(esquema)
                .instrumentation(new AutorizacionDeOperacionesInstrumentation(tabla, (cuenta, negocio, permiso) -> false)).build();
    }

    private String contar(String valor) {
        ejecuciones.incrementAndGet();
        return valor;
    }

    /** Con la entrada actual de la prueba en el contexto de GraphQL, como la deja el interceptor de entrada. */
    private ExecutionResult ejecutar(String consulta) {
        return motor().execute(graphql.ExecutionInput.newExecutionInput().query(consulta)
                .graphQLContext(SesionDePrueba.contextoGraphql()).build());
    }

    /** Sin ninguna entrada en el contexto: un motor ejecutado sin los interceptores de la plataforma. */
    private ExecutionResult ejecutarSinEntrada(String consulta) {
        return motor().execute(consulta);
    }

    @AfterEach
    void limpiar() {
        SesionDePrueba.salir();
    }

    @Test
    @DisplayName("la entrada se toma del contexto de GraphQL, sin depender de ningun hilo (como en un WebSocket)")
    void entradaDelContexto_sinDependerDelHilo() {
        var conPermiso = SesionDePrueba.entrada(SesionDePrueba.CUENTA, SesionDePrueba.NEGOCIO, "prueba.recurso.ver");

        var resultado = motor().execute(graphql.ExecutionInput.newExecutionInput().query("{ protegido }")
                .graphQLContext(Map.of(EntradaGraphQl.CLAVE, conPermiso)).build());

        assertThat(resultado.getErrors()).isEmpty();
        assertThat(ejecuciones).hasValue(1);
    }

    @Test
    @DisplayName("sin ninguna entrada en el contexto se niega (falla cerrado) y el resolvedor no se ejecuta")
    void sinEntradaEnElContexto_seNiega() {
        SesionDePrueba.entrar("prueba.recurso.ver");

        var resultado = ejecutarSinEntrada("{ protegido }");

        assertThat(resultado.getErrors()).hasSize(1);
        assertThat(ejecuciones).hasValue(0);
    }

    @Test
    @DisplayName("una sesion sin negocio activo se niega")
    void sesionSinNegocio_seNiega() {
        SesionDePrueba.entrarSinNegocio();

        assertThat(ejecutar("{ publico }").getErrors()).hasSize(1);
        assertThat(ejecuciones).hasValue(0);
    }

    @Test
    @DisplayName("una entrada anonima se niega en los campos que piden sesion")
    void entradaAnonima_seNiega() {
        SesionDePrueba.entrarAnonimo("0192f39f-3333-7000-8000-00000000c001");

        assertThat(ejecutar("{ publico }").getErrors()).hasSize(1);
        assertThat(ejecutar("{ protegido }").getErrors()).hasSize(1);
        assertThat(ejecuciones).hasValue(0);
    }

    @Test
    @DisplayName("un campo de la raiz que la tabla no conoce se niega y su resolvedor no se ejecuta")
    void campoSinEntrada_seNiega() {
        SesionDePrueba.entrar("prueba.recurso.ver");

        var resultado = ejecutar("{ sinEntrada }");

        assertThat(resultado.getErrors()).hasSize(1);
        assertThat(ejecuciones).hasValue(0);
    }

    @Test
    @DisplayName("sin sesion se niega aunque el campo solo pida sesion")
    void sinSesion_seNiega() {
        var resultado = ejecutar("{ publico }");

        assertThat(resultado.getErrors()).hasSize(1);
        assertThat(ejecuciones).hasValue(0);
    }

    @Test
    @DisplayName("un campo de sesion pasa con cualquier sesion con negocio; el protegido pide su permiso")
    void sesionYPermiso() {
        SesionDePrueba.entrar();
        assertThat(ejecutar("{ publico }").getErrors()).isEmpty();
        assertThat(ejecutar("{ protegido }").getErrors()).hasSize(1);

        SesionDePrueba.entrar("prueba.recurso.ver");
        assertThat(ejecutar("{ protegido }").getErrors()).isEmpty();
    }

    @Test
    @DisplayName("los campos anidados no se vigilan: la autorizacion es de la operacion, no de cada campo del arbol")
    void camposAnidados_noSeVigilan() {
        SesionDePrueba.entrar();

        var resultado = ejecutar("{ hijo { dato secreto } }");

        assertThat(resultado.getErrors()).isEmpty();
        assertThat(resultado.<Map<String, Map<String, String>>>getData().get("hijo")).containsEntry("dato", "d");
    }

    @Test
    @DisplayName("__typename en la raiz pasa con sesion y no necesita entrada")
    void typename_pasaConSesion() {
        SesionDePrueba.entrar();

        assertThat(ejecutar("{ __typename }").getErrors()).isEmpty();
    }

    @Test
    @DisplayName("un campo negado no impide los permitidos de la misma consulta (resultado parcial)")
    void resultadoParcial() {
        SesionDePrueba.entrar();

        var resultado = ejecutar("{ publico protegido }");

        assertThat(resultado.getErrors()).hasSize(1);
        assertThat(resultado.<Map<String, Object>>getData()).containsEntry("publico", "p");
    }

    // --- Acceso publico y de cuenta ---

    /** La excepcion que hizo fallar la primera operacion, tal como la deja el motor sin los resolvedores de la plataforma. */
    private static Throwable causa(ExecutionResult resultado) {
        assertThat(resultado.getErrors()).hasSize(1);
        var error = resultado.getErrors().get(0);
        assertThat(error).isInstanceOf(graphql.ExceptionWhileDataFetching.class);
        return ((graphql.ExceptionWhileDataFetching) error).getException();
    }

    private static String codigoDeLaCausa(ExecutionResult resultado) {
        var causa = causa(resultado);
        return causa instanceof mx.negocio360.compartido.seguridad.NoAutenticadoException e ? e.codigo()
                : causa.getClass().getSimpleName();
    }

    @Test
    @DisplayName("una operacion publica se ejecuta sin sesion, con sesion y con sesion sin negocio")
    void publica_nadieLaNecesita() {
        assertThat(ejecutar("{ abierto }").getErrors()).isEmpty();

        SesionDePrueba.entrarAnonimo("0192f39f-3333-7000-8000-00000000c001");
        assertThat(ejecutar("{ abierto }").getErrors()).isEmpty();

        SesionDePrueba.entrarSinNegocio();
        assertThat(ejecutar("{ abierto }").getErrors()).isEmpty();

        SesionDePrueba.entrar();
        assertThat(ejecutar("{ abierto }").getErrors()).isEmpty();
        assertThat(ejecuciones).hasValue(4);
    }

    @Test
    @DisplayName("una operacion publica no abre las demas: sin sesion, lo protegido sigue negado con NO_AUTENTICADO")
    void publica_noAbreLoDemas() {
        SesionDePrueba.entrarAnonimo(null);

        assertThat(codigoDeLaCausa(ejecutar("{ protegido }"))).isEqualTo("NO_AUTENTICADO");
        assertThat(codigoDeLaCausa(ejecutar("{ publico }"))).isEqualTo("NO_AUTENTICADO");
        assertThat(codigoDeLaCausa(ejecutar("{ hijo { dato } }"))).isEqualTo("NO_AUTENTICADO");
        assertThat(ejecuciones).hasValue(0);
    }

    @Test
    @DisplayName("una operacion de cuenta pide sesion, pero no negocio activo")
    void deCuenta_pideSesionSinNegocio() {
        SesionDePrueba.entrarSinNegocio();
        assertThat(ejecutar("{ deCuenta }").getErrors()).isEmpty();

        SesionDePrueba.entrar();
        assertThat(ejecutar("{ deCuenta }").getErrors()).isEmpty();
        assertThat(ejecuciones).hasValue(2);
    }

    @Test
    @DisplayName("una operacion de cuenta sin sesion se niega con NO_AUTENTICADO y no se ejecuta")
    void deCuenta_sinSesion_seNiega() {
        assertThat(codigoDeLaCausa(ejecutar("{ deCuenta }"))).isEqualTo("NO_AUTENTICADO");

        SesionDePrueba.entrarAnonimo("0192f39f-3333-7000-8000-00000000c001");
        assertThat(codigoDeLaCausa(ejecutar("{ deCuenta }"))).isEqualTo("NO_AUTENTICADO");
        assertThat(ejecuciones).hasValue(0);
    }

    @Test
    @DisplayName("una sesion sin negocio activo se niega con SIN_NEGOCIO_ACTIVO en lo que pide negocio")
    void sinNegocio_codigoPropio() {
        SesionDePrueba.entrarSinNegocio();

        assertThat(codigoDeLaCausa(ejecutar("{ publico }"))).isEqualTo("SIN_NEGOCIO_ACTIVO");
        assertThat(ejecuciones).hasValue(0);
    }

    @Test
    @DisplayName("una variante de comando publica se ejecuta sin sesion; la que pide permiso, no")
    void varianteDeComando_publicaVsProtegida() {
        SesionDePrueba.entrarAnonimo("0192f39f-3333-7000-8000-00000000c001");

        assertThat(ejecutar("mutation { comando(input: {abierta: \"x\"}) }").getErrors()).isEmpty();
        assertThat(ejecuciones).hasValue(1);

        assertThat(codigoDeLaCausa(ejecutar("mutation { comando(input: {cerrada: \"x\"}) }")))
                .isEqualTo("NO_AUTENTICADO");
        assertThat(ejecuciones).hasValue(1);
    }

    @Test
    @DisplayName("con sesion, la variante protegida pide su permiso; con el, pasa")
    void varianteProtegida_conSesion() {
        SesionDePrueba.entrar();
        assertThat(causa(ejecutar("mutation { comando(input: {cerrada: \"x\"}) }")))
                .isInstanceOf(mx.negocio360.compartido.seguridad.PermisoDenegadoException.class);

        SesionDePrueba.entrar("prueba.recurso.ver");
        assertThat(ejecutar("mutation { comando(input: {cerrada: \"x\"}) }").getErrors()).isEmpty();
    }

    @Test
    @DisplayName("un campo de la raiz que la tabla no conoce, sin sesion, dice NO_AUTENTICADO y no revela que no esta declarado")
    void campoSinEntrada_anonimo_noSeRevela() {
        SesionDePrueba.entrarAnonimo(null);

        assertThat(codigoDeLaCausa(ejecutar("{ sinEntrada }"))).isEqualTo("NO_AUTENTICADO");
    }
}
