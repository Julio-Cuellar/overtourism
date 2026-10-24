package mx.negocio360.restaurantes.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.SesionAutenticada;
import mx.negocio360.identidad.dominio.RolesBase;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * CT-07 a CT-10 de punta a punta con la aplicacion completa y los almacenes en memoria: el mesero pide la cuenta, la caja
 * la cobra con {@code registrarVenta} y la referencia de la cuenta, y la cuenta queda pagada con su venta y la mesa libre; si
 * la venta se anula, la cuenta lo recuerda y la mesa no se reocupa. Ventas, Caja y Restaurantes se hablan solo por el bus.
 */
@SpringBootTest(properties = {"restaurantes.almacen=memoria", "inventario.almacen=memoria", "ventas.almacen=memoria",
        "caja.almacen=memoria"})
@AutoConfigureMockMvc
class GraphQlCobroDeCuentaTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PREPARAR = "mutation($i: ComandoInput!) { prepararComando(input: $i) { token resumen } }";
    private static final String CONFIRMAR = "mutation($t: String!) { confirmarComando(token: $t) { ok recursoId } }";

    @Autowired
    private MockMvc mvc;
    @MockitoBean
    private IdentidadApi identidad;

    private String token(UUID negocio, String rol) {
        var token = "T" + UUID.randomUUID().toString().replace("-", "") + "xxxxxxxxxx";
        when(identidad.autenticar(token)).thenReturn(Optional.of(new SesionAutenticada(UUID.randomUUID(), "Ana", negocio,
                rol, RolesBase.porCodigo(rol).orElseThrow().permisos(), Instant.parse("2026-10-16T16:30:00Z"))));
        return token;
    }

    private JsonNode graphql(String token, String consulta, Map<String, Object> variables) throws Exception {
        var cuerpo = JSON.writeValueAsString(Map.of("query", consulta, "variables", variables));
        var peticion = post("/graphql").contentType(MediaType.APPLICATION_JSON).content(cuerpo)
                .header("Authorization", "Bearer " + token).with(r -> {
                    r.setRemoteAddr("10.6." + (int) (Math.random() * 250 + 1) + "." + (int) (Math.random() * 250 + 1));
                    return r;
                });
        return JSON.readTree(mvc.perform(peticion).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString());
    }

    private JsonNode sinErrores(JsonNode r) {
        assertThat(r.has("errors")).as(r.toString()).isFalse();
        return r;
    }

    private JsonNode comando(String token, Map<String, Object> entrada) throws Exception {
        var preparada = sinErrores(graphql(token, PREPARAR, Map.of("i", entrada)));
        var confirmada = sinErrores(graphql(token, CONFIRMAR, Map.of("t", preparada.at("/data/prepararComando/token").asText())));
        assertThat(confirmada.at("/data/confirmarComando/ok").asBoolean()).as(confirmada.toString()).isTrue();
        return confirmada.at("/data/confirmarComando");
    }

    private void esperar(String que, java.util.concurrent.Callable<Boolean> condicion) {
        Awaitility.await(que).atMost(Duration.ofSeconds(10)).pollInterval(Duration.ofMillis(50)).until(condicion);
    }

    private record Armado(UUID negocio, String dueno, String mesero, String mesa, String cuenta, String cemita,
                          String horchata) {
    }

    /** Un restaurante con una mesa, un turno abierto y una cuenta de la mesa por cobrar con 2 cemitas y 1 horchata ($205.00). */
    private Armado armarCuentaPorCobrar(int comensales) throws Exception {
        var negocio = UUID.randomUUID();
        var dueno = token(negocio, "DUENO");
        var mesero = token(negocio, "MESERO");
        var zona = sinErrores(graphql(dueno, "mutation { crearZona(nombre: \"Salon\") { id } }", Map.of()))
                .at("/data/crearZona/id").asText();
        var mesa = sinErrores(graphql(dueno, "mutation($i: RestMesaInput!) { crearMesa(input: $i) { id } }",
                Map.of("i", Map.of("zonaId", zona, "etiqueta", "Mesa 1", "capacidad", 4)))).at("/data/crearMesa/id").asText();
        sinErrores(graphql(dueno, "mutation { abrirServicio { abierto } }", Map.of()));
        var cemita = sinErrores(graphql(dueno, "mutation($i: RestPlatilloInput!) { crearPlatillo(input: $i) { id } }",
                Map.of("i", Map.of("nombre", "Cemita poblana", "precio", "85.00")))).at("/data/crearPlatillo/id").asText();
        var horchata = sinErrores(graphql(dueno, "mutation($i: RestPlatilloInput!) { crearPlatillo(input: $i) { id } }",
                Map.of("i", Map.of("nombre", "Agua de horchata", "precio", "35.00")))).at("/data/crearPlatillo/id").asText();
        var cuenta = sinErrores(graphql(mesero, "mutation($i: RestAbrirCuentaInput!) { abrirCuenta(input: $i) { id } }",
                Map.of("i", Map.of("mesaId", mesa, "comensales", comensales)))).at("/data/abrirCuenta/id").asText();
        sinErrores(graphql(mesero, "mutation($i: RestAgregarLineaInput!) { agregarLinea(input: $i) { total } }",
                Map.of("i", Map.of("cuentaId", cuenta, "platilloId", cemita, "cantidad", 2))));
        sinErrores(graphql(mesero, "mutation($i: RestAgregarLineaInput!) { agregarLinea(input: $i) { total } }",
                Map.of("i", Map.of("cuentaId", cuenta, "platilloId", horchata, "cantidad", 1))));
        sinErrores(graphql(mesero, "mutation($c: ID!) { enviarACocina(cuentaId: $c) { id } }", Map.of("c", cuenta)));
        sinErrores(graphql(mesero, "mutation($c: ID!) { pedirCuenta(cuentaId: $c) { estado } }", Map.of("c", cuenta)));
        return new Armado(negocio, dueno, mesero, mesa, cuenta, cemita, horchata);
    }

    private String turnoAbierto(String dueno) throws Exception {
        var caja = comando(dueno, Map.of("crearCaja", Map.of("nombre", "Caja del salon"))).get("recursoId").asText();
        return comando(dueno, Map.of("abrirTurno", Map.of("cajaId", caja, "fondoInicial", "500.00"))).get("recursoId").asText();
    }

    /** Cobra lo que dice cuentaParaCobro, con su referencia; las lineas de la venta salen de la consulta, no de la cuenta. */
    private String cobrar(Armado a, String turno, List<Map<String, Object>> lineas, String total, String referencia)
            throws Exception {
        var venta = new java.util.HashMap<String, Object>(Map.of("turnoId", turno, "lineas", lineas,
                "pagos", List.of(Map.of("metodo", "EFECTIVO", "monto", total, "recibido", "300.00"))));
        venta.put("referencia", referencia);
        return comando(a.dueno(), Map.of("registrarVenta", venta)).get("recursoId").asText();
    }

    private JsonNode cuenta(Armado a) throws Exception {
        return sinErrores(graphql(a.dueno(), "query($c: ID!) { cuenta(id: $c) { estado ventaId discrepancia ventaAnulada } }",
                Map.of("c", a.cuenta()))).at("/data/cuenta");
    }

    private int mesasLibres(Armado a) throws Exception {
        return sinErrores(graphql(a.dueno(), "{ salon { disponibilidad { libres total } } }", Map.of()))
                .at("/data/salon/disponibilidad/libres").asInt(-1);
    }

    @Test
    @DisplayName("CT-07, CT-08: la caja cobra con la referencia de la cuenta y la cuenta queda PAGADA con su venta y la mesa libre")
    void cobroDeUnaCuenta() throws Exception {
        var a = armarCuentaPorCobrar(2);
        var turno = turnoAbierto(a.dueno());
        assertThat(mesasLibres(a)).isZero();
        var cobro = sinErrores(graphql(a.mesero(), "query($c: ID!) { cuentaParaCobro(cuentaId: $c) { referencia total "
                + "lineas { productoId descripcion cantidad } } }", Map.of("c", a.cuenta()))).at("/data/cuentaParaCobro");
        assertThat(cobro.at("/total").asText()).isEqualTo("205.00");
        var lineas = new java.util.ArrayList<Map<String, Object>>();
        cobro.at("/lineas").forEach(l -> lineas.add(Map.of("productoId", l.at("/productoId").asText(), "cantidad",
                l.at("/cantidad").asText())));

        var ventaId = cobrar(a, turno, lineas, cobro.at("/total").asText(), cobro.at("/referencia").asText());

        esperar("la cuenta queda pagada", () -> "PAGADA".equals(cuenta(a).at("/estado").asText()));
        var pagada = cuenta(a);
        assertThat(pagada.at("/ventaId").asText()).isEqualTo(ventaId);
        assertThat(pagada.at("/discrepancia").asBoolean()).isFalse();
        assertThat(pagada.at("/ventaAnulada").asBoolean()).isFalse();
        esperar("la mesa se libera", () -> mesasLibres(a) == 1);
        var abiertas = sinErrores(graphql(a.mesero(), "{ cuentasAbiertas { id } }", Map.of()));
        assertThat(abiertas.at("/data/cuentasAbiertas")).isEmpty();
    }

    @Test
    @DisplayName("CT-08: si la caja cobra algo distinto de la cuenta, queda PAGADA con discrepancia")
    void cobroConDiscrepancia() throws Exception {
        var a = armarCuentaPorCobrar(2);
        var turno = turnoAbierto(a.dueno());
        var cobro = sinErrores(graphql(a.mesero(), "query($c: ID!) { cuentaParaCobro(cuentaId: $c) { referencia lineas "
                + "{ productoId cantidad } } }", Map.of("c", a.cuenta()))).at("/data/cuentaParaCobro");

        cobrar(a, turno, List.of(Map.of("productoId", a.cemita(), "cantidad", "2")), "170.00", cobro.at("/referencia").asText());

        esperar("la cuenta queda pagada", () -> "PAGADA".equals(cuenta(a).at("/estado").asText()));
        assertThat(cuenta(a).at("/discrepancia").asBoolean()).isTrue();
    }

    @Test
    @DisplayName("CT-10: al anular la venta la cuenta lo recuerda, sigue PAGADA y la mesa no se vuelve a ocupar")
    void ventaAnulada() throws Exception {
        var a = armarCuentaPorCobrar(2);
        var turno = turnoAbierto(a.dueno());
        var cobro = sinErrores(graphql(a.mesero(), "query($c: ID!) { cuentaParaCobro(cuentaId: $c) { referencia total lineas "
                + "{ productoId cantidad } } }", Map.of("c", a.cuenta()))).at("/data/cuentaParaCobro");
        var lineas = new java.util.ArrayList<Map<String, Object>>();
        cobro.at("/lineas").forEach(l -> lineas.add(Map.of("productoId", l.at("/productoId").asText(), "cantidad",
                l.at("/cantidad").asText())));
        var ventaId = cobrar(a, turno, lineas, cobro.at("/total").asText(), cobro.at("/referencia").asText());
        esperar("la cuenta queda pagada", () -> "PAGADA".equals(cuenta(a).at("/estado").asText()));

        comando(a.dueno(), Map.of("anularVenta", Map.of("ventaId", ventaId, "motivo", "Cobro duplicado")));

        esperar("la cuenta recuerda la anulacion", () -> cuenta(a).at("/ventaAnulada").asBoolean());
        assertThat(cuenta(a).at("/estado").asText()).isEqualTo("PAGADA");
        assertThat(mesasLibres(a)).isEqualTo(1);
    }

    @Test
    @DisplayName("CT-08: una venta con la referencia de la cuenta de OTRO negocio no la toca")
    void referenciaDeOtroNegocio() throws Exception {
        var a = armarCuentaPorCobrar(2);
        var otro = armarCuentaPorCobrar(2);
        var turnoDeOtro = turnoAbierto(otro.dueno());
        var referenciaDeA = sinErrores(graphql(a.mesero(), "query($c: ID!) { cuentaParaCobro(cuentaId: $c) { referencia } }",
                Map.of("c", a.cuenta()))).at("/data/cuentaParaCobro/referencia").asText();

        cobrar(otro, turnoDeOtro, List.of(Map.of("productoId", otro.cemita(), "cantidad", "1")), "85.00", referenciaDeA);

        // La caja del otro negocio cobro con la referencia de una cuenta ajena: la cuenta de A sigue por cobrar.
        Awaitility.await().pollDelay(Duration.ofMillis(700)).atMost(Duration.ofSeconds(3)).until(() -> true);
        assertThat(cuenta(a).at("/estado").asText()).isEqualTo("POR_COBRAR");
        assertThat(cuenta(otro).at("/estado").asText()).isEqualTo("POR_COBRAR");
    }
}
