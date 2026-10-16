package mx.negocio360.compartido.seguridad;

import java.util.Set;

/**
 * Lo que la persona de la peticion puede hacer (AZ-01, AZ-04). Todo permiso esta negado por defecto. Cada modulo
 * pregunta por un permiso concreto y no conoce los roles.
 */
public interface Autorizacion {

    boolean puede(String permiso);

    /** @throws PermisoDenegadoException si la persona no tiene el permiso */
    default void exigir(String permiso) {
        if (!puede(permiso)) {
            throw new PermisoDenegadoException(permiso);
        }
    }

    /** Autorizacion con exactamente esos permisos; copia el conjunto. */
    static Autorizacion de(Set<String> permisos) {
        var concedidos = Set.copyOf(permisos);
        return permiso -> permiso != null && concedidos.contains(permiso);
    }
}
