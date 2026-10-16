package mx.negocio360.identidad.dominio;

import java.util.Objects;
import java.util.Set;

/**
 * Un rol base (AM-09): un codigo, el nombre para la pantalla y los permisos que concede. Es un dato, no logica: los
 * verticales no se conocen aqui (R4). Inmutable.
 */
public record Rol(String codigo, String nombre, Set<String> permisos) {

    public Rol {
        Objects.requireNonNull(codigo, "codigo");
        Objects.requireNonNull(nombre, "nombre");
        permisos = Set.copyOf(permisos);
    }
}
