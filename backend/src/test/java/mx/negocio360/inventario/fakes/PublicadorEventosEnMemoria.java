package mx.negocio360.inventario.fakes;

import java.util.ArrayList;
import java.util.List;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.inventario.aplicacion.puertos.PublicadorEventos;

/** Guarda los eventos publicados, en orden, para que la prueba los inspeccione. */
public final class PublicadorEventosEnMemoria implements PublicadorEventos {

    private final List<SobreEvento> publicados = new ArrayList<>();

    @Override
    public void publicar(SobreEvento evento) {
        publicados.add(evento);
    }

    public List<SobreEvento> todos() {
        return List.copyOf(publicados);
    }

    public List<SobreEvento> deTipo(String tipo) {
        return publicados.stream().filter(e -> e.tipo().equals(tipo)).toList();
    }

    public List<String> tipos() {
        return publicados.stream().map(SobreEvento::tipo).toList();
    }

    public void limpiar() {
        publicados.clear();
    }
}
