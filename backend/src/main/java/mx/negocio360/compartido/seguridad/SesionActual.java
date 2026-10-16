package mx.negocio360.compartido.seguridad;

import mx.negocio360.compartido.contexto.ContextoNegocio;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * La sesion de la peticion en curso, para los resolvedores y los controladores. El negocio y el actor salen siempre
 * de aqui y nunca de un argumento ni de una cabecera del cliente (SG-02). Spring for GraphQL propaga el contexto de
 * seguridad a los resolvedores, tambien a los asincronos.
 */
public final class SesionActual {

    private SesionActual() {
    }

    /** @throws AuthenticationCredentialsNotFoundException si la peticion no tiene sesion (se responde 401) */
    public static SesionDePeticion sesion() {
        var autenticacion = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacion instanceof SesionAuthentication sesion) {
            return sesion.sesion();
        }
        throw new AuthenticationCredentialsNotFoundException("No hay sesion en la peticion");
    }

    /** @throws AccessDeniedException si la sesion no tiene negocio activo (se responde 403) */
    public static ContextoNegocio contexto() {
        return sesion().contexto().orElseThrow(() -> new AccessDeniedException("No hay negocio activo"));
    }

    public static Autorizacion autorizacion() {
        return sesion().autorizacion();
    }
}
