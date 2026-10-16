package mx.negocio360.identidad;

import java.util.List;
import java.util.UUID;

/**
 * Perfil de la sesion: la cuenta, el negocio activo (nulo si no hay; es el unico negocio de la persona o el que eligio),
 * el rol y los permisos de la persona en ese negocio (nulo y vacio sin negocio activo), los negocios donde tiene
 * membresia activa y los que pidio y siguen pendientes (D-79).
 */
public record PerfilVista(CuentaVista cuenta, UUID negocioActivo, String rol, List<String> permisos,
                          List<NegocioVista> negocios, List<NegocioPendienteVista> negociosPendientes) {

    public PerfilVista {
        permisos = List.copyOf(permisos);
        negocios = List.copyOf(negocios);
        negociosPendientes = List.copyOf(negociosPendientes);
    }

    /** Sin negocios pendientes. */
    public PerfilVista(CuentaVista cuenta, UUID negocioActivo, String rol, List<String> permisos,
                       List<NegocioVista> negocios) {
        this(cuenta, negocioActivo, rol, permisos, negocios, List.of());
    }

    @Override
    public String toString() {
        return "PerfilVista[negocioActivo=" + negocioActivo + ", negocios=" + negocios.size() + "]";
    }
}
