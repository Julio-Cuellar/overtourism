package mx.negocio360.negocios;

import java.util.List;

/** La calificacion del negocio. Hasta que lleguen las reseñas el total es 0 y el promedio es nulo. */
public record CalificacionVista(Double promedio, int total, List<Integer> distribucion) {

    public CalificacionVista {
        distribucion = List.copyOf(distribucion);
    }

    public static CalificacionVista sinResenas() {
        return new CalificacionVista(null, 0, List.of(0, 0, 0, 0, 0));
    }
}
