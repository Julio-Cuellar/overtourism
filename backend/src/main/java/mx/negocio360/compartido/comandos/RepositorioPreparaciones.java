package mx.negocio360.compartido.comandos;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** Puerto de la tabla {@code preparacion} de un modulo: el token de un solo uso de preparar -&gt; confirmar. */
public interface RepositorioPreparaciones {

    /**
     * Guarda una preparacion nueva.
     *
     * @param preparacion fila a guardar
     */
    void guardar(Preparacion preparacion);

    /**
     * Busca por hash del token.
     *
     * @param tokenHash SHA-256 del token
     * @return la fila, si existe
     */
    Optional<Preparacion> porHash(String tokenHash);

    /**
     * Marca el token como usado solo si todavia no lo estaba. Debe ser atomico: es lo que hace que un token se use
     * una sola vez aunque lleguen dos confirmaciones a la vez.
     *
     * @param tokenHash SHA-256 del token
     * @param ahora     instante de uso
     * @return verdadero si esta llamada lo marco; falso si otra peticion ya lo habia usado
     */
    boolean marcarUsada(String tokenHash, Instant ahora);

    /**
     * Vacia el comando y el resumen y conserva el resto de la fila.
     *
     * @param tokenHash SHA-256 del token
     */
    void vaciar(String tokenHash);

    /**
     * Cuenta las preparaciones sin usar y sin vencer de un tipo y ambito (CJ-07).
     *
     * @param negocioId   negocio
     * @param comandoTipo tipo del comando
     * @param ambito      ambito, por ejemplo el id del turno
     * @param ahora       instante de referencia
     * @return numero de preparaciones vigentes
     */
    long vigentes(UUID negocioId, String comandoTipo, String ambito, Instant ahora);
}
