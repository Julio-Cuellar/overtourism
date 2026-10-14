package mx.negocio360.identidad;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;

/**
 * Sesion ya autenticada. El filtro de entrada la convierte en {@link ContextoNegocio} cuando hay un negocio activo;
 * mientras no lo haya (la persona aun no elige negocio) no existe contexto de negocio.
 *
 * @param cuentaId   cuenta autenticada, que es el actor de las operaciones
 * @param nombre     nombre visible
 * @param negocioId  negocio activo, o nulo
 * @param expiraEn   fin por inactividad, ya renovado con este uso
 */
public record SesionAutenticada(UUID cuentaId, String nombre, UUID negocioId, Instant expiraEn) {

    public Optional<UUID> negocioActivo() {
        return Optional.ofNullable(negocioId);
    }

    /** El actor es el id de la cuenta (hallazgo I-5): no cambia la forma de ContextoNegocio. */
    public Optional<ContextoNegocio> contextoNegocio() {
        return negocioActivo().map(negocio -> new ContextoNegocio(negocio, cuentaId.toString()));
    }

    @Override
    public String toString() {
        return "SesionAutenticada[cuentaId=" + cuentaId + ", negocioId=" + negocioId + "]";
    }
}
