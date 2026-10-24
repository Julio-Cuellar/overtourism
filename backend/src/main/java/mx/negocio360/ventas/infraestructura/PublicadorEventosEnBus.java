package mx.negocio360.ventas.infraestructura;

import java.util.Objects;
import mx.negocio360.compartido.eventos.PublicadorEnBus;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.ventas.aplicacion.puertos.PublicadorEventos;
import org.springframework.context.ApplicationEventPublisher;

/**
 * Publicador real de Ventas: lleva cada evento al bus de Spring y, con Spring Modulith, al outbox, dentro de la
 * transaccion de la venta (B-05): el hecho y el evento se confirman juntos o ninguno. Fuera de una transaccion falla en
 * lugar de perder el evento.
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
