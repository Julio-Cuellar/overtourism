package mx.negocio360.negocios.dominio;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Los tramos de un dia (1 lunes a 7 domingo). Hasta tres (HO-02); se ordenan por hora de apertura y no se traslapan
 * entre si (HO-01). Sin tramos el dia esta cerrado.
 */
public record DiaHorario(int dia, List<Tramo> tramos) {

    public static final int MAXIMO_DE_TRAMOS = 3;

    public DiaHorario {
        if (dia < 1 || dia > 7) {
            throw new NegociosException(CodigoError.HORARIO_INVALIDO);
        }
        Objects.requireNonNull(tramos, "tramos");
        if (tramos.size() > MAXIMO_DE_TRAMOS) {
            throw new NegociosException(CodigoError.TRAMOS_EXCEDIDOS);
        }
        tramos = tramos.stream().sorted(Comparator.comparing(Tramo::abre)).toList();
        for (var i = 1; i < tramos.size(); i++) {
            var anterior = tramos.get(i - 1);
            if (anterior.minutoDeApertura() + anterior.duracionEnMinutos() > tramos.get(i).minutoDeApertura()) {
                throw new NegociosException(CodigoError.HORARIO_INVALIDO);
            }
        }
    }
}
