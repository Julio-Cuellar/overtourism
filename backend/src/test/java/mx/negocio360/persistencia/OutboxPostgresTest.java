package mx.negocio360.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Outbox transaccional (B-05): el evento se guarda en PostgreSQL en la misma transacción que el cambio. Si la
 * transacción se revierte no existe; si el listener falla queda pendiente para reintentarse (entrega al menos una
 * vez). La tabla es event_publication, del registro de publicación de eventos de Spring Modulith.
 */
@SpringBootTest
@Import(OutboxPostgresTest.Configuracion.class)
class OutboxPostgresTest {

    private static final Duration ESPERA = Duration.ofSeconds(10);

    @Autowired
    private ApplicationEventPublisher publicador;
    @Autowired
    private TransactionTemplate transaccion;
    @Autowired
    private Receptor receptor;

    @AfterEach
    void restablecer() {
        receptor.fallar(false);
    }

    private static SobreEvento evento() {
        var ahora = Instant.now();
        return new SobreEvento(UuidV7.generar(ahora), "prueba.ocurrio", 1, UUID.randomUUID(), "pruebas", ahora,
                UUID.randomUUID(), null, Map.of("cliente_id", UUID.randomUUID().toString()));
    }

    private static Boolean completado(UUID id) throws Exception {
        try (var sesion = SesionSql.comoDueno()) {
            // Cada modulo que escucha el bus deja su propia fila por evento: «completado» es que todas lo estan.
            var filas = sesion.columna("select bool_and(completion_date is not null) from plataforma.event_publication "
                    + "where serialized_event like '%' || ? || '%'", Boolean.class, id.toString());
            return filas.isEmpty() ? null : filas.get(0);
        }
    }

    @Test
    @DisplayName("un evento publicado dentro de una transacción llega al listener y queda marcado como completado")
    void evento_publicadoEnTransaccion_seEntregaYSeCompleta() {
        var evento = evento();

        transaccion.executeWithoutResult(estado -> publicador.publishEvent(evento));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(receptor.recibidos()).contains(evento.id()));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(completado(evento.id())).isTrue());
    }

    @Test
    @DisplayName("si la transacción se revierte, el evento no se publica ni queda registrado")
    void evento_siLaTransaccionSeRevierte_noExiste() throws Exception {
        var evento = evento();

        transaccion.executeWithoutResult(estado -> {
            publicador.publishEvent(evento);
            estado.setRollbackOnly();
        });

        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(receptor.recibidos()).doesNotContain(evento.id()));
        assertThat(completado(evento.id())).isNull();
    }

    @Test
    @DisplayName("si el listener falla, el evento queda pendiente en la base para reintentarse")
    void evento_siElListenerFalla_quedaPendiente() {
        receptor.fallar(true);
        var evento = evento();

        transaccion.executeWithoutResult(estado -> publicador.publishEvent(evento));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(receptor.intentos()).contains(evento.id()));
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(3))
                .untilAsserted(() -> assertThat(completado(evento.id())).isFalse());
    }

    @Test
    @DisplayName("el evento se guarda con sus campos en snake_case y sin datos personales")
    void evento_seSerializaConElSobreComun() throws Exception {
        var evento = evento();

        transaccion.executeWithoutResult(estado -> publicador.publishEvent(evento));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(completado(evento.id())).isNotNull());
        try (var sesion = SesionSql.comoDueno()) {
            var json = sesion.consultar("select serialized_event from plataforma.event_publication "
                    + "where serialized_event like '%' || ? || '%'", String.class, evento.id().toString());

            assertThat(json).contains("\"negocio_id\"", "\"correlacion_id\"", "\"ocurrido_en\"", "\"tipo\":\"prueba.ocurrio\"");
        }
    }

    /**
     * Listener de prueba: registra lo que recibe y puede fallar a propósito. Spring lo envuelve en un proxy por ser
     * asíncrono y transaccional, así que el estado se toca solo por métodos, nunca por campos.
     */
    static class Receptor {
        private final List<UUID> recibidos = new CopyOnWriteArrayList<>();
        private final List<UUID> intentos = new CopyOnWriteArrayList<>();
        private final AtomicBoolean fallar = new AtomicBoolean();

        @ApplicationModuleListener
        void alRecibir(SobreEvento evento) {
            intentos.add(evento.id());
            if (fallar.get()) {
                throw new IllegalStateException("fallo a propósito");
            }
            recibidos.add(evento.id());
        }

        public List<UUID> recibidos() {
            return recibidos;
        }

        public List<UUID> intentos() {
            return intentos;
        }

        public void fallar(boolean valor) {
            fallar.set(valor);
        }
    }

    @TestConfiguration
    @EnableAsync
    static class Configuracion {
        @Bean
        Receptor receptor() {
            return new Receptor();
        }
    }
}
