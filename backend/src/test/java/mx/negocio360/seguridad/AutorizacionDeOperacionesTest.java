package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.ExecutionResult;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.graphql.server.WebGraphQlHandler;
import org.springframework.graphql.server.WebGraphQlRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.util.LinkedMultiValueMap;

/**
 * SG-03, SG-04 y SG-19 contra el cableado real: la tabla de operaciones de Clientes, los permisos de verdad y la
 * confirmacion reforzada que todavia no existe (IDN-3.2), por lo que toda operacion sensible se niega por omision.
 */
@SpringBootTest
class AutorizacionDeOperacionesTest {

    @Autowired
    private WebGraphQlHandler manejador;

    @BeforeEach
    void contar() {
        ContextoDePruebaControlador.EJECUCIONES_PROTEGIDAS.set(0);
    }

    @AfterEach
    void limpiar() {
        SesionDePrueba.salir();
    }

    private ExecutionResult ejecutar(String consulta, Map<String, Object> variables) {
        var cuerpo = Map.<String, Object>of("query", consulta, "variables", variables);
        var peticion = new WebGraphQlRequest(URI.create("/graphql"), new HttpHeaders(), new LinkedMultiValueMap<>(),
                null, SesionDePrueba.atributos(), cuerpo, "prueba", Locale.ROOT);
        return manejador.handleRequest(peticion).block().getExecutionResult();
    }

    private ExecutionResult ejecutar(String consulta) {
        return ejecutar(consulta, Map.of());
    }

    private static String codigo(ExecutionResult resultado) {
        assertThat(resultado.getErrors()).hasSize(1);
        return (String) resultado.getErrors().get(0).getExtensions().get("codigo");
    }

    private static int ejecuciones() {
        return ContextoDePruebaControlador.EJECUCIONES_PROTEGIDAS.get();
    }

    // --- Un campo con permiso ---

    @Test
    @DisplayName("sin sesion: NO_AUTENTICADO")
    void sinSesion_noAutenticado() {
        assertThat(codigo(ejecutar("{ soloConPermisoDePrueba }"))).isEqualTo("NO_AUTENTICADO");
        assertThat(ejecuciones()).isZero();
    }

    @Test
    @DisplayName("una sesion sin negocio activo no puede operar: SIN_NEGOCIO_ACTIVO, y el resolvedor no se ejecuta")
    void sesionSinNegocio_sinNegocioActivo() {
        SesionDePrueba.entrarSinNegocio();

        assertThat(codigo(ejecutar("{ arbolDePrueba { valor } }"))).isEqualTo("SIN_NEGOCIO_ACTIVO");
        assertThat(codigo(ejecutar("{ soloConPermisoDePrueba }"))).isEqualTo("SIN_NEGOCIO_ACTIVO");
        assertThat(ejecuciones()).isZero();
    }

    @Test
    @DisplayName("una peticion anonima (con id de dispositivo) tampoco opera en lo que pide negocio: NO_AUTENTICADO")
    void anonima_noAutenticado() {
        SesionDePrueba.entrarAnonimo("0192f39f-3333-7000-8000-00000000c001");

        assertThat(codigo(ejecutar("{ arbolDePrueba { valor } }"))).isEqualTo("NO_AUTENTICADO");
        assertThat(codigo(ejecutar("{ soloConPermisoDePrueba }"))).isEqualTo("NO_AUTENTICADO");
        assertThat(ejecuciones()).isZero();
    }

    @Test
    @DisplayName("con el permiso pasa y se ejecuta; sin el, PERMISO_DENEGADO sin ejecutar y sin decir cual faltaba")
    void conYSinPermiso() {
        SesionDePrueba.entrar("clientes.sugerir");
        var permitida = ejecutar("{ soloConPermisoDePrueba }");
        assertThat(permitida.getErrors()).isEmpty();
        assertThat(ejecuciones()).isEqualTo(1);

        SesionDePrueba.entrar("clientes.lista.ver", "clientes.ficha.ver");
        var negada = ejecutar("{ soloConPermisoDePrueba }");
        assertThat(codigo(negada)).isEqualTo("PERMISO_DENEGADO");
        assertThat(negada.toSpecification().toString()).doesNotContain("clientes.sugerir", "sugerir");
        assertThat(ejecuciones()).isEqualTo(1);
    }

    // --- Operaciones sensibles (SG-19) ---

    @Test
    @DisplayName("SG-19: una operacion sensible con el permiso se niega por omision: CONFIRMACION_REQUERIDA, sin ejecutar")
    void sensibleConPermiso_requiereConfirmacion() {
        SesionDePrueba.entrar("clientes.cliente.anonimizar");

        var resultado = ejecutar("{ sensibleDePrueba }");

        assertThat(codigo(resultado)).isEqualTo("CONFIRMACION_REQUERIDA");
        assertThat(ejecuciones()).isZero();
    }

    @Test
    @DisplayName("SG-19: sin el permiso sale PERMISO_DENEGADO, no CONFIRMACION_REQUERIDA (no se revela que existe el flujo)")
    void sensibleSinPermiso_permisoDenegado() {
        SesionDePrueba.entrar("clientes.sugerir");

        assertThat(codigo(ejecutar("{ sensibleDePrueba }"))).isEqualTo("PERMISO_DENEGADO");
    }

    // --- Comandos por variante ---

    private static Map<String, Object> comando(String variante) {
        var id = "0192f3a0-7c1e-7b52-9d41-5a1c3e8f2b10";
        return Map.of(variante, switch (variante) {
            case "registrarCliente" -> Map.of("nombre", "Ana", "avisoVersion", 1);
            case "editarCliente" -> Map.of("clienteId", id, "version", 1);
            case "registrarConsentimiento" -> Map.of("clienteId", id, "finalidad", "PROMOCIONES", "estado", "OTORGADO",
                    "medio", "VERBAL", "avisoVersion", 1);
            case "anonimizarCliente" -> Map.of("clienteId", id);
            case "publicarAvisoPrivacidad" -> Map.of("responsableNombre", "x", "responsableDomicilio", "y",
                    "finalidades", List.of(Map.of("codigo", "A", "requiereConsentimiento", false)),
                    "textoSimplificado", "s", "textoIntegral", "t", "contactoArco", "c");
            case "registrarSolicitudArco" -> Map.of("derecho", "ACCESO", "recibidaEn", "2026-10-16", "medio", "PRESENCIAL");
            case "ampliarSolicitudArco" -> Map.of("solicitudId", id, "justificacion", "j");
            case "resolverSolicitudArco" -> Map.of("solicitudId", id, "resultado", "PROCEDENTE",
                    "identidadVerificada", true);
            default -> throw new IllegalArgumentException(variante);
        });
    }

    /** Variante, permiso que exige y si es sensible. */
    static Stream<Arguments> variantes() {
        return Stream.of(
                Arguments.of("registrarCliente", "clientes.cliente.registrar", false),
                Arguments.of("editarCliente", "clientes.cliente.editar", false),
                Arguments.of("registrarConsentimiento", "clientes.consentimiento.registrar", false),
                Arguments.of("anonimizarCliente", "clientes.cliente.anonimizar", true),
                Arguments.of("publicarAvisoPrivacidad", "clientes.aviso.publicar", true),
                Arguments.of("registrarSolicitudArco", "clientes.arco.gestionar", true),
                Arguments.of("ampliarSolicitudArco", "clientes.arco.gestionar", true),
                Arguments.of("resolverSolicitudArco", "clientes.arco.gestionar", true));
    }

    private static final String COMANDO = "mutation($c: ComandoInput!) { comandoDePrueba(input: $c) }";
    private static final List<String> TODOS_LOS_PERMISOS_DE_CLIENTES = List.of("clientes.sugerir",
            "clientes.lista.ver", "clientes.panorama.ver", "clientes.ficha.ver", "clientes.cliente.registrar",
            "clientes.cliente.editar", "clientes.consentimiento.registrar", "clientes.aviso.publicar",
            "clientes.cliente.anonimizar", "clientes.arco.gestionar", "clientes.datos.exportar");

    @ParameterizedTest(name = "{0} pide {1}")
    @MethodSource("variantes")
    @DisplayName("cada variante de comando pide su permiso; las sensibles ademas la confirmacion reforzada")
    void variante_conSuPermiso(String variante, String permiso, boolean sensible) {
        SesionDePrueba.entrar(permiso);

        var resultado = ejecutar(COMANDO, Map.of("c", comando(variante)));

        if (sensible) {
            assertThat(codigo(resultado)).isEqualTo("CONFIRMACION_REQUERIDA");
            assertThat(ejecuciones()).isZero();
        } else {
            assertThat(resultado.getErrors()).isEmpty();
            assertThat(ejecuciones()).isEqualTo(1);
        }
    }

    @ParameterizedTest(name = "{0} sin {1}")
    @MethodSource("variantes")
    @DisplayName("con todos los demas permisos de Clientes pero sin el suyo, la variante se niega")
    void variante_sinSuPermiso(String variante, String permiso, boolean sensible) {
        var otros = TODOS_LOS_PERMISOS_DE_CLIENTES.stream().filter(p -> !p.equals(permiso)).toArray(String[]::new);
        SesionDePrueba.entrar(otros);

        var resultado = ejecutar(COMANDO, Map.of("c", comando(variante)));

        assertThat(codigo(resultado)).isEqualTo("PERMISO_DENEGADO");
        assertThat(ejecuciones()).isZero();
    }

    // --- Las consultas reales de Clientes ---

    /** Consulta, permiso que exige y si es sensible. */
    static Stream<Arguments> consultasDeClientes() {
        return Stream.of(
                Arguments.of("{ clientes { __typename } }", "clientes.lista.ver", false),
                Arguments.of("{ cliente(id: \"0192f3a0-7c1e-7b52-9d41-5a1c3e8f2b10\") { __typename } }",
                        "clientes.ficha.ver", false),
                Arguments.of("{ historialCliente(clienteId: \"0192f3a0-7c1e-7b52-9d41-5a1c3e8f2b10\") { __typename } }",
                        "clientes.ficha.ver", false),
                Arguments.of("{ consentimientosCliente(clienteId: \"0192f3a0-7c1e-7b52-9d41-5a1c3e8f2b10\") { __typename } }",
                        "clientes.ficha.ver", false),
                Arguments.of("{ sugerirClientes(texto: \"an\") { __typename } }", "clientes.sugerir", false),
                Arguments.of("{ panoramaClientes { __typename } }", "clientes.panorama.ver", false),
                Arguments.of("{ avisoPrivacidadVigente { __typename } }", "clientes.cliente.registrar", false),
                Arguments.of("{ solicitudesArco { __typename } }", "clientes.arco.gestionar", true));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("consultasDeClientes")
    @DisplayName("cada consulta de Clientes se niega sin su permiso")
    void consultaDeClientes_sinPermiso(String consulta, String permiso, boolean sensible) {
        var otros = TODOS_LOS_PERMISOS_DE_CLIENTES.stream().filter(p -> !p.equals(permiso)).toArray(String[]::new);
        SesionDePrueba.entrar(otros);

        assertThat(codigo(ejecutar(consulta))).isEqualTo("PERMISO_DENEGADO");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("consultasDeClientes")
    @DisplayName("con su permiso, la consulta pasa la puerta; la sensible pide confirmacion reforzada")
    void consultaDeClientes_conPermiso(String consulta, String permiso, boolean sensible) {
        SesionDePrueba.entrar(permiso);

        var resultado = ejecutar(consulta);

        // Clientes aun no tiene resolvedores: lo que importa es que la puerta no la haya negado.
        var codigos = resultado.getErrors().stream().map(e -> (String) e.getExtensions().get("codigo")).toList();
        if (sensible) {
            assertThat(codigos).containsExactly("CONFIRMACION_REQUERIDA");
        } else {
            assertThat(codigos).doesNotContain("PERMISO_DENEGADO", "CONFIRMACION_REQUERIDA", "NO_AUTENTICADO");
        }
    }

    @Test
    @DisplayName("los campos de la plataforma (prepararComando y confirmarComando) piden sesion; el permiso es de cada comando")
    void camposDeLaPlataforma_pidenSoloSesion() {
        SesionDePrueba.entrar();
        var variables = Map.<String, Object>of("c", comando("registrarCliente"));

        var preparar = ejecutar("mutation($c: ComandoInput!) { prepararComando(input: $c) { token } }", variables);
        var confirmar = ejecutar("mutation { confirmarComando(token: \"x\") { ok } }");

        // La puerta de la raiz no los niega; el permiso de cada comando lo decide la tabla de variantes, por eso
        // prepararComando sin el permiso de la variante si se niega.
        assertThat(preparar.getErrors().stream().map(e -> e.getExtensions().get("codigo")))
                .containsExactly("PERMISO_DENEGADO");
        assertThat(confirmar.getErrors().stream().map(e -> e.getExtensions().get("codigo")))
                .doesNotContain("PERMISO_DENEGADO", "NO_AUTENTICADO");
    }

    @Test
    @DisplayName("un resultado parcial: la consulta permitida responde y la negada trae su error")
    void resultadoParcial() {
        SesionDePrueba.entrar();

        var resultado = ejecutar("{ arbolDePrueba { valor } soloConPermisoDePrueba }");

        assertThat(codigo(resultado)).isEqualTo("PERMISO_DENEGADO");
        assertThat(resultado.<Map<String, Object>>getData()).containsKey("arbolDePrueba");
        assertThat(Arrays.asList(resultado.getErrors().get(0).getPath().toArray())).containsExactly("soloConPermisoDePrueba");
    }
}
