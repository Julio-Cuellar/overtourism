package mx.negocio360.negocios.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.negocios.AltaVista.EstadoAlta;
import mx.negocio360.negocios.aplicacion.AltasApi;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioNegocios;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * De punta a punta por el bus real y el outbox: Identidad publica {@code negocio.registro_solicitado} y Negocios crea el
 * negocio solo, en otro hilo y en su propia transaccion (R2, R3). Con Postgres de verdad.
 */
@SpringBootTest
class AltaPorElBusTest {

    private static final Instant AHORA = Instant.parse("2026-10-16T16:00:00Z");

    @Autowired
    private ApplicationEventPublisher bus;
    @Autowired
    private TransactionTemplate transaccion;
    @Autowired
    private RepositorioNegocios negocios;
    @Autowired
    private AltasApi altas;
    @Autowired
    private JdbcClient jdbc;

    private void publicarPedido(UUID negocio, UUID cuenta, String nombre, String giro) {
        var sobre = new SobreEvento(UuidV7.generar(AHORA), "negocio.registro_solicitado", 1, negocio, "identidad", AHORA,
                UuidV7.generar(AHORA), null, Map.of("cuenta_id", cuenta.toString(), "nombre", nombre, "giro", giro));
        transaccion.executeWithoutResult(estado -> bus.publishEvent(sobre));
    }

    @Test
    @DisplayName("un negocio.registro_solicitado por el bus crea el negocio y deja su registro en el outbox terminado")
    void pedidoPorElBus_creaElNegocio() {
        var negocio = UuidV7.generar(AHORA);
        var cuenta = UuidV7.generar(AHORA);

        publicarPedido(negocio, cuenta, "Fonda Doña Chela", "RESTAURANTE");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(negocios.negocio(negocio)).isPresent());
        assertThat(altas.misAltas(cuenta)).extracting("estado").containsExactly(EstadoAlta.REGISTRADO);
        // Su negocio.registrado tambien llega al bus y el outbox lo da por entregado.
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(jdbc.sql(
                "SELECT count(*) FROM plataforma.event_publication WHERE serialized_event LIKE :id "
                        + "AND completion_date IS NULL").param("id", "%" + negocio + "%").query(Integer.class).single())
                .isZero());
    }

    @Test
    @DisplayName("un pedido con giro invalido por el bus queda RECHAZADO y no crea el negocio")
    void pedidoInvalidoPorElBus_quedaRechazado() {
        var negocio = UuidV7.generar(AHORA);
        var cuenta = UuidV7.generar(AHORA);

        publicarPedido(negocio, cuenta, "Farmacia del Centro", "FARMACIA");

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> assertThat(altas.misAltas(cuenta)).hasSize(1));
        assertThat(altas.misAltas(cuenta).get(0).estado()).isEqualTo(EstadoAlta.RECHAZADO);
        assertThat(negocios.negocio(negocio)).isEmpty();
    }
}
