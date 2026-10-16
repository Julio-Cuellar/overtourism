package mx.negocio360.identidad.infraestructura;

import java.util.ArrayDeque;
import java.util.List;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.identidad.aplicacion.puertos.PublicadorEventos;

/**
 * Guarda los eventos en memoria para el simulador y las pruebas. Conserva los ultimos {@value #MAXIMO} para que un uso
 * repetido no llene la memoria. Sin destino, los eventos no llegan a ningun otro modulo ni al outbox; con destino, cada
 * evento se reenvia primero (al bus real, por ejemplo) y solo se guarda si el destino lo acepta.
 */
public final class PublicadorEventosEnMemoria implements PublicadorEventos {

    static final int MAXIMO = 1_000;

    private final ArrayDeque<SobreEvento> eventos = new ArrayDeque<>();
    private final PublicadorEventos destino;

    public PublicadorEventosEnMemoria() {
        this(null);
    }

    public PublicadorEventosEnMemoria(PublicadorEventos destino) {
        this.destino = destino;
    }

    @Override
    public synchronized void publicar(SobreEvento evento) {
        if (destino != null) {
            destino.publicar(evento);
        }
        if (eventos.size() == MAXIMO) {
            eventos.removeFirst();
        }
        eventos.addLast(evento);
    }

    public synchronized List<SobreEvento> eventos() {
        return List.copyOf(eventos);
    }

    public synchronized void limpiar() {
        eventos.clear();
    }
}
