package mx.negocio360.identidad;

import java.util.List;

/** Un rol que ofrece el giro del negocio activo, con sus permisos en orden alfabetico. */
public record RolVista(String codigo, String nombre, List<String> permisos) {

    public RolVista {
        permisos = List.copyOf(permisos);
    }
}
