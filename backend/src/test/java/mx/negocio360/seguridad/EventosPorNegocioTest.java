package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import mx.negocio360.compartido.graphql.EventosPorNegocio;
import mx.negocio360.compartido.seguridad.NoAutenticadoException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

/**
 * SG-16: una suscripcion solo ve los eventos del negocio de quien la abrio. El negocio sale de la entrada de GraphQL al
 * suscribirse, nunca de un argumento de la suscripcion ni de lo que haya en el hilo; lo que sea de otro negocio se
 * descarta. Es la barrera que cada suscripcion de un modulo debe usar sobre su flujo de eventos.
 */
class EventosPorNegocioTest {

    private static final UUID A = UUID.fromString("0192f39f-1111-7000-8000-00000000a001");
    private static final UUID B = UUID.fromString("0192f39f-2222-7000-8000-00000000b002");

    @AfterEach
    void limpiar() {
        SesionDePrueba.salir();
    }

    private record Evento(UUID negocio, String texto) {
    }

    private static Flux<Evento> eventos() {
        return Flux.just(new Evento(A, "a1"), new Evento(B, "b1"), new Evento(A, "a2"), new Evento(B, "b2"));
    }

    private static EntradaGraphQl de(UUID negocio) {
        return SesionDePrueba.entrada(SesionDePrueba.CUENTA, negocio);
    }

    @Test
    @DisplayName("deja pasar solo los eventos del negocio de la sesion")
    void soloLosDelNegocioDeLaSesion() {
        var vistos = EventosPorNegocio.filtrar(eventos(), de(A), Evento::negocio).map(Evento::texto).collectList().block();

        assertThat(vistos).containsExactly("a1", "a2");
    }

    @Test
    @DisplayName("la sesion del otro negocio ve los suyos y ninguno ajeno")
    void elOtroNegocio_veLosSuyos() {
        var vistos = EventosPorNegocio.filtrar(eventos(), de(B), Evento::negocio).map(Evento::texto).collectList().block();

        assertThat(vistos).containsExactly("b1", "b2");
    }

    @Test
    @DisplayName("sin sesion, o sin negocio activo, la suscripcion falla al abrirse y no entrega nada")
    void sinSesionOSinNegocio_falla() {
        assertThatThrownBy(() -> EventosPorNegocio.filtrar(eventos(), EntradaGraphQl.anonima(null), Evento::negocio))
                .isInstanceOfSatisfying(NoAutenticadoException.class,
                        e -> assertThat(e.codigo()).isEqualTo(NoAutenticadoException.NO_AUTENTICADO));

        assertThatThrownBy(() -> EventosPorNegocio.filtrar(eventos(), de(null), Evento::negocio))
                .isInstanceOfSatisfying(NoAutenticadoException.class,
                        e -> assertThat(e.codigo()).isEqualTo(NoAutenticadoException.SIN_NEGOCIO_ACTIVO));
    }

    @Test
    @DisplayName("el negocio sale de la entrada recibida: lo que haya en el hilo no lo cambia")
    void elNegocioSaleDeLaEntrada() {
        SesionDePrueba.entrar(B);
        var sink = reactor.core.publisher.Sinks.many().multicast().<Evento>directBestEffort();
        var vistos = new java.util.concurrent.CopyOnWriteArrayList<String>();
        EventosPorNegocio.filtrar(sink.asFlux(), de(A), Evento::negocio).subscribe(e -> vistos.add(e.texto()));

        SesionDePrueba.entrar(A);
        sink.tryEmitNext(new Evento(A, "a1"));
        sink.tryEmitNext(new Evento(B, "b1"));

        assertThat(vistos).containsExactly("a1");
    }

    @Test
    @DisplayName("un evento sin negocio se descarta: ante la duda, no se entrega")
    void eventoSinNegocio_seDescarta() {
        var vistos = EventosPorNegocio
                .filtrar(Flux.just(new Evento(null, "x"), new Evento(A, "a1")), de(A), Evento::negocio)
                .map(Evento::texto).collectList().block();

        assertThat(vistos).containsExactly("a1");
    }

    @Test
    @DisplayName("hay una forma directa para los sobres de evento de la plataforma")
    void sobresDeEvento() {
        var propio = sobre(A);
        var ajeno = sobre(B);

        var vistos = EventosPorNegocio.deSobres(Flux.just(propio, ajeno), de(A)).collectList().block();

        assertThat(vistos).containsExactly(propio);
    }

    private static SobreEvento sobre(UUID negocio) {
        var ahora = Instant.now();
        return new SobreEvento(UuidV7.generar(ahora), "inventario.bajo_minimo", 1, negocio, "inventario", ahora,
                UUID.randomUUID(), null, Map.of("articulo_id", UUID.randomUUID().toString()));
    }
}
