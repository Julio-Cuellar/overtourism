package mx.negocio360.negocios;

import java.util.List;

/** Los tramos de un dia (1 lunes a 7 domingo). */
public record DiaVista(int dia, List<TramoVista> tramos) {

    public DiaVista {
        tramos = List.copyOf(tramos);
    }
}
