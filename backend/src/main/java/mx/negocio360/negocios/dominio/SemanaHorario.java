package mx.negocio360.negocios.dominio;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

/**
 * El horario semanal de una sucursal. Un dia sin entrada esta cerrado. Valida que no haya dos dias repetidos y que ningun
 * tramo se traslape con otro, contando el que viene del dia anterior y dando la vuelta a la semana (HO-01).
 */
public record SemanaHorario(List<DiaHorario> dias) {

    private static final int MINUTOS_DE_LA_SEMANA = 7 * Tramo.MINUTOS_DEL_DIA;

    public SemanaHorario {
        Objects.requireNonNull(dias, "dias");
        var vistos = new HashSet<Integer>();
        for (var d : dias) {
            if (!vistos.add(d.dia())) {
                throw new NegociosException(CodigoError.HORARIO_INVALIDO);
            }
        }
        dias = dias.stream().sorted(Comparator.comparingInt(DiaHorario::dia)).toList();
        verificarTraslapes(dias);
    }

    public static SemanaHorario vacia() {
        return new SemanaHorario(List.of());
    }

    /** Los tramos de un dia; vacios si el dia esta cerrado. */
    public DiaHorario dia(int dia) {
        return dias.stream().filter(d -> d.dia() == dia).findFirst().orElse(new DiaHorario(dia, List.of()));
    }

    public boolean tieneTramos() {
        return dias.stream().anyMatch(d -> !d.tramos().isEmpty());
    }

    private static void verificarTraslapes(List<DiaHorario> dias) {
        var intervalos = new ArrayList<int[]>();
        for (var d : dias) {
            for (var t : d.tramos()) {
                var inicio = (d.dia() - 1) * Tramo.MINUTOS_DEL_DIA + t.minutoDeApertura();
                intervalos.add(new int[] {inicio, inicio + t.duracionEnMinutos()});
            }
        }
        intervalos.sort(Comparator.comparingInt(i -> i[0]));
        for (var i = 1; i < intervalos.size(); i++) {
            if (intervalos.get(i - 1)[1] > intervalos.get(i)[0]) {
                throw new NegociosException(CodigoError.HORARIO_INVALIDO);
            }
        }
        if (!intervalos.isEmpty() && intervalos.get(intervalos.size() - 1)[1]
                > MINUTOS_DE_LA_SEMANA + intervalos.get(0)[0]) {
            throw new NegociosException(CodigoError.HORARIO_INVALIDO);
        }
    }
}
