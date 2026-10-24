package mx.negocio360.restaurantes.web;

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
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.SesionAutenticada;
import mx.negocio360.identidad.dominio.RolesBase;
import mx.negocio360.restaurantes.aplicacion.RepositorioRestaurantes;
import mx.negocio360.restaurantes.dominio.Restaurante;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * R6 de punta a punta por HTTP, con la aplicacion completa: el comensal, sin sesion, escanea el QR, pide, sigue su pedido y
 * pide la cuenta; el personal, con sesion real de Identidad, ve el pedido sin hacer nada y cancela con motivo. Las
 * defensas (llave, token de otra mesa, token inexistente) tambien se prueban por la puerta. Todo dato es de ejemplo.
 */
@SpringBootTest(properties = {"restaurantes.almacen=memoria", "inventario.almacen=memoria"})
@AutoConfigureMockMvc
class GraphQlPedidosQrTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String DISPOSITIVO = "5b0c1f0e-6a11-4d7e-9a3c-0d2f4e8a7b11";
    private static final String OTRO_DISPOSITIVO = "9f3d2c1b-7e44-4b0a-8c55-1a2b3c4d5e6f";
    private static final String PEDIR = "mutation($i: RestPedirPorQrInput!) { pedirPorQr(input: $i) { abrioLaCuenta "
            + "estado { cuenta lineas { id platillo cantidad estado mensaje } } } }";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private RepositorioRestaurantes restaurantes;
    @MockitoBean
    private IdentidadApi identidad;

    private String token(UUID negocio, String rol) {
        var token = "T" + UUID.randomUUID().toString().replace("-", "") + "xxxxxxxxxx";
        when(identidad.autenticar(token)).thenReturn(Optional.of(new SesionAutenticada(UUID.randomUUID(), "Ana", negocio,
                rol, RolesBase.porCodigo(rol).orElseThrow().permisos(), Instant.parse("2026-10-16T16:30:00Z"))));
        return token;
    }

    private JsonNode graphql(String token, String consulta, Map<String, Object> variables, String llave) throws Exception {
        var cuerpo = JSON.writeValueAsString(Map.of("query", consulta, "variables", variables));
        var peticion = post("/graphql").contentType(MediaType.APPLICATION_JSON).content(cuerpo).with(r -> {
            r.setRemoteAddr("10.8." + (int) (Math.random() * 250 + 1) + "." + (int) (Math.random() * 250 + 1));
            return r;
        });
        if (token != null) {
            peticion.header("Authorization", "Bearer " + token);
        }
        if (llave != null) {
            peticion.header("Idempotency-Key", llave);
        }
        return JSON.readTree(mvc.perform(peticion).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString());
    }

    private JsonNode graphql(String token, String consulta, Map<String, Object> variables) throws Exception {
        return graphql(token, consulta, variables, null);
    }

    private static String codigo(JsonNode respuesta) {
        return respuesta.at("/errors/0/extensions/codigo").asText();
    }

    private static JsonNode sinErrores(JsonNode r) {
        assertThat(r.has("errors")).as(r.toString()).isFalse();
        return r;
    }

    /** Un restaurante publicado con una mesa, el servicio abierto y dos platillos; devuelve lo que el comensal y el personal usan. */
    private record Armado(UUID negocio, String dueno, String mesaId, String qr, String cemita, String horchata) {
    }

    private Armado armar() throws Exception {
        var negocio = UUID.randomUUID();
        restaurantes.guardar(Restaurante.nuevo(negocio, Instant.now()).conPublicado(true, Instant.now()));
        var dueno = token(negocio, "DUENO");
        var zona = sinErrores(graphql(dueno, "mutation { crearZona(nombre: \"Salon\") { id } }", Map.of()))
                .at("/data/crearZona/id").asText();
        var mesa = sinErrores(graphql(dueno, "mutation($i: RestMesaInput!) { crearMesa(input: $i) { id } }",
                Map.of("i", Map.of("zonaId", zona, "etiqueta", "Mesa 1", "capacidad", 4)))).at("/data/crearMesa/id").asText();
        sinErrores(graphql(dueno, "mutation { abrirServicio { abierto } }", Map.of()));
        var cat = sinErrores(graphql(dueno, "mutation($i: RestCategoriaInput!) { crearCategoria(input: $i) { id } }",
                Map.of("i", Map.of("nombre", "Bebidas", "orden", 1, "estacion", "BARRA")))).at("/data/crearCategoria/id")
                .asText();
        var cemita = sinErrores(graphql(dueno, "mutation($i: RestPlatilloInput!) { crearPlatillo(input: $i) { id } }",
                Map.of("i", Map.of("nombre", "Cemita poblana", "precio", "85.00")))).at("/data/crearPlatillo/id").asText();
        var horchata = sinErrores(graphql(dueno, "mutation($i: RestPlatilloInput!) { crearPlatillo(input: $i) { id } }",
                Map.of("i", Map.of("nombre", "Agua de horchata", "precio", "35.00", "categoriaId", cat))))
                .at("/data/crearPlatillo/id").asText();
        var enlace = sinErrores(graphql(dueno, "query($m: ID!) { qrDeMesa(mesaId: $m) }", Map.of("m", mesa)))
                .at("/data/qrDeMesa").asText();
        return new Armado(negocio, dueno, mesa, enlace.substring(enlace.lastIndexOf('/') + 1), cemita, horchata);
    }

    private static Map<String, Object> pedido(String qr, String dispositivo, List<Map<String, Object>> lineas) {
        return Map.of("i", Map.of("token", qr, "dispositivoId", dispositivo, "lineas", lineas));
    }

    private static Map<String, Object> linea(String platillo, int cantidad) {
        return Map.of("platilloId", platillo, "cantidad", cantidad);
    }

    @Test
    @DisplayName("QR-03: sin sesion, mesaPorQr da la mesa y si acepta pedidos, y un token que no sirve responde nulo")
    void escanear() throws Exception {
        var a = armar();

        var mesa = sinErrores(graphql(null, "query($t: String!) { mesaPorQr(token: $t) { negocioId mesa servicioAbierto "
                + "aceptaPedidos estadoCuenta } }", Map.of("t", a.qr())));

        assertThat(mesa.at("/data/mesaPorQr/negocioId").asText()).isEqualTo(a.negocio().toString());
        assertThat(mesa.at("/data/mesaPorQr/mesa").asText()).isEqualTo("Mesa 1");
        assertThat(mesa.at("/data/mesaPorQr/aceptaPedidos").asBoolean()).isTrue();
        assertThat(mesa.at("/data/mesaPorQr/estadoCuenta").asText()).isEqualTo("SIN_CUENTA");
        var otro = sinErrores(graphql(null, "query($t: String!) { mesaPorQr(token: $t) { mesa } }",
                Map.of("t", "no-existe-ese-token")));
        assertThat(otro.at("/data/mesaPorQr").isNull()).isTrue();
    }

    @Test
    @DisplayName("QR-05, QR-06, QR-07: pedir sin cuenta la abre, va a cocina sin accion del mesero y el pedido se sigue por dispositivo")
    void pedirYSeguir() throws Exception {
        var a = armar();
        var cocinero = token(a.negocio(), "COCINERO");
        var mesero = token(a.negocio(), "MESERO");

        var hecho = sinErrores(graphql(null, PEDIR, pedido(a.qr(), DISPOSITIVO, List.of(linea(a.cemita(), 2),
                linea(a.horchata(), 1))), "pedido-http-0001"));

        assertThat(hecho.at("/data/pedirPorQr/abrioLaCuenta").asBoolean()).isTrue();
        assertThat(hecho.at("/data/pedirPorQr/estado/cuenta").asText()).isEqualTo("ABIERTA");
        assertThat(hecho.at("/data/pedirPorQr/estado/lineas")).hasSize(2);
        assertThat(hecho.at("/data/pedirPorQr/estado/lineas/0/estado").asText()).isEqualTo("EN_COCINA");
        var tablero = sinErrores(graphql(cocinero, "{ comandas { estacion lineas { platillo cantidad } } }", Map.of()));
        assertThat(tablero.at("/data/comandas")).hasSize(2);
        var cuentas = sinErrores(graphql(mesero, "{ cuentasAbiertas { numero origen comensales total } }", Map.of()));
        assertThat(cuentas.at("/data/cuentasAbiertas/0/origen").asText()).isEqualTo("QR");
        assertThat(cuentas.at("/data/cuentasAbiertas/0/comensales").isNull()).isTrue();
        assertThat(cuentas.at("/data/cuentasAbiertas/0/total").asText()).isEqualTo("205.00");

        var propio = sinErrores(graphql(null, "query($t: String!, $d: String!) { estadoDePedidoQr(token: $t, dispositivoId: $d) "
                + "{ cuenta lineas { platillo cantidad estado } } }", Map.of("t", a.qr(), "d", DISPOSITIVO)));
        var ajeno = sinErrores(graphql(null, "query($t: String!, $d: String!) { estadoDePedidoQr(token: $t, dispositivoId: $d) "
                + "{ cuenta lineas { platillo } } }", Map.of("t", a.qr(), "d", OTRO_DISPOSITIVO)));
        assertThat(propio.at("/data/estadoDePedidoQr/lineas")).hasSize(2);
        assertThat(ajeno.at("/data/estadoDePedidoQr/cuenta").asText()).isEqualTo("ABIERTA");
        assertThat(ajeno.at("/data/estadoDePedidoQr/lineas")).isEmpty();
    }

    @Test
    @DisplayName("CT-14, QR-15: repetir la llave no duplica el pedido; sin llave es LLAVE_REQUERIDA; la misma llave con otro pedido, LLAVE_REUTILIZADA")
    void llaveDeIdempotencia() throws Exception {
        var a = armar();
        var mesero = token(a.negocio(), "MESERO");
        var variables = pedido(a.qr(), DISPOSITIVO, List.of(linea(a.cemita(), 1)));

        var primero = sinErrores(graphql(null, PEDIR, variables, "pedido-http-0002"));
        var segundo = sinErrores(graphql(null, PEDIR, variables, "pedido-http-0002"));

        assertThat(segundo.at("/data")).isEqualTo(primero.at("/data"));
        var cuentas = sinErrores(graphql(mesero, "{ cuentasAbiertas { lineas { id } } }", Map.of()));
        assertThat(cuentas.at("/data/cuentasAbiertas/0/lineas")).hasSize(1);
        assertThat(codigo(graphql(null, PEDIR, variables, null))).isEqualTo("LLAVE_REQUERIDA");
        assertThat(codigo(graphql(null, PEDIR, pedido(a.qr(), DISPOSITIVO, List.of(linea(a.cemita(), 3))),
                "pedido-http-0002"))).isEqualTo("LLAVE_REUTILIZADA");
    }

    @Test
    @DisplayName("QR-10, QR-11: el mesero cancela con motivo y el dispositivo ve CANCELADO con el mensaje fijo")
    void cancelacion() throws Exception {
        var a = armar();
        var mesero = token(a.negocio(), "MESERO");
        sinErrores(graphql(null, PEDIR, pedido(a.qr(), DISPOSITIVO, List.of(linea(a.cemita(), 1), linea(a.horchata(), 1))),
                "pedido-http-0003"));
        var lineaId = sinErrores(graphql(mesero, "{ cuentasAbiertas { lineas { id nombre } } }", Map.of()))
                .at("/data/cuentasAbiertas/0/lineas/0/id").asText();

        sinErrores(graphql(mesero, "mutation($i: RestAnularLineaInput!) { anularLinea(input: $i) { total } }",
                Map.of("i", Map.of("lineaId", lineaId, "motivo", "AGOTADO"))));

        var estado = sinErrores(graphql(null, "query($t: String!, $d: String!) { estadoDePedidoQr(token: $t, dispositivoId: $d) "
                + "{ lineas { platillo estado mensaje } } }", Map.of("t", a.qr(), "d", DISPOSITIVO)));
        assertThat(estado.at("/data/estadoDePedidoQr/lineas/0/estado").asText()).isEqualTo("CANCELADO");
        assertThat(estado.at("/data/estadoDePedidoQr/lineas/0/mensaje").asText()).isEqualTo("Cancelado: agotado");
        assertThat(estado.at("/data/estadoDePedidoQr/lineas/1/estado").asText()).isEqualTo("EN_COCINA");
    }

    @Test
    @DisplayName("QR-13, QR-14: pedir la cuenta por QR la pasa a POR_COBRAR y el siguiente pedido falla con CUENTA_SOLICITADA")
    void pedirLaCuenta() throws Exception {
        var a = armar();
        var mesero = token(a.negocio(), "MESERO");
        sinErrores(graphql(null, PEDIR, pedido(a.qr(), DISPOSITIVO, List.of(linea(a.cemita(), 1))), "pedido-http-0004"));

        var estado = sinErrores(graphql(null, "mutation($t: String!, $d: String!) { pedirCuentaPorQr(token: $t, "
                + "dispositivoId: $d) { cuenta lineas { platillo } } }", Map.of("t", a.qr(), "d", DISPOSITIVO)));

        assertThat(estado.at("/data/pedirCuentaPorQr/cuenta").asText()).isEqualTo("SOLICITADA");
        var cuentas = sinErrores(graphql(mesero, "{ cuentasAbiertas { estado } }", Map.of()));
        assertThat(cuentas.at("/data/cuentasAbiertas/0/estado").asText()).isEqualTo("POR_COBRAR");
        assertThat(codigo(graphql(null, PEDIR, pedido(a.qr(), OTRO_DISPOSITIVO, List.of(linea(a.horchata(), 1))),
                "pedido-http-0005"))).isEqualTo("CUENTA_SOLICITADA");
        var mesa = sinErrores(graphql(null, "query($t: String!) { mesaPorQr(token: $t) { aceptaPedidos estadoCuenta } }",
                Map.of("t", a.qr())));
        assertThat(mesa.at("/data/mesaPorQr/aceptaPedidos").asBoolean()).isFalse();
        assertThat(mesa.at("/data/mesaPorQr/estadoCuenta").asText()).isEqualTo("SOLICITADA");
    }

    @Test
    @DisplayName("QR-19: el token de un negocio no pide en otro; un token que no sirve da QR_INVALIDO y no escribe nada")
    void tokensAjenos() throws Exception {
        var a = armar();
        var b = armar();
        var mesero = token(b.negocio(), "MESERO");

        // El token de A con un platillo de B: la mesa es de A, el platillo no existe ahi.
        assertThat(codigo(graphql(null, PEDIR, pedido(a.qr(), DISPOSITIVO, List.of(linea(b.cemita(), 1))), "pedido-http-0006")))
                .isEqualTo("PLATILLO_NO_ENCONTRADO");
        assertThat(codigo(graphql(null, PEDIR, pedido("token-que-no-existe", DISPOSITIVO, List.of(linea(a.cemita(), 1))),
                "pedido-http-0007"))).isEqualTo("QR_INVALIDO");
        var cuentas = sinErrores(graphql(mesero, "{ cuentasAbiertas { id } }", Map.of()));
        assertThat(cuentas.at("/data/cuentasAbiertas")).isEmpty();
    }

    @Test
    @DisplayName("QR-18: ninguna respuesta publica trae ids de cuenta, comensales ni totales")
    void sinDatosDeLaCuenta() throws Exception {
        var a = armar();
        sinErrores(graphql(null, PEDIR, pedido(a.qr(), DISPOSITIVO, List.of(linea(a.cemita(), 1))), "pedido-http-0008"));

        var publico = graphql(null, "query($t: String!) { mesaPorQr(token: $t) { id cuentaId total comensales } }",
                Map.of("t", a.qr()));

        assertThat(publico.has("errors")).isTrue();
        assertThat(codigo(publico)).isEqualTo("CONSULTA_INVALIDA");
    }

    @Test
    @DisplayName("SG-04: sin sesion no se llega a nada del panel de Restaurantes")
    void panelSigueCerrado() throws Exception {
        var respuesta = graphql(null, "{ salon { servicioAbierto } }", Map.of());

        assertThat(respuesta.has("errors")).isTrue();
        assertThat(codigo(respuesta)).isEqualTo("NO_AUTENTICADO");
    }
}
