package mx.negocio360.caja.infraestructura;

import java.util.Objects;
import mx.negocio360.caja.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.compartido.eventos.SobreEvento;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publica los eventos de Caja ({@code caja.*}) en el bus de Spring, sincrono y en el mismo proceso (F4, demo).
 * Pendiente F5: el outbox de Spring Modulith dentro de la transaccion.
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
