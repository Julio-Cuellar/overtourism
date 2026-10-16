package mx.negocio360.negocios.dominio;

import java.util.List;
import java.util.Objects;

/**
 * Lo que un negocio declara de si mismo (AT-01 a AT-06): dieta y accesibilidad (codigos del catalogo), cuanto dura una
 * visita y cuanto se gasta en promedio. Todo es opcional y son datos declarados, no verificados. Esta clase valida lo que
 * no depende del catalogo; que los codigos existan lo valida quien tiene el catalogo.
 */
public record AtributosDeclarados(List<String> dieta, List<String> accesibilidad, Integer duracionVisitaMin,
                                  Integer costoMedioMxn) {

    public static final int MAXIMO_POR_TIPO = 10;
    public static final int DURACION_MINIMA = 5;
    public static final int DURACION_MAXIMA = 480;
    public static final int COSTO_MAXIMO = 100_000;

    public AtributosDeclarados {
        Objects.requireNonNull(dieta, "dieta");
        Objects.requireNonNull(accesibilidad, "accesibilidad");
        if (dieta.size() > MAXIMO_POR_TIPO || accesibilidad.size() > MAXIMO_POR_TIPO) {
            throw new NegociosException(CodigoError.ATRIBUTOS_EXCEDIDOS);
        }
        if (dieta.stream().distinct().count() != dieta.size() || accesibilidad.stream().distinct().count()
                != accesibilidad.size() || dieta.stream().anyMatch(Objects::isNull)
                || accesibilidad.stream().anyMatch(Objects::isNull)) {
            throw new NegociosException(CodigoError.ATRIBUTO_INVALIDO);
        }
        if (duracionVisitaMin != null && (duracionVisitaMin < DURACION_MINIMA || duracionVisitaMin > DURACION_MAXIMA)) {
            throw new NegociosException(CodigoError.ATRIBUTO_INVALIDO);
        }
        if (costoMedioMxn != null && (costoMedioMxn < 0 || costoMedioMxn > COSTO_MAXIMO)) {
            throw new NegociosException(CodigoError.ATRIBUTO_INVALIDO);
        }
        dieta = List.copyOf(dieta);
        accesibilidad = List.copyOf(accesibilidad);
    }
}
