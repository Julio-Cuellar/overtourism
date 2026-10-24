package mx.negocio360.restaurantes.infraestructura;

import java.util.Objects;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.restaurantes.aplicacion.AvisoDeComanda;
import mx.negocio360.restaurantes.aplicacion.AvisoDelSalon;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * El puente entre el bus de eventos y las suscripciones de Restaurantes (8.4 del diseno). Recibe cada evento que publica la
 * plataforma y deja pasar solo los de comanda y de salon; la suscripcion los filtra por negocio (SG-16) y por lo que su
 * rol puede ver. No guarda nada: una suscripcion que llega tarde no ve lo anterior y se corrige con la consulta de
 * respaldo ({@code comandas}, {@code salon}). Un suscriptor lento o roto no frena a los demas ni al bus.
 */
public class AvisosEnVivo {

    private final Sinks.Many<SobreEvento> sumidero = Sinks.many().multicast().directBestEffort();
    private final Object cerrojo = new Object();

    /** Recibe cada evento publicado; los que no son de comanda ni de salon se ignoran. */
    @EscuchaDeEventos
    public void alRecibir(SobreEvento evento) {
        Objects.requireNonNull(evento, "evento");
        if (AvisoDeComanda.de(evento).isEmpty() && AvisoDelSalon.de(evento).isEmpty()) {
            return;
        }
        synchronized (cerrojo) {
            // Sin suscriptores o con alguno saturado el aviso se pierde a proposito: es un atajo, no la verdad.
            sumidero.tryEmitNext(evento);
        }
    }

    /** Cuantas suscripciones estan abiertas ahora, de todos los negocios. */
    public int suscritos() {
        return sumidero.currentSubscriberCount();
    }

    /** Todos los avisos que llegan desde ahora, de todos los negocios: quien los usa los filtra por el suyo. */
    public Flux<SobreEvento> eventos() {
        return sumidero.asFlux();
    }
}
