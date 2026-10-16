package mx.negocio360.seguridad;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/** Solo pruebas: el canal por el que "llegan" eventos de todos los negocios a la suscripcion de prueba. */
public final class EventosDePrueba {

    private static final Sinks.Many<EventoDePrueba> CANAL = Sinks.many().multicast().directBestEffort();

    private EventosDePrueba() {
    }

    static Flux<EventoDePrueba> flujo() {
        return CANAL.asFlux();
    }

    public static void publicar(EventoDePrueba evento) {
        CANAL.tryEmitNext(evento);
    }

    /** Cuantas suscripciones estan escuchando; las pruebas esperan a que haya antes de publicar. */
    public static int suscriptores() {
        return CANAL.currentSubscriberCount();
    }
}
