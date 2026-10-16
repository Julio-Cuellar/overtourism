package mx.negocio360.negocios.dominio;

import java.time.LocalTime;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Un tramo de apertura de un dia. Si {@code cierra} es menor que {@code abre} cruza la medianoche (HO-01); abrir y
 * cerrar a la misma hora no es un tramo.
 */
public record Tramo(LocalTime abre, LocalTime cierra) {

    private static final Pattern HORA = Pattern.compile("^([01]\\d|2[0-3]):([0-5]\\d)$");
    static final int MINUTOS_DEL_DIA = 24 * 60;

    public Tramo {
        if (abre == null || cierra == null || abre.equals(cierra)) {
            throw new NegociosException(CodigoError.HORARIO_INVALIDO);
        }
    }

    /** Lee horas escritas como {@code HH:mm}; cualquier otra forma es {@code HORARIO_INVALIDO}. */
    public static Tramo de(String abre, String cierra) {
        return new Tramo(hora(abre), hora(cierra));
    }

    static LocalTime hora(String texto) {
        if (texto == null) {
            throw new NegociosException(CodigoError.HORARIO_INVALIDO);
        }
        var coincidencia = HORA.matcher(texto);
        if (!coincidencia.matches()) {
            throw new NegociosException(CodigoError.HORARIO_INVALIDO);
        }
        return LocalTime.of(Integer.parseInt(coincidencia.group(1)), Integer.parseInt(coincidencia.group(2)));
    }

    public boolean cruzaMedianoche() {
        return cierra.isBefore(abre);
    }

    int minutoDeApertura() {
        return abre.getHour() * 60 + abre.getMinute();
    }

    int duracionEnMinutos() {
        var cierre = cierra.getHour() * 60 + cierra.getMinute();
        return cruzaMedianoche() ? MINUTOS_DEL_DIA - minutoDeApertura() + cierre : cierre - minutoDeApertura();
    }

    @Override
    public String toString() {
        return Objects.toString(abre) + "-" + cierra;
    }
}
