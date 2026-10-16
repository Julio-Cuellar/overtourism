package mx.negocio360.identidad.infraestructura;

import java.util.ArrayDeque;
import java.util.List;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.identidad.aplicacion.puertos.PublicadorEventos;

/**
 * Guarda los eventos en memoria. Solo para desarrollo, demo y pruebas: no llegan a ningun otro modulo ni al outbox.
 * Conserva los ultimos {@value #MAXIMO} para que un uso repetido no llene la memoria.
 */
public final class PublicadorEventosEnMemoria implements PublicadorEventos {

    static final int MAXIMO = 1_000;

    private final ArrayDeque<SobreEvento> eventos = new ArrayDeque<>();

    @Override
    public synchronized void publicar(SobreEvento evento) {
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
