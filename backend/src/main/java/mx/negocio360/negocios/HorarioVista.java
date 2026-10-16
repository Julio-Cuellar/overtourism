package mx.negocio360.negocios;

import java.util.List;
import java.util.UUID;

/** El horario de una sucursal con su {@code abiertoAhora} calculado en el servidor (HO-04). */
public record HorarioVista(UUID sucursalId, List<DiaVista> semana, List<ExcepcionVista> excepciones,
                           boolean abiertoAhora, long version) {

    public HorarioVista {
        semana = List.copyOf(semana);
        excepciones = List.copyOf(excepciones);
    }
}
