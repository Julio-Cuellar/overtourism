package mx.negocio360.ventas.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.catalogo.FuenteDeProductos;
import mx.negocio360.compartido.catalogo.ProductoParaVenta;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.SesionAutenticada;
import mx.negocio360.identidad.dominio.RolesBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Ventas y Caja de punta a punta por HTTP con sesiones reales de Identidad (los roles base de {@code RolesBase}):
 * la puerta de {@code /graphql}, el despachador de comandos, los permisos por variante y por servicio, el aislamiento por
 * negocio y los eventos de Ventas hacia Caja. Los productos salen de una fuente de prueba, como saldran de Misceláneos o
 * Restaurantes (D-128). Todo dato es de ejemplo.
 *
 * <p>Es la misma bateria para los dos almacenes (F6): {@code GraphQlVentasCajaMemoriaTest} y
 * {@code GraphQlVentasCajaPostgresTest} solo cambian la propiedad {@code ventas.almacen} y {@code caja.almacen}. Con
 * PostgreSQL los eventos de Ventas llegan a Caja por el outbox, despues de confirmar y en otro hilo; por eso las pruebas
 * esperan ({@link #esperar}) a que Caja los aplique en lugar de suponer que ya paso.
 */
public abstract class GraphQlVentasCajaContratoTest {

    private static final UUID TAMAL = UUID.fromString("0192f000-0000-7000-8000-0000000000a1");
    private static final UUID AMARANTO = UUID.fromString("0192f000-0000-7000-8000-0000000000a2");
    private static final ObjectMapper JSON = new ObjectMapper();

    /** Una fuente de productos como la de un vertical: los mismos dos productos para cualquier negocio. */
    @TestConfiguration
    public static class ProductosDePrueba {
        @Bean
        FuenteDeProductos fuenteDePrueba() {
            var productos = Map.of(
                    TAMAL, new ProductoParaVenta(TAMAL, "Tamal de mole", "pieza", new BigDecimal("30.00"), false, true),
                    AMARANTO, new ProductoParaVenta(AMARANTO, "Amaranto a granel", "kg", new BigDecimal("80.00"), true,
                            true));
            return new FuenteDeProductos() {
                @Override
                public Map<UUID, ProductoParaVenta> preciosParaVenta(UUID negocioId, Set<UUID> ids) {
                    var encontrados = new HashMap<UUID, ProductoParaVenta>();
                    ids.forEach(id -> {
                        if (productos.containsKey(id)) {
                            encontrados.put(id, productos.get(id));
                        }
                    });
                    return encontrados;
                }

                @Override
                public List<ProductoParaVenta> buscar(UUID negocioId, String texto, int limite) {
                    return productos.values().stream().filter(p -> texto == null
                            || p.descripcion().toLowerCase().contains(texto.toLowerCase())).limit(limite).toList();
                }
            };
        }
    }

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private IdentidadApi identidad;

    private int ip;

    @BeforeEach
    void ipNueva() {
        ip = (int) (Math.random() * 200) + 20;
    }

    /** Una sesion con un rol base de Identidad: los permisos son los reales. */
    private record Sesion(UUID negocioId, UUID cuentaId, String token) {
    }

    private Sesion sesion(UUID negocioId, String rol) {
        var ahora = Instant.parse("2026-10-16T16:00:00Z");
        var cuenta = UUID.randomUUID();
        var token = "T" + UUID.randomUUID().toString().replace("-", "") + "xxxxxxxxxx";
        when(identidad.autenticar(token)).thenReturn(Optional.of(new SesionAutenticada(cuenta, "Ana", negocioId, rol,
                RolesBase.porCodigo(rol).orElseThrow().permisos(), ahora.plusSeconds(1800))));
        return new Sesion(negocioId, cuenta, token);
    }

    private Sesion sinNegocio() {
        var ahora = Instant.parse("2026-10-16T16:00:00Z");
        var token = "T" + UUID.randomUUID().toString().replace("-", "") + "xxxxxxxxxx";
        when(identidad.autenticar(token)).thenReturn(Optional.of(new SesionAutenticada(UUID.randomUUID(), "Ana", null,
                null, Set.of(), ahora.plusSeconds(1800))));
        return new Sesion(null, null, token);
    }

    private JsonNode graphql(Sesion s, String consulta, Map<String, Object> variables) throws Exception {
        var cuerpo = JSON.writeValueAsString(Map.of("query", consulta, "variables", variables));
        var peticion = post("/graphql").contentType(MediaType.APPLICATION_JSON).content(cuerpo).with(r -> {
            r.setRemoteAddr("10.8." + ip + "." + (int) (Math.random() * 250 + 1));
            return r;
        });
        if (s != null) {
            peticion.header("Authorization", "Bearer " + s.token());
        }
        var respuesta = mvc.perform(peticion).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JSON.readTree(respuesta);
    }

    private static final String PREPARAR = "mutation($i: ComandoInput!) { prepararComando(input: $i) "
            + "{ token resumen expiraEn advertencias } }";
    private static final String CONFIRMAR = "mutation($t: String!) { confirmarComando(token: $t) "
            + "{ ok codigo mensajePlantilla recursoId datos { clave valor } } }";

    /** Espera hasta 10 s a que la condicion se cumpla: con PostgreSQL Caja aplica los eventos de Ventas despues de confirmar. */
    protected static void esperar(String que, java.util.concurrent.Callable<Boolean> condicion) {
        org.awaitility.Awaitility.await(que).atMost(java.time.Duration.ofSeconds(10))
                .pollInterval(java.time.Duration.ofMillis(50)).until(condicion);
    }

    private static String codigo(JsonNode r) {
        return r.at("/errors/0/extensions/codigo").asText();
    }

    /** Prepara y confirma un comando; falla la prueba si algo sale mal. */
    private JsonNode comando(Sesion s, Map<String, Object> entrada) throws Exception {
        var preparada = graphql(s, PREPARAR, Map.of("i", entrada));
        assertThat(preparada.has("errors")).as(preparada.toString()).isFalse();
        var token = preparada.at("/data/prepararComando/token").asText();
        var confirmada = graphql(s, CONFIRMAR, Map.of("t", token));
        assertThat(confirmada.at("/data/confirmarComando/ok").asBoolean()).as(confirmada.toString()).isTrue();
        return confirmada.at("/data/confirmarComando");
    }

    private static String dato(JsonNode resultado, String clave) {
        for (var d : resultado.get("datos")) {
            if (d.get("clave").asText().equals(clave)) {
                return d.get("valor").asText();
            }
        }
        throw new AssertionError("falta el dato " + clave);
    }

    private String turnoAbierto(Sesion duenoOAdmin) throws Exception {
        var caja = comando(duenoOAdmin, Map.of("crearCaja", Map.of("nombre", "Mostrador"))).get("recursoId").asText();
        return comando(duenoOAdmin, Map.of("abrirTurno", Map.of("cajaId", caja, "fondoInicial", "500.00")))
                .get("recursoId").asText();
    }

    private Map<String, Object> venta(String turno) {
        return Map.of("registrarVenta", Map.of("turnoId", turno,
                "lineas", List.of(Map.of("productoId", TAMAL.toString(), "cantidad", "2"),
                        Map.of("productoId", AMARANTO.toString(), "cantidad", "0.5")),
                "pagos", List.of(Map.of("metodo", "EFECTIVO", "monto", "100.00", "recibido", "200.00"))));
    }

    /** Cuantas ventas de ese turno ya aplico Caja. */
    private int ventasAplicadas(Sesion s, String turno) throws Exception {
        var saldo = graphql(s, "query($t: ID!) { saldoTurno(turnoId: $t) { ventasAplicadas } }", Map.of("t", turno));
        return saldo.at("/data/saldoTurno/ventasAplicadas").asInt(-1);
    }

    // ---- el camino completo del dueño ----

    @Test
    @DisplayName("el dueño crea caja, abre turno, cobra, ve el saldo y las ventas, anula y corta")
    void caminoDelDueno() throws Exception {
        var dueno = sesion(UUID.randomUUID(), "DUENO");
        var turno = turnoAbierto(dueno);

        var productos = graphql(dueno, "{ productosParaVenta(texto: \"tamal\", primeros: 5) { productoId descripcion "
                + "unidad precio admiteFraccion } }", Map.of());
        assertThat(productos.at("/data/productosParaVenta/0/productoId").asText()).isEqualTo(TAMAL.toString());
        assertThat(productos.at("/data/productosParaVenta/0/precio").asText()).isEqualTo("30.00");

        var cobro = comando(dueno, venta(turno));
        assertThat(dato(cobro, "total")).isEqualTo("100.00");
        assertThat(dato(cobro, "cambio")).isEqualTo("100.00");
        assertThat(dato(cobro, "folio")).isEqualTo("1");
        var ventaId = cobro.get("recursoId").asText();
        esperar("Caja aplica la venta", () -> ventasAplicadas(dueno, turno) == 1);

        var saldo = graphql(dueno, "query($t: ID!) { saldoTurno(turnoId: $t) { estado ventasAplicadas esperadoPorMetodo "
                + "{ metodo monto } movimientos(primeros: 10) { movimientos { tipo metodo monto } pageInfo { hayMas } } } }",
                Map.of("t", turno));
        assertThat(saldo.has("errors")).as(saldo.toString()).isFalse();
        assertThat(saldo.at("/data/saldoTurno/ventasAplicadas").asInt()).isEqualTo(1);
        assertThat(saldo.at("/data/saldoTurno/esperadoPorMetodo/0/monto").asText()).isEqualTo("600.00");
        assertThat(saldo.at("/data/saldoTurno/movimientos/movimientos")).hasSize(2);

        var ventas = graphql(dueno, "query($f: FiltroVentas) { ventas(filtro: $f, primeros: 5) { ventas { id serie folio "
                + "estado total pagos { metodo monto recibido cambio } lineas { descripcion cantidad importe } } "
                + "pageInfo { hayMas cursorFinal } } }", Map.of("f", Map.of("turnoId", turno)));
        assertThat(ventas.has("errors")).as(ventas.toString()).isFalse();
        assertThat(ventas.at("/data/ventas/ventas/0/id").asText()).isEqualTo(ventaId);
        assertThat(ventas.at("/data/ventas/ventas/0/lineas")).hasSize(2);
        assertThat(ventas.at("/data/ventas/pageInfo/hayMas").asBoolean()).isFalse();

        var anulada = comando(dueno, Map.of("anularVenta", Map.of("ventaId", ventaId, "motivo", "Cobro duplicado")));
        assertThat(dato(anulada, "estado")).isEqualTo("ANULADA");
        esperar("Caja revierte la venta anulada", () -> ventasAplicadas(dueno, turno) == 0);

        var corte = comando(dueno, Map.of("cerrarTurno", Map.of("turnoId", turno, "contado",
                List.of(Map.of("metodo", "EFECTIVO", "monto", "500.00")))));
        assertThat(dato(corte, "diferencia_efectivo")).isEqualTo("0.00");
        var cortes = graphql(dueno, "{ cortes(primeros: 5) { cortes { turnoId diferenciaEfectivo esperadoPorMetodo "
                + "{ metodo monto } } pageInfo { hayMas } } }", Map.of());
        assertThat(cortes.at("/data/cortes/cortes/0/turnoId").asText()).isEqualTo(turno);
    }

    // ---- permisos por rol ----

    @Test
    @DisplayName("el cajero cobra y abre turno, pero no anula, no retira, no crea cajas ni audita cortes")
    void cajero_cobraPeroNoAnula() throws Exception {
        var negocio = UUID.randomUUID();
        var dueno = sesion(negocio, "DUENO");
        var cajero = sesion(negocio, "CAJERO");
        var turno = turnoAbierto(dueno);
        var ventaId = comando(cajero, venta(turno)).get("recursoId").asText();

        var anular = graphql(cajero, PREPARAR, Map.of("i", Map.of("anularVenta", Map.of("ventaId", ventaId,
                "motivo", "x"))));
        var retiro = graphql(cajero, PREPARAR, Map.of("i", Map.of("registrarMovimientoCaja", Map.of("turnoId", turno,
                "tipo", "RETIRO", "monto", "10.00", "nota", "Deposito"))));
        var crear = graphql(cajero, PREPARAR, Map.of("i", Map.of("crearCaja", Map.of("nombre", "Otra"))));
        var cortes = graphql(cajero, "{ cortes(primeros: 5) { cortes { id } } }", Map.of());

        assertThat(codigo(anular)).isEqualTo("PERMISO_DENEGADO");
        assertThat(codigo(retiro)).isEqualTo("PERMISO_DENEGADO");
        assertThat(codigo(crear)).isEqualTo("PERMISO_DENEGADO");
        assertThat(codigo(cortes)).isEqualTo("PERMISO_DENEGADO");
        assertThat(anular.toString()).doesNotContain("ventas.venta.anular");
        // Lo que si puede: ver su turno y las ventas.
        assertThat(graphql(cajero, "{ cajas { id nombre } }", Map.of()).at("/data/cajas/0/nombre").asText())
                .isEqualTo("Mostrador");
    }

    @Test
    @DisplayName("mesero, cocinero y guia no cobran ni tocan la caja")
    void roles_sinCaja() throws Exception {
        var negocio = UUID.randomUUID();
        var turno = turnoAbierto(sesion(negocio, "DUENO"));

        for (var rol : List.of("MESERO", "COCINERO", "GUIA")) {
            var s = sesion(negocio, rol);
            assertThat(codigo(graphql(s, PREPARAR, Map.of("i", venta(turno))))).as(rol).isEqualTo("PERMISO_DENEGADO");
            assertThat(codigo(graphql(s, "{ cajas { id } }", Map.of()))).as(rol).isEqualTo("PERMISO_DENEGADO");
            assertThat(codigo(graphql(s, "{ ventas(primeros: 5) { ventas { id } } }", Map.of()))).as(rol)
                    .isEqualTo("PERMISO_DENEGADO");
        }
    }

    // ---- sesion y aislamiento ----

    @Test
    @DisplayName("sin sesion o sin negocio activo nada de Ventas ni de Caja responde, ni siquiera confirmar")
    void anonimoYSinNegocio_negados() throws Exception {
        assertThat(codigo(graphql(null, "{ cajas { id } }", Map.of()))).isEqualTo("NO_AUTENTICADO");
        assertThat(codigo(graphql(null, PREPARAR, Map.of("i", Map.of("crearCaja", Map.of("nombre", "X"))))))
                .isEqualTo("NO_AUTENTICADO");
        assertThat(codigo(graphql(null, CONFIRMAR, Map.of("t", "ventas.abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG"))))
                .isEqualTo("NO_AUTENTICADO");
        var sin = sinNegocio();
        assertThat(codigo(graphql(sin, "{ cajas { id } }", Map.of()))).isEqualTo("SIN_NEGOCIO_ACTIVO");
        assertThat(codigo(graphql(sin, CONFIRMAR, Map.of("t", "caja.abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGH"))))
                .isEqualTo("SIN_NEGOCIO_ACTIVO");
    }

    @Test
    @DisplayName("dos negocios no se ven: ni cajas, ni ventas, ni turnos; y un token ajeno no confirma")
    void dosNegocios_noSeVen() throws Exception {
        var a = sesion(UUID.randomUUID(), "DUENO");
        var b = sesion(UUID.randomUUID(), "DUENO");
        var turnoA = turnoAbierto(a);
        var ventaA = comando(a, venta(turnoA)).get("recursoId").asText();

        assertThat(graphql(b, "{ cajas { id } }", Map.of()).at("/data/cajas")).isEmpty();
        assertThat(graphql(b, "query($i: ID!) { venta(id: $i) { id } }", Map.of("i", ventaA)).at("/data/venta").isNull())
                .isTrue();
        assertThat(codigo(graphql(b, "query($t: ID!) { saldoTurno(turnoId: $t) { estado } }", Map.of("t", turnoA))))
                .isEqualTo("TURNO_NO_ENCONTRADO");
        assertThat(graphql(b, "{ ventas(primeros: 5) { ventas { id } } }", Map.of()).at("/data/ventas/ventas"))
                .isEmpty();

        var preparada = graphql(a, PREPARAR, Map.of("i", venta(turnoA)));
        var token = preparada.at("/data/prepararComando/token").asText();
        var robado = graphql(b, CONFIRMAR, Map.of("t", token));
        assertThat(robado.at("/errors/0/extensions/codigo").asText()).isEqualTo("TOKEN_INVALIDO");
    }

    @Test
    @DisplayName("un token sin prefijo conocido, o de otro modulo, responde TOKEN_INVALIDO sin decir que modulos existen")
    void token_prefijoDesconocido() throws Exception {
        var dueno = sesion(UUID.randomUUID(), "DUENO");

        var r1 = graphql(dueno, CONFIRMAR, Map.of("t", "inventado.abcdefghijklmnopqrstuvwxyz0123456789ABCDEFG"));
        var r2 = graphql(dueno, CONFIRMAR, Map.of("t", "caja.abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGH"));

        assertThat(codigo(r1)).isEqualTo("TOKEN_INVALIDO");
        assertThat(codigo(r2)).isEqualTo("TOKEN_INVALIDO");
        assertThat(r1.toString()).doesNotContain("ventas").doesNotContain("negocios");
    }

    @Test
    @DisplayName("D-126: si le quitan el permiso entre preparar y confirmar, confirmar falla y el token sigue vivo")
    void permisoRevocadoEntrePasos() throws Exception {
        var negocio = UUID.randomUUID();
        var dueno = sesion(negocio, "DUENO");
        var turno = turnoAbierto(dueno);
        var ventaId = comando(dueno, venta(turno)).get("recursoId").asText();
        var token = graphql(dueno, PREPARAR, Map.of("i", Map.of("anularVenta", Map.of("ventaId", ventaId, "motivo",
                "Error")))).at("/data/prepararComando/token").asText();
        // Mismo token de sesion, pero ahora Identidad ya no le da el permiso de anular.
        when(identidad.autenticar(dueno.token())).thenReturn(Optional.of(new SesionAutenticada(dueno.cuentaId(), "Ana",
                negocio, "CAJERO", RolesBase.porCodigo("CAJERO").orElseThrow().permisos(),
                Instant.parse("2026-10-16T16:30:00Z"))));

        var sin = graphql(dueno, CONFIRMAR, Map.of("t", token));
        assertThat(codigo(sin)).isEqualTo("PERMISO_DENEGADO");

        when(identidad.autenticar(dueno.token())).thenReturn(Optional.of(new SesionAutenticada(dueno.cuentaId(), "Ana",
                negocio, "DUENO", RolesBase.porCodigo("DUENO").orElseThrow().permisos(),
                Instant.parse("2026-10-16T16:30:00Z"))));
        var con = graphql(dueno, CONFIRMAR, Map.of("t", token));
        assertThat(con.at("/data/confirmarComando/ok").asBoolean()).as(con.toString()).isTrue();
    }

    // ---- errores de dominio ----

    @Test
    @DisplayName("un error de dominio sale con su codigo y texto fijos; un cupon es invalido mientras no exista Pasaporte")
    void erroresDeDominio() throws Exception {
        var dueno = sesion(UUID.randomUUID(), "DUENO");
        var turno = turnoAbierto(dueno);
        var conCupon = Map.<String, Object>of("registrarVenta", Map.of("turnoId", turno,
                "lineas", List.of(Map.of("productoId", TAMAL.toString(), "cantidad", "1")),
                "pagos", List.of(Map.of("metodo", "EFECTIVO", "monto", "30.00")), "codigoCupon", "PASAPORTE-20"));

        var r = graphql(dueno, PREPARAR, Map.of("i", conCupon));

        assertThat(codigo(r)).isEqualTo("CUPON_INVALIDO");
        assertThat(r.at("/errors/0/message").asText()).isEqualTo("Ese cupón no es válido.");
        assertThat(codigo(graphql(dueno, PREPARAR, Map.of("i", Map.of("anularVenta", Map.of("ventaId",
                UUID.randomUUID().toString(), "motivo", "x")))))).isEqualTo("VENTA_NO_ENCONTRADA");
    }

    @Test
    @DisplayName("R-V1: la referencia entra por registrarVenta, queda en la venta y una insegura se rechaza con REFERENCIA_INVALIDA")
    void referenciaDeLaVenta() throws Exception {
        var dueno = sesion(UUID.randomUUID(), "DUENO");
        var turno = turnoAbierto(dueno);
        var con = new java.util.HashMap<String, Object>(Map.of("turnoId", turno,
                "lineas", List.of(Map.of("productoId", TAMAL.toString(), "cantidad", "1")),
                "pagos", List.of(Map.of("metodo", "EFECTIVO", "monto", "30.00"))));
        con.put("referencia", "restaurantes:cuenta:0192c1aa-1111-7000-8000-00000000c001");

        var cobro = comando(dueno, Map.of("registrarVenta", con));

        var ventas = graphql(dueno, "query($f: FiltroVentas) { ventas(filtro: $f, primeros: 5) { ventas { id referencia } } }",
                Map.of("f", Map.of("turnoId", turno)));
        assertThat(ventas.at("/data/ventas/ventas/0/id").asText()).isEqualTo(cobro.get("recursoId").asText());
        assertThat(ventas.at("/data/ventas/ventas/0/referencia").asText())
                .isEqualTo("restaurantes:cuenta:0192c1aa-1111-7000-8000-00000000c001");
        con.put("referencia", "con espacios y <html>");
        var mala = graphql(dueno, PREPARAR, Map.of("i", Map.of("registrarVenta", con)));
        assertThat(codigo(mala)).isEqualTo("REFERENCIA_INVALIDA");
        assertThat(mala.toString()).doesNotContain("<html>");
    }

    @Test
    @DisplayName("un comando con campos que no existen, o con dos variantes, se rechaza")
    void comandoInvalido() throws Exception {
        var dueno = sesion(UUID.randomUUID(), "DUENO");

        var campoExtra = graphql(dueno, PREPARAR, Map.of("i", Map.of("crearCaja", Map.of("nombre", "X", "negocioId",
                UUID.randomUUID().toString()))));
        var dosVariantes = graphql(dueno, PREPARAR, Map.of("i", Map.of("crearCaja", Map.of("nombre", "X"),
                "descartarCorte", Map.of("turnoId", UUID.randomUUID().toString()))));

        assertThat(campoExtra.has("errors")).isTrue();
        assertThat(dosVariantes.has("errors")).isTrue();
    }
}
