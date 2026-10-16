package mx.negocio360.compartido;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.PublicadorEnBus;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * El adaptador que lleva los eventos al bus de Spring y, con Spring Modulith, al outbox. Falla cerrado: sin una
 * transaccion activa el evento se perderia en silencio (nadie lo recibe y no queda en el outbox), asi que se rechaza.
 */
class PublicadorEnBusTest {

    private final List<Object> publicados = new ArrayList<>();
    private final ApplicationEventPublisher bus = publicados::add;
    private final GestorTransaccionalFalso gestor = new GestorTransaccionalFalso();
    private final PublicadorEnBus publicador = new PublicadorEnBus(bus);

    private static SobreEvento evento() {
        var ahora = Instant.parse("2026-10-15T18:00:00Z");
        return new SobreEvento(UuidV7.generar(ahora), "cliente.registrado", 1, UUID.randomUUID(), "clientes", ahora,
                UUID.randomUUID(), null, Map.of("cliente_id", UUID.randomUUID().toString()));
    }

    @Test
    @DisplayName("publicar con una transaccion activa entrega el mismo sobre al bus")
    void publicar_conTransaccionActiva_entregaElSobreAlBus() {
        var evento = evento();

        gestor.plantilla().executeWithoutResult(estado -> publicador.publicar(evento));

        assertThat(publicados).containsExactly(evento);
    }

    @Test
    @DisplayName("publicar sin transaccion activa falla y no llega al bus")
    void publicar_sinTransaccion_fallaCerradoYNoPublica() {
        assertThatThrownBy(() -> publicador.publicar(evento()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("transaccion");

        assertThat(publicados).isEmpty();
    }

    @Test
    @DisplayName("publicar un evento nulo falla antes de tocar el bus")
    void publicar_eventoNulo_falla() {
        gestor.plantilla().executeWithoutResult(estado ->
                assertThatNullPointerException().isThrownBy(() -> publicador.publicar(null)));

        assertThat(publicados).isEmpty();
    }

    @Test
    @DisplayName("el publicador exige un bus")
    void constructor_sinBus_falla() {
        assertThatNullPointerException().isThrownBy(() -> new PublicadorEnBus(null));
    }
}
