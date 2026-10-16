package mx.negocio360.restaurantes.dominio;

import java.time.LocalDate;
import java.time.MonthDay;
import java.time.format.DateTimeParseException;
import java.util.Objects;

/**
 * Temporada de un platillo (MN-06, D-177): un rango de mes y dia, sin año, que puede cruzar el fin de año (del 1 de
 * diciembre al 6 de enero). Los dos extremos cuentan. Se evalua con la fecha del negocio, no la del servidor.
 *
 * @param desde primer dia de la temporada
 * @param hasta ultimo dia de la temporada
 */
public record Temporada(MonthDay desde, MonthDay hasta) {

    public Temporada {
        Objects.requireNonNull(desde, "desde");
        Objects.requireNonNull(hasta, "hasta");
    }

    /**
     * Lee una temporada de dos textos {@code MM-dd}.
     *
     * @throws RestaurantesException TEMPORADA_INVALIDA si alguno no es un mes y dia validos
     */
    public static Temporada de(String desde, String hasta) {
        try {
            return new Temporada(MonthDay.parse("--" + desde), MonthDay.parse("--" + hasta));
        } catch (DateTimeParseException | NullPointerException e) {
            throw new RestaurantesException(CodigoError.TEMPORADA_INVALIDA);
        }
    }

    /** Verdadero si ese dia cae en la temporada. */
    public boolean contiene(LocalDate fecha) {
        var dia = MonthDay.from(fecha);
        if (desde.compareTo(hasta) <= 0) {
            return dia.compareTo(desde) >= 0 && dia.compareTo(hasta) <= 0;
        }
        return dia.compareTo(desde) >= 0 || dia.compareTo(hasta) <= 0;
    }

    /** {@code MM-dd} del primer dia. */
    public String desdeComoTexto() {
        return texto(desde);
    }

    /** {@code MM-dd} del ultimo dia. */
    public String hastaComoTexto() {
        return texto(hasta);
    }

    private static String texto(MonthDay valor) {
        return String.format("%02d-%02d", valor.getMonthValue(), valor.getDayOfMonth());
    }
}
