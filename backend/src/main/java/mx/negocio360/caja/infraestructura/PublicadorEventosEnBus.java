package mx.negocio360.caja.infraestructura;

import java.util.Objects;
import mx.negocio360.caja.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.compartido.eventos.PublicadorEnBus;
import mx.negocio360.compartido.eventos.SobreEvento;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publicador real de Caja: lleva cada evento al bus de Spring y, con Spring Modulith, al outbox, dentro de la transaccion
 * del caso de uso (B-05). Fuera de una transaccion falla en lugar de perder el evento.
 */
public final class PublicadorEventosEnBus implements PublicadorEventos {

    private final PublicadorEnBus bus;

    /**
     * Arma el publicador.
     *
     * @param bus bus de eventos de Spring
     */
    public PublicadorEventosEnBus(ApplicationEventPublisher bus) {
        this.bus = new PublicadorEnBus(Objects.requireNonNull(bus, "bus"));
    }

    @Override
    public void publicar(SobreEvento evento) {
        bus.publicar(evento);
    }
}
