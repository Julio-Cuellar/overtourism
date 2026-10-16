package mx.negocio360.inventario.aplicacion;

import java.util.List;
import org.springframework.modulith.NamedInterface;

/**
 * Una pagina de resultados paginada por cursor.
 *
 * @param hayMas verdadero si existen resultados despues de esta pagina
 * @param cursor id del ultimo elemento, para pedir la siguiente pagina; nulo si la pagina esta vacia
 */
@NamedInterface("api")
public record Pagina<T>(List<T> nodos, boolean hayMas, String cursor) {

    public Pagina {
        nodos = List.copyOf(nodos);
    }
}
