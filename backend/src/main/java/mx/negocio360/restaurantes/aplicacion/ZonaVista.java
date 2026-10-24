package mx.negocio360.restaurantes.aplicacion;

import java.util.UUID;
import mx.negocio360.restaurantes.dominio.Zona;

/** Una zona del salon tal como la ve la app. */
public record ZonaVista(UUID id, String nombre, int orden, boolean activa, long version) {

    /** La vista de una zona. */
    public static ZonaVista de(Zona zona) {
        return new ZonaVista(zona.id(), zona.nombre(), zona.orden(), zona.activa(), zona.version());
    }
}
