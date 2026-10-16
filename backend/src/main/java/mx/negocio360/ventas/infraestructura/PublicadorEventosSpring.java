package mx.negocio360.ventas.infraestructura;

import java.util.Objects;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.ventas.aplicacion.puertos.PublicadorEventos;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publica los eventos de Ventas en el bus de Spring, de forma sincrona y en el mismo proceso (F4, demo). Caja los
 * recibe al momento. Pendiente F5: publicarlos dentro de la transaccion con el outbox de Spring Modulith
 * ({@code event_publication}), para que un fallo no pierda el evento.
 */
public final class PublicadorEventosSpring implements PublicadorEventos {

    private final ApplicationEventPublisher bus;

    /**
     * Arma el publicador.
     *
     * @param bus bus de eventos de Spring
     */
    public PublicadorEventosSpring(ApplicationEventPublisher bus) {
        this.bus = Objects.requireNonNull(bus, "bus");
    }

    @Override
    public void publicar(SobreEvento evento) {
        bus.publishEvent(evento);
    }
}
