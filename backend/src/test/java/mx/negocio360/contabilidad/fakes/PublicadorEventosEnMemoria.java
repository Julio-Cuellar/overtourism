package mx.negocio360.contabilidad.fakes;

import java.util.ArrayList;
import java.util.List;
import mx.negocio360.contabilidad.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.compartido.eventos.SobreEvento;

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
}
