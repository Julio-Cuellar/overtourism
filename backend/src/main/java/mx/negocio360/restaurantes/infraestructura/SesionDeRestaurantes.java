package mx.negocio360.restaurantes.infraestructura;

import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.SesionActual;
import mx.negocio360.restaurantes.aplicacion.AutorizacionDeRestaurantes;
import org.springframework.security.core.AuthenticationException;

/**
 * Los permisos de la sesion de la peticion para los manejadores de comandos (AZ-04). Falla cerrado: sin sesion, sin negocio
 * activo o con el contexto de otro negocio, todo permiso esta negado (SG-02).
 */
public final class SesionDeRestaurantes implements AutorizacionDeRestaurantes {

    @Override
    public boolean puede(ContextoNegocio contexto, String permiso) {
        try {
            var sesion = SesionActual.sesion();
            var propio = sesion.contexto();
            return propio.isPresent() && propio.get().negocioId().equals(contexto.negocioId())
                    && sesion.autorizacion().puede(permiso);
        } catch (AuthenticationException sinSesion) {
            return false;
        }
    }
}
