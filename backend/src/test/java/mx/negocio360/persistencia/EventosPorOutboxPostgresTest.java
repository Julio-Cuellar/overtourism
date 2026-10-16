package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import mx.negocio360.clientes.eventos.ClienteRegistrado;
import mx.negocio360.clientes.eventos.OrigenCliente;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.identidad.eventos.MembresiaAgregada;
import mx.negocio360.identidad.eventos.NegocioRegistroSolicitado;
import mx.negocio360.identidad.infraestructura.CanalSimulado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Los eventos reales de Identidad y Clientes pasan por el bus de Spring y quedan en el outbox (event_publication) en
 * la misma transaccion que el cambio, y su listener los completa. Va contra PostgreSQL de verdad.
 */
@SpringBootTest
@AutoConfigureMockMvc
class EventosPorOutboxPostgresTest {

    private static final Duration ESPERA = Duration.ofSeconds(10);
    private static final AtomicInteger SECUENCIA = new AtomicInteger(70000);

    @Autowired
    private MockMvc mvc;
    @Autowired
    private TransactionTemplate transaccion;
    @Autowired
    private CanalSimulado canal;
    @Autowired
    private org.springframework.context.ApplicationEventPublisher publicador;
    @Autowired
    private mx.negocio360.clientes.aplicacion.puertos.PublicadorEventos publicadorDeClientes;

    /** Filas del outbox cuyo evento es de ese tipo y habla de ese negocio: (tipo, completado). */
    private static Boolean completado(String tipo, UUID negocioId) throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            var filas = sesion.columna("select bool_and(completion_date is not null) from plataforma.event_publication "
                            + "where serialized_event like '%\"tipo\":\"' || ? || '\"%' "
                            + "and serialized_event like '%' || ? || '%'", Boolean.class, tipo, negocioId.toString());
            return filas.isEmpty() ? null : filas.get(0);
        }
    }

    /** Una cuenta recien registrada: su token y el negocio que pidio y aun no esta registrado. */
    private record CuentaConPedido(String token, UUID negocioId) {
    }

    private String registrarCuentaConNegocioPendiente() throws Exception {
        return registrarCuentaConPedido().negocioId().toString();
    }

    private CuentaConPedido registrarCuentaConPedido() throws Exception {
        var n = SECUENCIA.incrementAndGet();
        var telefono = String.format("22291%05d", n);
        var ip = "10.9." + (n / 250) + "." + (n % 250 + 1);
        var correo = "u" + telefono + "@example.com";
        var formulario = "{\"nombre\":\"Ana\",\"apellidos\":\"López Pérez\",\"correo\":\"" + correo
                + "\",\"telefono\":\"" + telefono + "\",\"aceptaAviso\":true,"
                + "\"negocio\":{\"nombre\":\"Fonda Doña Chela\",\"giro\":\"RESTAURANTE\"}}";
        mvc.perform(post("/registro").contentType(MediaType.APPLICATION_JSON).content(formulario)
                .with(peticion -> {
                    peticion.setRemoteAddr(ip);
                    return peticion;
                })).andExpect(status().is2xxSuccessful());
        var codigo = canal.ultimoCodigoDe(correo).orElseThrow();
        var respuesta = mvc.perform(post("/registro/verificacion").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"correo\":\"" + correo + "\",\"codigo\":\"" + codigo + "\"}")
                        .with(peticion -> {
                            peticion.setRemoteAddr(ip);
                            return peticion;
                        }))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String negocio = JsonPath.read(respuesta, "$.negociosPendientes[0].id");
        String token = JsonPath.read(respuesta, "$.token");
        return new CuentaConPedido(token, UUID.fromString(negocio));
    }

    private int negociosDe(String token) throws Exception {
        var perfil = mvc.perform(get("/auth/yo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.<java.util.List<?>>read(perfil, "$.negocios").size();
    }

    private String cuentaDe(String token) throws Exception {
        var perfil = mvc.perform(get("/auth/yo").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(perfil, "$.cuenta.id");
    }

    private int filasDelTipo(String tipo, UUID negocioId) throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            // Cada modulo que escucha el bus deja su propia fila por evento: se cuentan los eventos, no las filas.
            return sesion.consultar("select count(distinct serialized_event) from plataforma.event_publication "
                    + "where serialized_event like '%\"tipo\":\"' || ? || '\"%' and serialized_event like '%' || ? "
                    + "|| '%'", Long.class, tipo, negocioId.toString()).intValue();
        }
    }

    private SobreEvento negocioRegistrado(UUID negocioId, String cuentaId, UUID id) {
        var ahora = Instant.now();
        return new SobreEvento(id, "negocio.registrado", 1, negocioId, "negocios", ahora, UUID.randomUUID(), null,
                Map.of("solicitado_por", cuentaId, "nombre", "Fonda Doña Chela", "giro", "RESTAURANTE"));
    }

    @Test
    @DisplayName("pedir un negocio deja negocio.registro_solicitado en el outbox y el escucha lo completa")
    void solicitarNegocio_dejaElEventoEnElOutbox() throws Exception {
        var negocio = UUID.fromString(registrarCuentaConNegocioPendiente());

        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(completado(NegocioRegistroSolicitado.TIPO, negocio)).isTrue());
    }

    @Test
    @DisplayName("al registrarse el negocio, identidad.membresia_agregada tambien pasa por el outbox")
    void negocioRegistrado_dejaMembresiaAgregadaEnElOutbox() throws Exception {
        var negocio = UUID.fromString(registrarCuentaConNegocioPendiente());

        mvc.perform(post("/dev/negocios/" + negocio + "/registrado")).andExpect(status().isNoContent());

        await().atMost(ESPERA).untilAsserted(() -> assertThat(completado(MembresiaAgregada.TIPO, negocio)).isTrue());
    }

    @Test
    @DisplayName("el publicador de Clientes escribe en el outbox dentro de una transaccion y se completa")
    void clientes_publicadorDeLaPlataforma_escribeEnElOutbox() throws Exception {
        var ahora = Instant.now();
        var negocio = UUID.randomUUID();
        var cliente = UUID.randomUUID();
        var evento = new SobreEvento(UuidV7.generar(ahora), ClienteRegistrado.TIPO, ClienteRegistrado.VERSION, negocio,
                "clientes", ahora, cliente, null, new ClienteRegistrado(cliente, OrigenCliente.PANEL).datos());

        transaccion.executeWithoutResult(estado -> publicadorDeClientes.publicar(evento));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(completado(ClienteRegistrado.TIPO, negocio)).isTrue());
    }

    @Test
    @DisplayName("el publicador de Clientes rechaza publicar fuera de una transaccion")
    void clientes_publicadorDeLaPlataforma_sinTransaccionFalla() {
        var ahora = Instant.now();
        var evento = new SobreEvento(UuidV7.generar(ahora), ClienteRegistrado.TIPO, ClienteRegistrado.VERSION,
                UUID.randomUUID(), "clientes", ahora, UUID.randomUUID(), null, Map.of());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> publicadorDeClientes.publicar(evento))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("negocio.registrado por el bus real llega al escucha de Identidad, que deja al dueno una sola vez aunque se entregue dos veces")
    void negocioRegistrado_porElBus_seAplicaUnaVezAunqueSeRepita() throws Exception {
        var cuenta = registrarCuentaConPedido();
        var cuentaId = cuentaDe(cuenta.token());
        var aviso = negocioRegistrado(cuenta.negocioId(), cuentaId, UuidV7.generar(Instant.now()));
        var repetido = negocioRegistrado(cuenta.negocioId(), cuentaId, UuidV7.generar(Instant.now()));

        transaccion.executeWithoutResult(estado -> publicador.publishEvent(aviso));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(negociosDe(cuenta.token())).isEqualTo(1));
        transaccion.executeWithoutResult(estado -> publicador.publishEvent(aviso));
        transaccion.executeWithoutResult(estado -> publicador.publishEvent(repetido));
        await().during(Duration.ofSeconds(2)).atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(negociosDe(cuenta.token())).isEqualTo(1);
            assertThat(filasDelTipo(MembresiaAgregada.TIPO, cuenta.negocioId())).isEqualTo(1);
        });
    }
}
