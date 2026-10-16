package mx.negocio360.compartido.contexto;

import java.util.Objects;
import java.util.UUID;

/**
 * Negocio y actor de la peticion en curso. Los fija la infraestructura al autenticar; ninguna operacion los recibe
 * como argumento del usuario (SG-02).
 *
 * @param negocioId negocio al que pertenece la peticion
 * @param actor     quien actua, para la auditoria (por ejemplo el identificador del locatario)
 */
public record ContextoNegocio(UUID negocioId, String actor) {

    public ContextoNegocio {
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(actor, "actor");
        if (actor.isBlank()) {
            throw new IllegalArgumentException("El actor no puede estar vacio");
        }
    }
}
