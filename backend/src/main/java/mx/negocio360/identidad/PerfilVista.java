package mx.negocio360.identidad;

import java.util.List;
import java.util.UUID;

/**
 * Perfil de la sesion: la cuenta, el negocio activo (nulo hasta elegir) y los negocios donde tiene membresia (vacio
 * hasta IDN-2). El rol y los permisos se agregan con las membresias.
 */
public record PerfilVista(CuentaVista cuenta, UUID negocioActivo, List<NegocioVista> negocios) {

    public PerfilVista {
        negocios = List.copyOf(negocios);
    }

    @Override
    public String toString() {
        return "PerfilVista[negocioActivo=" + negocioActivo + ", negocios=" + negocios.size() + "]";
    }
}
