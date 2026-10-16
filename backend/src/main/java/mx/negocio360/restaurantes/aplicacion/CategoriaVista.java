package mx.negocio360.restaurantes.aplicacion;

import java.util.UUID;
import mx.negocio360.restaurantes.dominio.Categoria;
import mx.negocio360.restaurantes.dominio.Estacion;

/** Una categoria tal como la ve el panel. */
public record CategoriaVista(UUID id, String nombre, int orden, Estacion estacion, boolean activa, long version) {

    /** Vista de una categoria. */
    public static CategoriaVista de(Categoria c) {
        return new CategoriaVista(c.id(), c.nombre(), c.orden(), c.estacion(), c.activa(), c.version());
    }
}
