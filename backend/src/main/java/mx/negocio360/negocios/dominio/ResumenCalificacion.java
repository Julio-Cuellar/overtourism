package mx.negocio360.negocios.dominio;

import java.util.List;

/**
 * El resumen de calificaciones de un negocio (RS-06): cuantas reseñas hay de cada calificacion. El total, la suma y el
 * promedio los calcula el codigo, nunca una persona.
 */
public record ResumenCalificacion(int c1, int c2, int c3, int c4, int c5) {

    public static ResumenCalificacion de(int c1, int c2, int c3, int c4, int c5) {
        return new ResumenCalificacion(c1, c2, c3, c4, c5);
    }

    public static ResumenCalificacion vacio() {
        return new ResumenCalificacion(0, 0, 0, 0, 0);
    }

    public int total() {
        return c1 + c2 + c3 + c4 + c5;
    }

    public int suma() {
        return c1 + 2 * c2 + 3 * c3 + 4 * c4 + 5 * c5;
    }

    /** Promedio con un decimal, o nulo si todavia no hay reseñas. */
    public Double promedio() {
        return total() == 0 ? null : Math.round(suma() * 10.0 / total()) / 10.0;
    }

    public List<Integer> distribucion() {
        return List.of(c1, c2, c3, c4, c5);
    }
}
