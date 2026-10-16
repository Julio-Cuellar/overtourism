package mx.negocio360.negocios;

import java.util.List;

/** El horario que ve el turista, con «abierto ahora» ya calculado. */
public record HorarioPublicoVista(List<DiaVista> semana, List<ExcepcionVista> excepciones, boolean abiertoAhora,
                                  String zonaHoraria) {

    public HorarioPublicoVista {
        semana = List.copyOf(semana);
        excepciones = List.copyOf(excepciones);
    }
}
