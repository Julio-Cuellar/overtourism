package mx.negocio360.ventas.fakes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.ventas.aplicacion.puertos.PublicadorEventos;

/**
 * Guarda los eventos publicados, en orden. Puede reenviarlos a un oyente, para las pruebas que juntan Ventas con
 * Caja como lo haria el bus.
 */
public final class PublicadorEventosEnMemoria implements PublicadorEventos {

    private final List<SobreEvento> publicados = new ArrayList<>();
    private final List<Consumer<SobreEvento>> oyentes = new ArrayList<>();

    @Override
    public void publicar(SobreEvento evento) {
        publicados.add(evento);
        oyentes.forEach(o -> o.accept(evento));
    }

    public void alPublicar(Consumer<SobreEvento> oyente) {
        oyentes.add(oyente);
    }

    public List<SobreEvento> todos() {
        return List.copyOf(publicados);
    }

    public List<SobreEvento> deTipo(String tipo) {
        return publicados.stream().filter(e -> e.tipo().equals(tipo)).toList();
    }

    public SobreEvento unicoDeTipo(String tipo) {
        var eventos = deTipo(tipo);
        if (eventos.size() != 1) {
            throw new IllegalStateException("Se esperaba un solo " + tipo + " y hay " + eventos.size());
        }
        return eventos.get(0);
    }
}
