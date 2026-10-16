package mx.negocio360.negocios.dominio;

import java.util.ArrayList;
import java.util.List;

/**
 * Completitud del perfil (NG-11), calculada solo con codigo. El nombre y el giro ya estan al nacer el negocio y valen
 * 40 %; la ubicacion y el horario valen 20 % cada uno, y el contacto y el logo, 10 %. Los atributos opcionales no
 * cuentan (AT-01).
 */
public record Completitud(int porcentaje, List<Faltante> faltantes) {

    /** Lo que le falta al perfil, en un orden fijo. */
    public enum Faltante { UBICACION, HORARIO, CONTACTO, LOGO }

    private static final int BASE = 40;

    public Completitud {
        faltantes = List.copyOf(faltantes);
    }

    public static Completitud de(boolean ubicacion, boolean horario, boolean contacto, boolean logo) {
        var faltantes = new ArrayList<Faltante>();
        var porcentaje = BASE;
        if (ubicacion) {
            porcentaje += 20;
        } else {
            faltantes.add(Faltante.UBICACION);
        }
        if (horario) {
            porcentaje += 20;
        } else {
            faltantes.add(Faltante.HORARIO);
        }
        if (contacto) {
            porcentaje += 10;
        } else {
            faltantes.add(Faltante.CONTACTO);
        }
        if (logo) {
            porcentaje += 10;
        } else {
            faltantes.add(Faltante.LOGO);
        }
        return new Completitud(porcentaje, faltantes);
    }
}
