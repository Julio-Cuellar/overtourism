package mx.negocio360.clientes.web;

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
import mx.negocio360.clientes.ClientesApi;
import mx.negocio360.clientes.aplicacion.PublicarAvisoComando;
import mx.negocio360.clientes.dominio.FinalidadAviso;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.SesionAutenticada;
import mx.negocio360.identidad.dominio.RolesBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Clientes de punta a punta por HTTP con sesiones reales de Identidad (los roles base) y PostgreSQL: la puerta de
 * {@code /graphql}, el despachador de comandos, los permisos por variante y por token, el aislamiento por negocio y lo
 * que todavia no esta programado (que responde {@code OPERACION_NO_DISPONIBLE}, no cifras inventadas). Todo dato es de
 * ejemplo.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GraphQlClientesTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PREPARAR = "mutation($i: ComandoInput!) { prepararComando(input: $i) "
            + "{ token resumen expiraEn advertencias } }";
    private static final String CONFIRMAR = "mutation($t: String!) { confirmarComando(token: $t) "
            + "{ ok codigo mensajePlantilla recursoId datos { clave valor } } }";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ClientesApi clientes;
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
            r.setRemoteAddr("10.7." + ip + "." + (int) (Math.random() * 250 + 1));
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

    /** El aviso de privacidad se publica por la API (el permiso es sensible y la confirmacion reforzada aun no existe). */
    private UUID negocioConAviso() {
        var negocio = UUID.randomUUID();
        var contexto = new ContextoNegocio(negocio, "responsable-prueba");
        var aviso = new PublicarAvisoComando("Taller Mixcoatl", "Calle 5 de Mayo 10, Puebla",
                List.of(new FinalidadAviso("VENTA", false), new FinalidadAviso("PROMOCIONES", true)),
                "Usamos tu nombre y telefono para atenderte.", "Aviso integral de ejemplo.", "privacidad@taller.test");
        clientes.confirmar(contexto, clientes.preparar(contexto, aviso).token());
        return negocio;
    }

    /** Un celular de Puebla que no se repite entre pruebas. */
    private static String telefonoNuevo() {
        return "+52222" + String.format("%07d", java.util.concurrent.ThreadLocalRandom.current().nextInt(10_000_000));
    }

    private Map<String, Object> alta(String nombre) {
        return Map.of("registrarCliente", Map.of("nombre", nombre, "apellidos", "Pérez",
                "telefono", telefonoNuevo(), "avisoVersion", 1));
    }

    @Test
    @DisplayName("el cajero registra un cliente por GraphQL, lo encuentra al sugerir y ve el aviso vigente")
    void cajero_registraYSugiere() throws Exception {
        var negocio = negocioConAviso();
        var cajero = sesion(negocio, "CAJERO");

        var preparada = graphql(cajero, PREPARAR, Map.of("i", alta("Josefina")));
        assertThat(preparada.has("errors")).as(preparada.toString()).isFalse();
        var token = preparada.at("/data/prepararComando/token").asText();
        assertThat(token).startsWith("clientes.");
        var confirmada = graphql(cajero, CONFIRMAR, Map.of("t", token));
        assertThat(confirmada.at("/data/confirmarComando/ok").asBoolean()).as(confirmada.toString()).isTrue();
        var id = confirmada.at("/data/confirmarComando/recursoId").asText();

        var sugeridos = graphql(cajero, "{ sugerirClientes(texto: \"josefina\") { id nombreCompleto "
                + "telefonoEnmascarado } }", Map.of());
        assertThat(sugeridos.at("/data/sugerirClientes/0/id").asText()).isEqualTo(id);
        assertThat(sugeridos.at("/data/sugerirClientes/0/telefonoEnmascarado").asText()).startsWith("••••");
        var aviso = graphql(cajero, "{ avisoPrivacidadVigente { version responsableNombre finalidades { codigo "
                + "requiereConsentimiento } } }", Map.of());
        assertThat(aviso.at("/data/avisoPrivacidadVigente/version").asInt()).isEqualTo(1);
        assertThat(aviso.at("/data/avisoPrivacidadVigente/finalidades")).hasSize(2);
    }

    @Test
    @DisplayName("el mesero busca clientes pero no los registra; sin el permiso del comando falla en la puerta")
    void mesero_sugierePeroNoRegistra() throws Exception {
        var negocio = negocioConAviso();
        var mesero = sesion(negocio, "MESERO");

        assertThat(graphql(mesero, "{ sugerirClientes(texto: \"ab\") { id } }", Map.of()).has("errors")).isFalse();
        assertThat(codigo(graphql(mesero, PREPARAR, Map.of("i", alta("Pedro"))))).isEqualTo("PERMISO_DENEGADO");
        assertThat(codigo(graphql(mesero, "{ avisoPrivacidadVigente { version } }", Map.of())))
                .isEqualTo("PERMISO_DENEGADO");
    }

    @Test
    @DisplayName("D-126: si le quitan clientes.cliente.registrar entre preparar y confirmar, falla y el token sigue vivo")
    void permisoRevocadoEntrePasos() throws Exception {
        var negocio = negocioConAviso();
        var cajero = sesion(negocio, "CAJERO");
        var token = graphql(cajero, PREPARAR, Map.of("i", alta("Lupita"))).at("/data/prepararComando/token").asText();
        when(identidad.autenticar(cajero.token())).thenReturn(Optional.of(new SesionAutenticada(cajero.cuentaId(), "Ana",
                negocio, "MESERO", RolesBase.porCodigo("MESERO").orElseThrow().permisos(),
                Instant.parse("2026-10-16T16:30:00Z"))));

        assertThat(codigo(graphql(cajero, CONFIRMAR, Map.of("t", token)))).isEqualTo("PERMISO_DENEGADO");

        when(identidad.autenticar(cajero.token())).thenReturn(Optional.of(new SesionAutenticada(cajero.cuentaId(), "Ana",
                negocio, "CAJERO", RolesBase.porCodigo("CAJERO").orElseThrow().permisos(),
                Instant.parse("2026-10-16T16:30:00Z"))));
        var bien = graphql(cajero, CONFIRMAR, Map.of("t", token));
        assertThat(bien.at("/data/confirmarComando/ok").asBoolean()).as(bien.toString()).isTrue();
    }

    @Test
    @DisplayName("dos negocios no se ven los clientes, y un token de otro negocio no confirma")
    void dosNegocios_noSeVen() throws Exception {
        var a = sesion(negocioConAviso(), "DUENO");
        var b = sesion(negocioConAviso(), "DUENO");
        var token = graphql(a, PREPARAR, Map.of("i", alta("Mariana"))).at("/data/prepararComando/token").asText();
        graphql(a, CONFIRMAR, Map.of("t", token));

        assertThat(graphql(a, "{ sugerirClientes(texto: \"mariana\") { id } }", Map.of())
                .at("/data/sugerirClientes")).hasSize(1);
        assertThat(graphql(b, "{ sugerirClientes(texto: \"mariana\") { id } }", Map.of())
                .at("/data/sugerirClientes")).isEmpty();
        var otro = graphql(a, PREPARAR, Map.of("i", alta("Tomas"))).at("/data/prepararComando/token").asText();
        assertThat(codigo(graphql(b, CONFIRMAR, Map.of("t", otro)))).isEqualTo("TOKEN_INVALIDO");
    }

    @Test
    @DisplayName("lo que Clientes aun no programa responde OPERACION_NO_DISPONIBLE o COMANDO_NO_DISPONIBLE, sin inventar cifras")
    void loPendiente_noDisponible() throws Exception {
        var dueno = sesion(negocioConAviso(), "DUENO");

        assertThat(codigo(graphql(dueno, "{ panoramaClientes { total } }", Map.of())))
                .isEqualTo("OPERACION_NO_DISPONIBLE");
        assertThat(codigo(graphql(dueno, "{ clientes { nodos { id } } }", Map.of())))
                .isEqualTo("OPERACION_NO_DISPONIBLE");
        assertThat(codigo(graphql(dueno, PREPARAR, Map.of("i", Map.of("registrarConsentimiento", Map.of("clienteId",
                UUID.randomUUID().toString(), "finalidad", "PROMOCIONES", "estado", "OTORGADO", "medio", "PANEL",
                "avisoVersion", 1))))))
                .isEqualTo("COMANDO_NO_DISPONIBLE");
    }

    @Test
    @DisplayName("un texto de sugerencia de un solo caracter es una consulta invalida")
    void sugerir_textoCorto() throws Exception {
        var cajero = sesion(negocioConAviso(), "CAJERO");

        assertThat(codigo(graphql(cajero, "{ sugerirClientes(texto: \"a\") { id } }", Map.of())))
                .isEqualTo("CONSULTA_INVALIDA");
    }
}
