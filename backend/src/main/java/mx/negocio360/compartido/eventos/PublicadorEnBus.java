package mx.negocio360.compartido.eventos;

import java.util.Objects;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Lleva un evento al bus de Spring. Con Spring Modulith, publicar dentro de una transaccion guarda el evento en el
 * outbox ({@code plataforma.event_publication}) en esa misma transaccion y lo entrega a los listeners despues del
 * commit, al menos una vez (R3).
 *
 * <p>Falla cerrado: los listeners de Modulith solo reciben lo que se publica dentro de una transaccion, y fuera de
 * una el evento no llega a nadie ni queda en el outbox, sin avisar. Publicar sin transaccion es un error de quien
 * llama, y se rechaza en lugar de perder el hecho.
 */
public final class PublicadorEnBus {

    private final ApplicationEventPublisher bus;

    public PublicadorEnBus(ApplicationEventPublisher bus) {
        this.bus = Objects.requireNonNull(bus, "bus");
    }

    public void publicar(SobreEvento evento) {
        Objects.requireNonNull(evento, "evento");
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("No se puede publicar " + evento.tipo() + " sin una transaccion activa: "
                    + "el evento no quedaria en el outbox ni llegaria a ningun listener");
        }
        bus.publishEvent(evento);
    }
}
