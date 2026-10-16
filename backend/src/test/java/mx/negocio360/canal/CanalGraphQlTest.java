package mx.negocio360.canal;

import static org.assertj.core.api.Assertions.assertThat;

import graphql.ExecutionResult;
import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.caja.infraestructura.CajaConfiguracion;
import mx.negocio360.canal.demo.DemoConfiguracion;
import mx.negocio360.canal.web.ComandosController;
import mx.negocio360.canal.web.ConsultasController;
import mx.negocio360.canal.web.ContextoGraphQl;
import mx.negocio360.canal.web.ErroresDeCanal;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.graphql.Escalares;
import mx.negocio360.compartido.pruebas.RelojMutable;
import mx.negocio360.ventas.infraestructura.VentasConfiguracion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.graphql.GraphQlTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.ExecutionGraphQlService;
import org.springframework.graphql.support.DefaultExecutionGraphQlRequest;
import org.springframework.test.context.TestPropertySource;

/**
 * F4 de Ventas y Caja: el panel prepara y confirma comandos y consulta por GraphQL (D-55), con Catalogo, Pasaporte y
 * permisos simulados. Recorre el camino dorado de punta a punta con los modulos reales en memoria y el bus de Spring.
 * Sin base de datos: usa la rebanada de GraphQL de Spring Boot.
 */
@GraphQlTest(controllers = {ComandosController.class, ConsultasController.class})
@Import({Escalares.class, VentasConfiguracion.class, CajaConfiguracion.class, DemoConfiguracion.class,
        ErroresDeCanal.class, CanalGraphQlTest.Reloj.class})
@TestPropertySource(properties = {"negocio360.demo.habilitada=true", "negocio360.pruebas.sin-base=true"})
class CanalGraphQlTest {

    @TestConfiguration
    static class Reloj {
        @Bean
        Clock reloj() {
            return new RelojMutable();
        }
    }

    private static final ContextoNegocio CAJERO = new ContextoNegocio(UUID.randomUUID(), "cuenta-demo");
    private static final String PREPARAR = """
            mutation($input: ComandoInput!) { prepararComando(input: $input) { token resumen expiraEn advertencias } }""";
    private static final String CONFIRMAR = """
            mutation($token: String!) { confirmarComando(token: $token) { ok codigo mensajePlantilla recursoId
              datos { clave valor } } }""";

    @Autowired
    private ExecutionGraphQlService graphQl;

    private ExecutionResult ejecutar(ContextoNegocio contexto, String documento, Map<String, Object> variables) {
        var peticion = new DefaultExecutionGraphQlRequest(documento, null, variables, null, UUID.randomUUID().toString(),
                null);
        if (contexto != null) {
            peticion.configureExecutionInput((entrada, constructor) ->
                    constructor.graphQLContext(Map.of(ContextoGraphQl.CLAVE, contexto)).build());
        }
        return graphQl.execute(peticion).block().getExecutionResult();
    }

    @SuppressWarnings("unchecked")
    private static <T> T dato(ExecutionResult resultado, String... ruta) {
        assertThat(resultado.getErrors()).as("errores de GraphQL").isEmpty();
        Object actual = resultado.getData();
        for (var paso : ruta) {
            actual = ((Map<String, Object>) actual).get(paso);
        }
        return (T) actual;
    }

    private static Map<String, Object> extensiones(ExecutionResult resultado) {
        assertThat(resultado.getErrors()).isNotEmpty();
        return resultado.getErrors().get(0).getExtensions();
    }

    /** Prepara y confirma un comando con el contexto del cajero; devuelve el resultado de confirmar. */
    private Map<String, Object> comando(String variante, Map<String, Object> campos) {
        String token = dato(ejecutar(CAJERO, PREPARAR, Map.of("input", Map.of(variante, campos))),
                "prepararComando", "token");
        return dato(ejecutar(CAJERO, CONFIRMAR, Map.of("token", token)), "confirmarComando");
    }

    private String abrirTurno(String nombreCaja) {
        var caja = (String) comando("crearCaja", Map.of("nombre", nombreCaja)).get("recursoId");
        return (String) comando("abrirTurno", Map.of("cajaId", caja, "fondoInicial", "500.00")).get("recursoId");
    }

    private static String valor(Map<String, Object> resultado, String clave) {
        @SuppressWarnings("unchecked")
        var datos = (List<Map<String, String>>) resultado.get("datos");
        return datos.stream().filter(d -> d.get("clave").equals(clave)).findFirst().orElseThrow().get("valor");
    }

    @Test
    @DisplayName("sin sesion con negocio, cualquier operacion falla con NO_AUTENTICADO")
    void sinContexto_noAutenticado() {
        var resultado = ejecutar(null, "{ cajas { id } }", Map.of());

        assertThat(extensiones(resultado)).containsEntry("codigo", "NO_AUTENTICADO");
    }

    @Test
    @DisplayName("los productos y cupones de demostracion vienen rotulados como simulados")
    void datosDeDemo_rotulados() {
        List<Map<String, Object>> productos = dato(ejecutar(CAJERO,
                "{ productosSimulados { productoId descripcion precio simulado } }", Map.of()), "productosSimulados");
        List<Map<String, Object>> cupones = dato(ejecutar(CAJERO,
                "{ cuponesSimulados { codigo descuento simulado } }", Map.of()), "cuponesSimulados");

        assertThat(productos).isNotEmpty().allSatisfy(p -> assertThat(p).containsEntry("simulado", true));
        assertThat(cupones).isNotEmpty().allSatisfy(c -> assertThat(c).containsEntry("simulado", true));
    }

    @Test
    @DisplayName("camino dorado: caja, turno, venta con cupon, saldo, corte sin diferencia y lista de cortes")
    void caminoDorado() {
        var turno = abrirTurno("Mostrador");
        List<Map<String, Object>> productos = dato(ejecutar(CAJERO, "{ productosSimulados { productoId precio } }",
                Map.of()), "productosSimulados");
        List<Map<String, Object>> cupones = dato(ejecutar(CAJERO, "{ cuponesSimulados { codigo descuento } }",
                Map.of()), "cuponesSimulados");
        var producto = (String) productos.get(0).get("productoId");
        var total = new BigDecimal((String) productos.get(0).get("precio")).multiply(BigDecimal.TWO)
                .subtract(new BigDecimal((String) cupones.get(0).get("descuento"))).setScale(2).toPlainString();

        var venta = comando("registrarVenta", Map.of(
                "turnoId", turno,
                "lineas", List.of(Map.of("productoId", producto, "cantidad", "2")),
                "pagos", List.of(Map.of("metodo", "TARJETA", "monto", total)),
                "codigoCupon", cupones.get(0).get("codigo")));

        assertThat(venta).containsEntry("ok", true).containsEntry("codigo", "OK");
        assertThat(valor(venta, "folio")).isEqualTo("1");
        assertThat(valor(venta, "total")).isEqualTo(total);

        Map<String, Object> saldo = dato(ejecutar(CAJERO, """
                query($t: ID!) { saldoTurno(turnoId: $t) { estado ventasAplicadas esperadoPorMetodo { metodo monto } } }""",
                Map.of("t", turno)), "saldoTurno");
        assertThat(saldo).containsEntry("estado", "ABIERTO").containsEntry("ventasAplicadas", 1);
        assertThat((List<Object>) saldo.get("esperadoPorMetodo")).contains(Map.of("metodo", "TARJETA", "monto", total));

        var corte = comando("cerrarTurno", Map.of("turnoId", turno, "autorizar", false, "contado", List.of(
                Map.of("metodo", "EFECTIVO", "monto", "500.00"),
                Map.of("metodo", "TARJETA", "monto", total))));
        assertThat(corte).containsEntry("ok", true);

        List<Map<String, Object>> cortes = dato(ejecutar(CAJERO, "{ cortes { turnoId diferenciaEfectivo } }",
                Map.of()), "cortes");
        assertThat(cortes).singleElement().satisfies(c -> assertThat(c).containsEntry("diferenciaEfectivo", "0.00"));
    }

    @Test
    @DisplayName("un error de dominio llega con su codigo, su regla y el texto fijo en espanol")
    void errorDeDominio_codigoYPlantilla() {
        var resultado = ejecutar(CAJERO, PREPARAR, Map.of("input", Map.of("registrarVenta", Map.of(
                "turnoId", UUID.randomUUID().toString(),
                "lineas", List.of(Map.of("productoId", UUID.randomUUID().toString(), "cantidad", "1")),
                "pagos", List.of(Map.of("metodo", "TARJETA", "monto", "10.00"))))));

        assertThat(extensiones(resultado)).containsEntry("codigo", "TURNO_NO_ABIERTO").containsEntry("regla", "VT-01");
        assertThat(resultado.getErrors().get(0).getMessage()).isEqualTo("Abre un turno de caja antes de vender.");
    }

    @Test
    @DisplayName("un token que no es de ningun modulo falla con TOKEN_INVALIDO")
    void tokenDesconocido_tokenInvalido() {
        var resultado = ejecutar(CAJERO, CONFIRMAR, Map.of("token", "inventado"));

        assertThat(extensiones(resultado)).containsEntry("codigo", "TOKEN_INVALIDO");
    }

    @Test
    @DisplayName("un comando de Clientes todavia no esta disponible por el canal")
    void comandoDeClientes_noDisponible() {
        var resultado = ejecutar(CAJERO, PREPARAR, Map.of("input", Map.of("anonimizarCliente",
                Map.of("clienteId", UUID.randomUUID().toString()))));

        assertThat(extensiones(resultado)).containsEntry("codigo", "NO_DISPONIBLE");
    }

    @Test
    @DisplayName("turnoActivo devuelve el turno abierto de la caja; otro negocio no lo ve")
    void turnoActivo_aisladoPorNegocio() {
        var caja = (String) comando("crearCaja", Map.of("nombre", "Barra")).get("recursoId");
        comando("abrirTurno", Map.of("cajaId", caja, "fondoInicial", "0.00"));
        var consulta = "query($c: ID!) { turnoActivo(cajaId: $c) { estado } }";

        Map<String, Object> turno = dato(ejecutar(CAJERO, consulta, Map.of("c", caja)), "turnoActivo");
        Object ajeno = dato(ejecutar(new ContextoNegocio(UUID.randomUUID(), "otra"), consulta, Map.of("c", caja)),
                "turnoActivo");

        assertThat(turno).containsEntry("estado", "ABIERTO");
        assertThat(ajeno).isNull();
    }
}
