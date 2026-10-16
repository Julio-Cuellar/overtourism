package mx.negocio360.negocios;

import java.util.List;

/** Los atributos opcionales que declara el negocio (AT-01 a AT-06): declarados, no verificados. */
public record AtributosVista(List<EtiquetaVista> dieta, List<EtiquetaVista> accesibilidad, Integer duracionVisitaMin,
                             Integer costoMedioMxn) {

    public AtributosVista {
        dieta = List.copyOf(dieta);
        accesibilidad = List.copyOf(accesibilidad);
    }
}
