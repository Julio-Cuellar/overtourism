package mx.negocio360.restaurantes.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.SesionAutenticada;
import mx.negocio360.identidad.dominio.RolesBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * R5 de punta a punta por HTTP, con la aplicacion completa y sesiones reales de Identidad (roles base): abrir una cuenta,
 * pedir, enviar a cocina, el tablero, pedir la cuenta, la cuenta para cobro y los comandos de merma por preparar y
 * confirmar. Cada puesto ve y hace solo lo suyo. Todo dato es de ejemplo.
 */
@SpringBootTest(properties = {"restaurantes.almacen=memoria", "inventario.almacen=memoria"})
@AutoConfigureMockMvc
class GraphQlCuentasTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PREPARAR = "mutation($i: ComandoInput!) { prepararComando(input: $i) { token resumen } }";
    private static final String CONFIRMAR = "mutation($t: String!) { confirmarComando(token: $t) { ok } }";

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
                    r.setRemoteAddr("10.9." + (int) (Math.random() * 250 + 1) + "." + (int) (Math.random() * 250 + 1));
                    return r;
                });
        return JSON.readTree(mvc.perform(peticion).andExpect(status().isOk()).andReturn().getResponse()
                .getContentAsString());
    }

    private static String codigo(JsonNode respuesta) {
        return respuesta.at("/errors/0/extensions/codigo").asText();
    }

    private static JsonNode sinErrores(JsonNode r) {
        assertThat(r.has("errors")).as(r.toString()).isFalse();
        return r;
    }

    /** Un restaurante con una mesa, el servicio abierto y dos platillos; devuelve los ids. */
    private record Armado(UUID negocio, String dueno, String mesaId, String cemita, String horchata) {
    }

    private Armado armar() throws Exception {
        var negocio = UUID.randomUUID();
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
        return new Armado(negocio, dueno, mesa, cemita, horchata);
    }

    private String abrirConDosLineas(Armado a, String token) throws Exception {
        var cuenta = sinErrores(graphql(token, "mutation($i: RestAbrirCuentaInput!) { abrirCuenta(input: $i) { id numero "
                + "estado } }", Map.of("i", Map.of("mesaId", a.mesaId(), "comensales", 2)))).at("/data/abrirCuenta/id")
                .asText();
        sinErrores(graphql(token, "mutation($i: RestAgregarLineaInput!) { agregarLinea(input: $i) { total } }",
                Map.of("i", Map.of("cuentaId", cuenta, "platilloId", a.cemita(), "cantidad", 2, "nota", "sin cebolla",
                        "marcaAlergia", true))));
        sinErrores(graphql(token, "mutation($i: RestAgregarLineaInput!) { agregarLinea(input: $i) { total } }",
                Map.of("i", Map.of("cuentaId", cuenta, "platilloId", a.horchata(), "cantidad", 1))));
        return cuenta;
    }

    @Test
    @DisplayName("del pedido al cobro: el mesero abre y envia, cocina y barra avanzan, se pide la cuenta y queda lista para cobrar")
    void flujoCompleto() throws Exception {
        var a = armar();
        var mesero = token(a.negocio(), "MESERO");
        var cocinero = token(a.negocio(), "COCINERO");
        var cuenta = abrirConDosLineas(a, mesero);

        var enviadas = sinErrores(graphql(mesero, "mutation($c: ID!) { enviarACocina(cuentaId: $c) { id estacion estado } }",
                Map.of("c", cuenta)));
        assertThat(enviadas.at("/data/enviarACocina")).hasSize(2);

        var tablero = sinErrores(graphql(cocinero, "{ comandas(estacion: COCINA) { id mesa numeroDeCuenta estado retrasada "
                + "lineas { platillo cantidad nota marcaAlergia anulada } } }", Map.of()));
        assertThat(tablero.at("/data/comandas")).hasSize(1);
        var comanda = tablero.at("/data/comandas/0");
        assertThat(comanda.at("/mesa").asText()).isEqualTo("Mesa 1");
        assertThat(comanda.at("/lineas/0/platillo").asText()).isEqualTo("Cemita poblana");
        assertThat(comanda.at("/lineas/0/marcaAlergia").asBoolean()).isTrue();

        var id = comanda.at("/id").asText();
        sinErrores(graphql(cocinero, "mutation($i: ID!) { avanzarComanda(id: $i) { estado } }", Map.of("i", id)));
        var lista = sinErrores(graphql(cocinero, "mutation($i: ID!) { avanzarComanda(id: $i) { estado segundosDePreparacion } }",
                Map.of("i", id)));
        assertThat(lista.at("/data/avanzarComanda/estado").asText()).isEqualTo("LISTA");
        var entregada = sinErrores(graphql(mesero, "mutation($i: ID!) { avanzarComanda(id: $i) { estado } }", Map.of("i", id)));
        assertThat(entregada.at("/data/avanzarComanda/estado").asText()).isEqualTo("ENTREGADA");

        var pedida = sinErrores(graphql(mesero, "mutation($c: ID!) { pedirCuenta(cuentaId: $c) { estado total } }",
                Map.of("c", cuenta)));
        assertThat(pedida.at("/data/pedirCuenta/estado").asText()).isEqualTo("POR_COBRAR");
        assertThat(pedida.at("/data/pedirCuenta/total").asText()).isEqualTo("205.00");

        var cobro = sinErrores(graphql(mesero, "query($c: ID!) { cuentaParaCobro(cuentaId: $c) { referencia total "
                + "lineas { productoId descripcion cantidad } } }", Map.of("c", cuenta)));
        assertThat(cobro.at("/data/cuentaParaCobro/referencia").asText()).isEqualTo("restaurantes:cuenta:" + cuenta);
        assertThat(cobro.at("/data/cuentaParaCobro/lineas")).hasSize(2);
        var salon = sinErrores(graphql(mesero, "{ salon { disponibilidad { libres total } mesas { estado cuenta { numero "
                + "estado total } } } }", Map.of()));
        assertThat(salon.at("/data/salon/mesas/0/estado").asText()).isEqualTo("OCUPADA");
        assertThat(salon.at("/data/salon/mesas/0/cuenta/estado").asText()).isEqualTo("POR_COBRAR");
        assertThat(salon.at("/data/salon/disponibilidad/libres").asInt()).isZero();
    }

    @Test
    @DisplayName("cada puesto hace solo lo suyo: el cocinero no abre cuentas ni el mesero marca lista una comanda")
    void permisosPorPuesto() throws Exception {
        var a = armar();
        var mesero = token(a.negocio(), "MESERO");
        var cocinero = token(a.negocio(), "COCINERO");
        var cuenta = abrirConDosLineas(a, mesero);
        graphql(mesero, "mutation($c: ID!) { enviarACocina(cuentaId: $c) { id } }", Map.of("c", cuenta));
        var comanda = graphql(cocinero, "{ comandas(estacion: COCINA) { id } }", Map.of()).at("/data/comandas/0/id").asText();

        var abrir = graphql(cocinero, "mutation($i: RestAbrirCuentaInput!) { abrirCuenta(input: $i) { id } }",
                Map.of("i", Map.of("mesaId", a.mesaId(), "comensales", 1)));
        var marcarLista = graphql(mesero, "mutation($i: ID!) { avanzarComanda(id: $i) { estado } }", Map.of("i", comanda));
        var cuentaVista = graphql(cocinero, "query($c: ID!) { cuenta(id: $c) { id } }", Map.of("c", cuenta));

        assertThat(codigo(abrir)).isEqualTo("PERMISO_DENEGADO");
        assertThat(codigo(marcarLista)).isEqualTo("PERMISO_DENEGADO");
        assertThat(codigo(cuentaVista)).isEqualTo("PERMISO_DENEGADO");
    }

    @Test
    @DisplayName("anular una linea exige motivo; la cuenta bajo y el comensal veria el texto fijo del motivo")
    void anularConMotivo() throws Exception {
        var a = armar();
        var mesero = token(a.negocio(), "MESERO");
        var cuenta = abrirConDosLineas(a, mesero);
        graphql(mesero, "mutation($c: ID!) { enviarACocina(cuentaId: $c) { id } }", Map.of("c", cuenta));
        var linea = graphql(mesero, "query($c: ID!) { cuenta(id: $c) { lineas { id nombre } } }", Map.of("c", cuenta))
                .at("/data/cuenta/lineas/1/id").asText();

        var sinMotivo = graphql(mesero, "mutation($i: RestAnularLineaInput!) { anularLinea(input: $i) { total } }",
                Map.of("i", Map.of("lineaId", linea, "motivo", "OTRO")));
        var anulada = sinErrores(graphql(mesero, "mutation($i: RestAnularLineaInput!) { anularLinea(input: $i) { total "
                + "lineas { estado mensajeDelMotivo } } }", Map.of("i", Map.of("lineaId", linea, "motivo", "AGOTADO"))));

        assertThat(codigo(sinMotivo)).isEqualTo("MOTIVO_REQUERIDO");
        assertThat(anulada.at("/data/anularLinea/total").asText()).isEqualTo("170.00");
        assertThat(anulada.at("/data/anularLinea/lineas/1/estado").asText()).isEqualTo("ANULADA");
        assertThat(anulada.at("/data/anularLinea/lineas/1/mensajeDelMotivo").asText()).isEqualTo("Cancelado: agotado");
    }

    @Test
    @DisplayName("una linea ya lista no la anula el mesero por la mutacion directa; el gerente lo hace por preparar y confirmar")
    void anularPreparadaPorComando() throws Exception {
        var a = armar();
        var mesero = token(a.negocio(), "MESERO");
        var cocinero = token(a.negocio(), "COCINERO");
        var gerente = token(a.negocio(), "GERENTE");
        var cuenta = abrirConDosLineas(a, mesero);
        graphql(mesero, "mutation($c: ID!) { enviarACocina(cuentaId: $c) { id } }", Map.of("c", cuenta));
        var comanda = graphql(cocinero, "{ comandas(estacion: COCINA) { id } }", Map.of()).at("/data/comandas/0/id").asText();
        graphql(cocinero, "mutation($i: ID!) { avanzarComanda(id: $i) { estado } }", Map.of("i", comanda));
        graphql(cocinero, "mutation($i: ID!) { avanzarComanda(id: $i) { estado } }", Map.of("i", comanda));
        var linea = graphql(mesero, "query($c: ID!) { cuenta(id: $c) { lineas { id } } }", Map.of("c", cuenta))
                .at("/data/cuenta/lineas/0/id").asText();

        var directa = graphql(mesero, "mutation($i: RestAnularLineaInput!) { anularLinea(input: $i) { total } }",
                Map.of("i", Map.of("lineaId", linea, "motivo", "ERROR_DEL_PEDIDO")));
        var preparada = sinErrores(graphql(gerente, PREPARAR, Map.of("i", Map.of("anularLineaPreparada",
                Map.of("lineaId", linea, "motivo", "ERROR_DEL_PEDIDO")))));
        var confirmada = sinErrores(graphql(gerente, CONFIRMAR, Map.of("t",
                preparada.at("/data/prepararComando/token").asText())));

        assertThat(codigo(directa)).isEqualTo("LINEA_PREPARADA");
        assertThat(preparada.at("/data/prepararComando/resumen").asText()).contains("merma");
        assertThat(confirmada.at("/data/confirmarComando/ok").asBoolean()).isTrue();
        var despues = graphql(mesero, "query($c: ID!) { cuenta(id: $c) { total } }", Map.of("c", cuenta));
        assertThat(despues.at("/data/cuenta/total").asText()).isEqualTo("35.00");
    }

    @Test
    @DisplayName("cancelar una cuenta con platillos preparados exige el comando de gerente; el mesero no puede prepararlo")
    void cancelarPreparadaPorComando() throws Exception {
        var a = armar();
        var mesero = token(a.negocio(), "MESERO");
        var cocinero = token(a.negocio(), "COCINERO");
        var gerente = token(a.negocio(), "GERENTE");
        var cuenta = abrirConDosLineas(a, mesero);
        graphql(mesero, "mutation($c: ID!) { enviarACocina(cuentaId: $c) { id } }", Map.of("c", cuenta));
        var comanda = graphql(cocinero, "{ comandas(estacion: COCINA) { id } }", Map.of()).at("/data/comandas/0/id").asText();
        graphql(cocinero, "mutation($i: ID!) { avanzarComanda(id: $i) { estado } }", Map.of("i", comanda));
        graphql(cocinero, "mutation($i: ID!) { avanzarComanda(id: $i) { estado } }", Map.of("i", comanda));

        var directa = graphql(mesero, "mutation($c: ID!) { cancelarCuenta(cuentaId: $c, motivo: SIN_COMENSALES) { id } }",
                Map.of("c", cuenta));
        var delMesero = graphql(mesero, PREPARAR, Map.of("i", Map.of("cancelarCuentaPreparada",
                Map.of("cuentaId", cuenta, "motivo", "SIN_COMENSALES"))));
        var preparada = sinErrores(graphql(gerente, PREPARAR, Map.of("i", Map.of("cancelarCuentaPreparada",
                Map.of("cuentaId", cuenta, "motivo", "SIN_COMENSALES")))));
        sinErrores(graphql(gerente, CONFIRMAR, Map.of("t", preparada.at("/data/prepararComando/token").asText())));

        assertThat(codigo(directa)).isEqualTo("CUENTA_CON_LINEAS_PREPARADAS");
        assertThat(codigo(delMesero)).isEqualTo("PERMISO_DENEGADO");
        var estado = graphql(gerente, "query($c: ID!) { cuenta(id: $c) { estado } }", Map.of("c", cuenta));
        assertThat(estado.at("/data/cuenta/estado").asText()).isEqualTo("CANCELADA");
    }

    @Test
    @DisplayName("un negocio no ve las cuentas ni las comandas de otro")
    void aislado() throws Exception {
        var a = armar();
        var mesero = token(a.negocio(), "MESERO");
        var cuenta = abrirConDosLineas(a, mesero);
        graphql(mesero, "mutation($c: ID!) { enviarACocina(cuentaId: $c) { id } }", Map.of("c", cuenta));
        var otro = token(UUID.randomUUID(), "DUENO");

        var cuentaAjena = graphql(otro, "query($c: ID!) { cuenta(id: $c) { id } }", Map.of("c", cuenta));
        var tablero = graphql(otro, "{ comandas { id } }", Map.of());

        assertThat(codigo(cuentaAjena)).isEqualTo("CUENTA_NO_ENCONTRADA");
        assertThat(tablero.at("/data/comandas")).isEmpty();
    }
}
