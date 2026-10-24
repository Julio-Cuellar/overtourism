package mx.negocio360.restaurantes.aplicacion;

import mx.negocio360.restaurantes.dominio.Disponibilidad;

/** Las mesas libres de un restaurante (MS-04), tal como las ve la app. */
public record DisponibilidadVista(int libres, int total, boolean abierto) {

    /** La vista de una disponibilidad. */
    public static DisponibilidadVista de(Disponibilidad d) {
        return new DisponibilidadVista(d.libres(), d.total(), d.abierto());
    }
}
