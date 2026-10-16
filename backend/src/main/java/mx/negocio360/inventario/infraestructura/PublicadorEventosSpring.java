package mx.negocio360.inventario.infraestructura;

import java.util.Objects;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.inventario.aplicacion.puertos.PublicadorEventos;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publica los eventos de Inventario en el bus de Spring, de forma sincrona y en el mismo proceso (desarrollo y demo).
 * Pendiente: publicarlos dentro de la transaccion con el outbox de Spring Modulith cuando Inventario tenga base de datos,
 * para que un fallo no pierda el evento.
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
