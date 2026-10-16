package mx.negocio360.identidad;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.Autorizacion;

/**
 * Sesion ya autenticada. El filtro de entrada la convierte en {@link ContextoNegocio} y en {@link Autorizacion} cuando
 * hay un negocio activo; sin negocio activo no hay contexto y todo permiso esta negado (AZ-04).
 *
 * @param cuentaId   cuenta autenticada, que es el actor de las operaciones
 * @param nombre     nombre visible
 * @param negocioId  negocio activo, o nulo
 * @param rol        rol de la persona en el negocio activo, o nulo; sale de la membresia actual (AM-08)
 * @param permisos   permisos de ese rol; vacio sin negocio activo
 * @param expiraEn   fin por inactividad, ya renovado con este uso
 */
public record SesionAutenticada(UUID cuentaId, String nombre, UUID negocioId, String rol, Set<String> permisos,
                                Instant expiraEn) {

    public SesionAutenticada {
        permisos = Set.copyOf(permisos);
    }

    public Optional<UUID> negocioActivo() {
        return Optional.ofNullable(negocioId);
    }

    /** El actor es el id de la cuenta (hallazgo I-5): no cambia la forma de ContextoNegocio. */
    public Optional<ContextoNegocio> contextoNegocio() {
        return negocioActivo().map(negocio -> new ContextoNegocio(negocio, cuentaId.toString()));
    }

    public Autorizacion autorizacion() {
        return Autorizacion.de(permisos);
    }

    @Override
    public String toString() {
        return "SesionAutenticada[cuentaId=" + cuentaId + ", negocioId=" + negocioId + "]";
    }
}
