package mx.negocio360.compartido.seguridad;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;

/**
 * Lo que la plataforma sabe de quien hace la peticion, ya autenticado: la cuenta que actua, su negocio activo y sus
 * permisos en el. Es la forma neutral con la que Identidad entrega la sesion sin que ningun modulo conozca sus tipos
 * (R1 y R4). Sin negocio activo no hay contexto y todo permiso esta negado (AZ-04), aunque la sesion trajera permisos.
 *
 * @param actor     id de la cuenta, para la auditoria
 * @param negocioId negocio activo, o nulo
 * @param permisos  permisos de la persona en ese negocio; vacio sin negocio activo
 */
public record SesionDePeticion(String actor, UUID negocioId, Set<String> permisos) {

    public SesionDePeticion {
        Objects.requireNonNull(actor, "actor");
        if (actor.isBlank()) {
            throw new IllegalArgumentException("El actor no puede estar vacio");
        }
        permisos = negocioId == null ? Set.of() : Set.copyOf(Objects.requireNonNull(permisos, "permisos"));
    }

    public Optional<ContextoNegocio> contexto() {
        return Optional.ofNullable(negocioId).map(negocio -> new ContextoNegocio(negocio, actor));
    }

    public Autorizacion autorizacion() {
        return Autorizacion.de(permisos);
    }

    /** Sin la lista de permisos: un registro no debe poder copiarla. */
    @Override
    public String toString() {
        return "SesionDePeticion[actor=" + actor + ", negocioId=" + negocioId + "]";
    }
}
