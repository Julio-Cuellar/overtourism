package mx.negocio360.identidad.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.negocios.infraestructura.AvisosDeAlta;
import mx.negocio360.seguridad.ClienteWebSocket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.MockMvc;

/**
 * W1 y SG-16 contra un servidor real: la suscripcion {@code altaDeNegocio} por WebSocket. Es para quien acaba de registrarse y
 * todavia no tiene negocio activo, asi que la conexion se acepta sin negocio; el aviso es de la cuenta (nunca de otra) y la
 * suscripcion pasa por el tope de la plataforma. Lo que si pide negocio se sigue negando en la misma conexion.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "seguridad.websocket.suscripciones-por-cuenta=2", "spring.graphql.websocket.connection-init-timeout=2s"})
@AutoConfigureMockMvc
class AltaDeNegocioPorWebSocketTest {

    private static final Duration ESPERA = Duration.ofSeconds(5);
    private static final Instant AHORA = Instant.parse("2026-10-16T16:00:00Z");
    private static final String SUSCRIPCION = "subscription { altaDeNegocio { negocioId estado motivo } }";

    @LocalServerPort
    private int puerto;

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AvisosDeAlta avisos;

    @Autowired
    private mx.negocio360.compartido.graphql.ControlDeSuscripciones control;

    private record Cuenta(String token, UUID id) {
    }

    private Cuenta cuentaSinNegocio() throws Exception {
        var token = new FlujoHttp(mvc).tokenDeRegistro(FlujoHttp.telefono(), FlujoHttp.ipNueva());
        var yo = mvc.perform(get("/auth/yo").header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new Cuenta(token, UUID.fromString(JsonPath.read(yo, "$.cuenta.id")));
    }

    private ClienteWebSocket conectado(String token) throws Exception {
        var cliente = ClienteWebSocket.conectar(puerto, null);
        cliente.iniciar(token);
        assertThat(cliente.esperar("connection_ack", ESPERA)).isNotNull();
        return cliente;
    }

    private static SobreEvento rechazo(UUID negocio, UUID cuenta) {
        return new SobreEvento(UuidV7.generar(AHORA), "negocio.registro_rechazado", 1, negocio, "negocios", AHORA,
                UuidV7.generar(AHORA), null, Map.of("solicitado_por", cuenta.toString(), "nombre", "Farmacia",
                        "giro", "FARMACIA", "motivo", "GIRO_INVALIDO"));
    }

    private void esperarSuscripciones(int cuantas) throws Exception {
        var limite = System.nanoTime() + ESPERA.toNanos();
        while (avisos.suscritas() < cuantas && System.nanoTime() < limite) {
            Thread.sleep(25);
        }
        assertThat(avisos.suscritas()).isGreaterThanOrEqualTo(cuantas);
    }

    @Test
    @DisplayName("una cuenta sin negocio se conecta, se suscribe, recibe el aviso de su alta y el flujo se cierra solo")
    void sinNegocio_recibeSuAviso() throws Exception {
        var cuenta = cuentaSinNegocio();
        var negocio = UuidV7.generar(AHORA);
        try (var cliente = conectado(cuenta.token())) {
            cliente.suscribir("1", SUSCRIPCION);
            esperarSuscripciones(1);

            avisos.alRecibir(rechazo(negocio, cuenta.id()));

            var siguiente = cliente.esperar("next", ESPERA);
            assertThat(siguiente.path("id").asText()).isEqualTo("1");
            assertThat(siguiente.at("/payload/data/altaDeNegocio/negocioId").asText()).isEqualTo(negocio.toString());
            assertThat(siguiente.at("/payload/data/altaDeNegocio/estado").asText()).isEqualTo("RECHAZADO");
            assertThat(siguiente.at("/payload/data/altaDeNegocio/motivo").asText()).isEqualTo("GIRO_INVALIDO");
            assertThat(cliente.esperar("complete", ESPERA).path("id").asText()).isEqualTo("1");
        }
    }

    @Test
    @DisplayName("SG-16: el aviso de una cuenta no llega a otra que tambien esta suscrita")
    void otraCuenta_noRecibeElAvisoAjeno() throws Exception {
        var una = cuentaSinNegocio();
        var otra = cuentaSinNegocio();
        try (var deUna = conectado(una.token()); var deOtra = conectado(otra.token())) {
            deUna.suscribir("1", SUSCRIPCION);
            deOtra.suscribir("2", SUSCRIPCION);
            esperarSuscripciones(2);

            avisos.alRecibir(rechazo(UuidV7.generar(AHORA), una.id()));

            assertThat(deUna.esperar("next", ESPERA).at("/payload/data/altaDeNegocio/estado").asText())
                    .isEqualTo("RECHAZADO");
            assertThat(deOtra.siguienteSinPings(Duration.ofMillis(700))).isNull();
        }
    }

    @Test
    @DisplayName("en la misma conexion, lo que pide negocio activo se niega con SIN_NEGOCIO_ACTIVO y no se ejecuta")
    void sinNegocio_lasOperacionesDeNegocioSeNiegan() throws Exception {
        var cuenta = cuentaSinNegocio();
        try (var cliente = conectado(cuenta.token())) {
            cliente.suscribir("1", "{ miNegocio { id } }");

            var siguiente = cliente.esperar("next", ESPERA);

            assertThat(siguiente.at("/payload/errors/0/extensions/codigo").asText()).isEqualTo("SIN_NEGOCIO_ACTIVO");
            assertThat(siguiente.at("/payload/data/miNegocio").isMissingNode()
                    || siguiente.at("/payload/data/miNegocio").isNull()).isTrue();
        }
    }

    @Test
    @DisplayName("SG-15: altaDeNegocio ocupa un lugar del tope de la cuenta mientras esta abierta y lo libera al terminar")
    void altaDeNegocio_ocupaYLiberaSuLugar() throws Exception {
        var cuenta = cuentaSinNegocio();
        try (var cliente = conectado(cuenta.token())) {
            cliente.suscribir("1", SUSCRIPCION);
            esperarSuscripciones(1);
            assertThat(control.activas(cuenta.id().toString())).isEqualTo(1);

            avisos.alRecibir(rechazo(UuidV7.generar(AHORA), cuenta.id()));
            assertThat(cliente.esperar("next", ESPERA).path("id").asText()).isEqualTo("1");
            assertThat(cliente.esperar("complete", ESPERA).path("id").asText()).isEqualTo("1");

            var limite = System.nanoTime() + ESPERA.toNanos();
            while (control.activas(cuenta.id().toString()) > 0 && System.nanoTime() < limite) {
                Thread.sleep(25);
            }
            assertThat(control.activas(cuenta.id().toString())).isZero();
        }
    }

    @Test
    @DisplayName("una segunda suscripcion de la misma cuenta cierra la primera: nunca hay dos avisos abiertos para ella")
    void unaPorCuenta_laSegundaCierraLaPrimera() throws Exception {
        var cuenta = cuentaSinNegocio();
        try (var cliente = conectado(cuenta.token())) {
            cliente.suscribir("1", SUSCRIPCION);
            esperarSuscripciones(1);

            cliente.suscribir("2", SUSCRIPCION);

            assertThat(cliente.esperar("complete", ESPERA).path("id").asText()).isEqualTo("1");
        }
    }
}
