package mx.negocio360.compartido.graphql;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import mx.negocio360.compartido.eventos.SobreEvento;
import reactor.core.publisher.Flux;

/**
 * SG-16: aislamiento de eventos por negocio. Toda suscripcion de un modulo pasa su flujo de eventos por aqui: la persona
 * que se suscribe solo recibe los eventos de su negocio. El negocio sale de la {@link EntradaGraphQl} de la suscripcion
 * al abrirla, nunca de un argumento de la suscripcion, y un evento sin negocio se descarta. Sin sesion o sin negocio
 * activo la suscripcion falla al abrirse ({@code NO_AUTENTICADO} o {@code SIN_NEGOCIO_ACTIVO}) y no entrega nada.
 * Las suscripciones por cuenta (por ejemplo el aviso de un alta, cuando todavia no hay negocio) usan
 * un filtro por cuenta (EventosPorCuenta).
 */
public final class EventosPorNegocio {

    private EventosPorNegocio() {
    }

    /** @throws mx.negocio360.compartido.seguridad.NoAutenticadoException si no hay sesion o negocio activo */
    public static <T> Flux<T> filtrar(Flux<T> eventos, EntradaGraphQl entrada, Function<T, UUID> negocioDe) {
        var negocio = Objects.requireNonNull(entrada, "entrada").exigirContexto().negocioId();
        return eventos.filter(evento -> negocio.equals(negocioDe.apply(evento)));
    }

    /** Lo mismo para los sobres de evento de la plataforma. */
    public static Flux<SobreEvento> deSobres(Flux<SobreEvento> eventos, EntradaGraphQl entrada) {
        return filtrar(eventos, entrada, SobreEvento::negocioId);
    }
}
