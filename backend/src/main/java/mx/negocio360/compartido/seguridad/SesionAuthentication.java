package mx.negocio360.compartido.seguridad;

import java.util.Objects;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * La autenticacion de Spring Security de una peticion con sesion valida. Los permisos son las autoridades, tal cual
 * (sin prefijo de rol): {@code hasAuthority("clientes.sugerir")}. No guarda credenciales.
 */
public final class SesionAuthentication extends AbstractAuthenticationToken {

    private static final long serialVersionUID = 1L;

    private final SesionDePeticion sesion;

    public SesionAuthentication(SesionDePeticion sesion) {
        super(Objects.requireNonNull(sesion, "sesion").permisos().stream().map(SimpleGrantedAuthority::new).toList());
        this.sesion = sesion;
        setAuthenticated(true);
    }

    public SesionDePeticion sesion() {
        return sesion;
    }

    @Override
    public Object getCredentials() {
        return null;
    }

    @Override
    public Object getPrincipal() {
        return sesion;
    }

    @Override
    public String getName() {
        return sesion.actor();
    }

    @Override
    public String toString() {
        return "SesionAuthentication[actor=" + sesion.actor() + ", negocioId=" + sesion.negocioId() + "]";
    }
}
