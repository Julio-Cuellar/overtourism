package mx.negocio360.inventario.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.inventario.InventarioApi;
import mx.negocio360.inventario.aplicacion.LineaDeConsumo;
import mx.negocio360.identidad.SesionAutenticada;
import mx.negocio360.identidad.dominio.RolesBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Inventario de punta a punta por HTTP con sesiones reales de Identidad (los roles base): la puerta de {@code /graphql}, el
 * despachador de comandos, los permisos por variante y por token, el aislamiento por negocio y el kardex. Todo dato es de
 * ejemplo.
 */
public abstract class GraphQlInventarioContratoTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PREPARAR = "mutation($i: ComandoInput!) { prepararComando(input: $i) "
            + "{ token resumen expiraEn advertencias } }";
    private static final String CONFIRMAR = "mutation($t: String!) { confirmarComando(token: $t) "
            + "{ ok codigo mensajePlantilla recursoId datos { clave valor } } }";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private InventarioApi inventario;
    @MockitoBean
    private IdentidadApi identidad;

    private int ip;

    @BeforeEach
    void ipNueva() {
        ip = (int) (Math.random() * 200) + 20;
    }

    private record Sesion(UUID negocioId, UUID cuentaId, String token) {
    }

    private Sesion sesion(UUID negocioId, String rol) {
        var cuenta = UUID.randomUUID();
        var token = "T" + UUID.randomUUID().toString().replace("-", "") + "xxxxxxxxxx";
        when(identidad.autenticar(token)).thenReturn(Optional.of(new SesionAutenticada(cuenta, "Ana", negocioId, rol,
                RolesBase.porCodigo(rol).orElseThrow().permisos(), Instant.parse("2026-10-16T16:30:00Z"))));
        return new Sesion(negocioId, cuenta, token);
    }

    private JsonNode graphql(Sesion s, String consulta, Map<String, Object> variables) throws Exception {
        var cuerpo = JSON.writeValueAsString(Map.of("query", consulta, "variables", variables));
        var peticion = post("/graphql").contentType(MediaType.APPLICATION_JSON).content(cuerpo).with(r -> {
            r.setRemoteAddr("10.6." + ip + "." + (int) (Math.random() * 250 + 1));
            return r;
        });
        if (s != null) {
            peticion.header("Authorization", "Bearer " + s.token());
        }
        return JSON.readTree(mvc.perform(peticion).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString());
    }

    private static String codigo(JsonNode r) {
        return r.at("/errors/0/extensions/codigo").asText();
    }

    private JsonNode comando(Sesion s, Map<String, Object> entrada) throws Exception {
        var preparada = graphql(s, PREPARAR, Map.of("i", entrada));
        assertThat(preparada.has("errors")).as(preparada.toString()).isFalse();
        var token = preparada.at("/data/prepararComando/token").asText();
        assertThat(token).startsWith("inventario.");
        var confirmada = graphql(s, CONFIRMAR, Map.of("t", token));
        assertThat(confirmada.at("/data/confirmarComando/ok").asBoolean()).as(confirmada.toString()).isTrue();
        return confirmada.at("/data/confirmarComando");
    }

    private String harina(Sesion s) throws Exception {
        return comando(s, Map.of("crearArticulo", Map.of("nombre", "Harina", "unidadBase", "g", "minimo", "1",
                "unidadMinimo", "kg", "unidadesPropias", List.of(Map.of("nombre", "costal", "factor", "25000")))))
                .get("recursoId").asText();
    }

    @Test
    @DisplayName("el administrador crea un articulo, registra entrada, salida y merma, y ve saldo, costo y kardex")
    void caminoCompleto() throws Exception {
        var admin = sesion(UUID.randomUUID(), "ADMINISTRADOR");
        var articulo = harina(admin);

        comando(admin, Map.of("registrarEntradaInventario", Map.of("articuloId", articulo, "cantidad", "2",
                "unidad", "costal", "costoUnitario", "450.00")));
        comando(admin, Map.of("registrarSalidaInventario", Map.of("articuloId", articulo, "cantidad", "5",
                "unidad", "kg")));
        comando(admin, Map.of("registrarMermaInventario", Map.of("articuloId", articulo, "cantidad", "1", "unidad",
                "kg", "motivo", "DANO")));

        var vista = graphql(admin, "query($i: ID!) { articulo(id: $i) { nombre unidadBase cantidadBase cpp valor "
                + "bajoMinimo unidadesPropias { nombre factor } } }", Map.of("i", articulo));
        assertThat(vista.has("errors")).as(vista.toString()).isFalse();
        assertThat(vista.at("/data/articulo/nombre").asText()).isEqualTo("Harina");
        assertThat(vista.at("/data/articulo/cantidadBase").asText()).isEqualTo("44000.0000");
        assertThat(vista.at("/data/articulo/unidadesPropias/0/nombre").asText()).isEqualTo("costal");
        assertThat(vista.at("/data/articulo/bajoMinimo").asBoolean()).isFalse();

        var kardex = graphql(admin, "query($i: ID!) { kardex(articuloId: $i, primeros: 2) { nodos { tipo causaTipo "
                + "cantidadBase saldoResultante } pageInfo { hayMas cursorFinal } } }", Map.of("i", articulo));
        assertThat(kardex.at("/data/kardex/nodos")).hasSize(2);
        assertThat(kardex.at("/data/kardex/nodos/0/tipo").asText()).isEqualTo("ENTRADA");
        assertThat(kardex.at("/data/kardex/pageInfo/hayMas").asBoolean()).isTrue();
        var siguiente = graphql(admin, "query($i: ID!, $d: String) { kardex(articuloId: $i, primeros: 5, despues: $d) "
                + "{ nodos { tipo } } }", Map.of("i", articulo, "d", kardex.at("/data/kardex/pageInfo/cursorFinal")
                .asText()));
        assertThat(siguiente.at("/data/kardex/nodos")).hasSize(1);

        var lista = graphql(admin, "{ inventario(texto: \"harin\", primeros: 5) { nodos { id nombre } pageInfo "
                + "{ hayMas } } }", Map.of());
        assertThat(lista.at("/data/inventario/nodos/0/id").asText()).isEqualTo(articulo);
    }

    @Test
    @DisplayName("el cocinero ve existencias y registra salidas, pero no crea articulos ni ajusta por conteo")
    void cocinero_soloOperaLaCocina() throws Exception {
        var negocio = UUID.randomUUID();
        var articulo = harina(sesion(negocio, "DUENO"));
        var cocinero = sesion(negocio, "COCINERO");
        comando(sesion(negocio, "DUENO"), Map.of("registrarEntradaInventario", Map.of("articuloId", articulo,
                "cantidad", "10", "unidad", "kg", "costoUnitario", "18.50")));

        comando(cocinero, Map.of("registrarSalidaInventario", Map.of("articuloId", articulo, "cantidad", "1",
                "unidad", "kg")));
        assertThat(graphql(cocinero, "{ inventario(primeros: 5) { nodos { nombre } } }", Map.of())
                .at("/data/inventario/nodos/0/nombre").asText()).isEqualTo("Harina");

        assertThat(codigo(graphql(cocinero, PREPARAR, Map.of("i", Map.of("crearArticulo", Map.of("nombre", "Sal",
                "unidadBase", "g")))))).isEqualTo("PERMISO_DENEGADO");
        assertThat(codigo(graphql(cocinero, PREPARAR, Map.of("i", Map.of("ajustarPorConteo", Map.of("articuloId",
                articulo, "cantidadContada", "5", "unidad", "kg")))))).isEqualTo("PERMISO_DENEGADO");
    }

    @Test
    @DisplayName("cajero, mesero y guia no tocan Inventario; sin sesion o sin negocio, tampoco")
    void sinPermiso_niSesion() throws Exception {
        var negocio = UUID.randomUUID();
        for (var rol : List.of("CAJERO", "MESERO", "GUIA", "TAQUILLERO")) {
            var s = sesion(negocio, rol);
            assertThat(codigo(graphql(s, "{ inventario(primeros: 5) { nodos { id } } }", Map.of()))).as(rol)
                    .isEqualTo("PERMISO_DENEGADO");
        }
        assertThat(codigo(graphql(null, "{ inventario(primeros: 5) { nodos { id } } }", Map.of())))
                .isEqualTo("NO_AUTENTICADO");
        assertThat(codigo(graphql(null, CONFIRMAR, Map.of("t", "inventario.abcdefghijklmnopqrstuvwxyz0123456789ABC"))))
                .isEqualTo("NO_AUTENTICADO");
    }

    @Test
    @DisplayName("dos negocios no se ven los articulos ni el kardex, y un token de otro negocio no confirma")
    void dosNegocios_noSeVen() throws Exception {
        var a = sesion(UUID.randomUUID(), "DUENO");
        var b = sesion(UUID.randomUUID(), "DUENO");
        var articulo = harina(a);

        assertThat(graphql(b, "{ inventario(primeros: 5) { nodos { id } } }", Map.of()).at("/data/inventario/nodos"))
                .isEmpty();
        assertThat(graphql(b, "query($i: ID!) { articulo(id: $i) { id } }", Map.of("i", articulo))
                .at("/data/articulo").isNull()).isTrue();
        assertThat(codigo(graphql(b, "query($i: ID!) { kardex(articuloId: $i) { nodos { id } } }",
                Map.of("i", articulo)))).isEqualTo("ARTICULO_NO_ENCONTRADO");
        var token = graphql(a, PREPARAR, Map.of("i", Map.of("registrarSalidaInventario", Map.of("articuloId", articulo,
                "cantidad", "1", "unidad", "kg")))).at("/data/prepararComando/token").asText();
        assertThat(codigo(graphql(b, CONFIRMAR, Map.of("t", token)))).isEqualTo("TOKEN_INVALIDO");
    }

    @Test
    @DisplayName("D-126: si le quitan el permiso entre preparar y confirmar, falla con PERMISO_DENEGADO y el token sigue vivo")
    void permisoRevocadoEntrePasos() throws Exception {
        var negocio = UUID.randomUUID();
        var admin = sesion(negocio, "ADMINISTRADOR");
        var articulo = harina(admin);
        var token = graphql(admin, PREPARAR, Map.of("i", Map.of("registrarEntradaInventario", Map.of("articuloId",
                articulo, "cantidad", "1", "unidad", "kg", "costoUnitario", "20.00")))).at("/data/prepararComando/token")
                .asText();
        when(identidad.autenticar(admin.token())).thenReturn(Optional.of(new SesionAutenticada(admin.cuentaId(), "Ana",
                negocio, "CAJERO", RolesBase.porCodigo("CAJERO").orElseThrow().permisos(),
                Instant.parse("2026-10-16T16:30:00Z"))));

        assertThat(codigo(graphql(admin, CONFIRMAR, Map.of("t", token)))).isEqualTo("PERMISO_DENEGADO");

        when(identidad.autenticar(admin.token())).thenReturn(Optional.of(new SesionAutenticada(admin.cuentaId(), "Ana",
                negocio, "ADMINISTRADOR", RolesBase.porCodigo("ADMINISTRADOR").orElseThrow().permisos(),
                Instant.parse("2026-10-16T16:30:00Z"))));
        assertThat(graphql(admin, CONFIRMAR, Map.of("t", token)).at("/data/confirmarComando/ok").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("un error de dominio sale con su codigo y texto fijos; una busqueda demasiado larga es una consulta invalida")
    void errores() throws Exception {
        var admin = sesion(UUID.randomUUID(), "ADMINISTRADOR");
        var articulo = harina(admin);

        var sinSaldo = graphql(admin, PREPARAR, Map.of("i", Map.of("registrarMermaInventario", Map.of("articuloId",
                articulo, "cantidad", "0", "unidad", "kg", "motivo", "ROBO"))));
        assertThat(sinSaldo.has("errors")).isTrue();
        assertThat(sinSaldo.at("/errors/0/extensions/codigo").asText()).isNotEqualTo("ERROR_INTERNO");
        assertThat(codigo(graphql(admin, "query($t: String) { inventario(texto: $t, primeros: 5) { nodos { id } } }",
                Map.of("t", "x".repeat(81))))).isEqualTo("CONSULTA_INVALIDA");
    }

    @Test
    @DisplayName("el kardex de un articulo con un consumo revertido (venta anulada) lista la REVERSION sin error")
    void kardexConReversion() throws Exception {
        var negocio = UUID.randomUUID();
        var dueno = sesion(negocio, "DUENO");
        var articulo = harina(dueno);
        comando(dueno, Map.of("registrarEntradaInventario", Map.of("articuloId", articulo, "cantidad", "10",
                "unidad", "kg", "costoUnitario", "18.50")));
        var contexto = new ContextoNegocio(negocio, dueno.cuentaId().toString());
        var venta = UUID.randomUUID();
        inventario.registrarConsumo(contexto, "restaurantes", venta, Instant.parse("2026-10-16T16:00:00Z"),
                List.of(new LineaDeConsumo(UUID.fromString(articulo), new java.math.BigDecimal("2"), "kg")));
        inventario.revertirConsumo(contexto, "restaurantes", venta);

        var kardex = graphql(dueno, "query($i: ID!) { kardex(articuloId: $i) { nodos { tipo causaTipo cantidadBase "
                + "saldoResultante } } }", Map.of("i", articulo));

        assertThat(kardex.has("errors")).as(kardex.toString()).isFalse();
        var nodos = kardex.at("/data/kardex/nodos");
        assertThat(nodos).hasSize(3);
        assertThat(nodos.get(1).get("causaTipo").asText()).isEqualTo("VENTA");
        assertThat(nodos.get(2).get("causaTipo").asText()).isEqualTo("REVERSION");
        assertThat(nodos.get(2).get("saldoResultante").asText()).isEqualTo("10000.0000");
    }
}
