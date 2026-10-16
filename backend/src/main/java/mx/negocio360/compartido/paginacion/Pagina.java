package mx.negocio360.compartido.paginacion;

import java.util.List;

/**
 * Una pagina de una lista larga (SG-07, D-131): los elementos, si hay mas y el cursor opaco para pedir la siguiente.
 *
 * @param elementos   los de esta pagina, en el orden de la consulta
 * @param hayMas      verdadero si quedan elementos despues del ultimo
 * @param cursorFinal cursor para pasarlo como {@code despues}; nulo si no hay mas
 * @param <T>         tipo de elemento
 */
public record Pagina<T>(List<T> elementos, boolean hayMas, String cursorFinal) {

    /** Copia la lista para que la pagina sea inmutable. */
    public Pagina {
        elementos = List.copyOf(elementos);
    }
}
