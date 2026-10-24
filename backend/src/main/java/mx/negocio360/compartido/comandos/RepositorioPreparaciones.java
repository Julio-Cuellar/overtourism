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
     * Busca por hash del token dentro de un negocio. Con PostgreSQL la seguridad por fila no muestra una fila sin el
     * negocio, y la preparacion de un negocio no la ve otro.
     *
     * @param negocioId negocio del contexto autenticado
     * @param tokenHash SHA-256 del token
     * @return la fila, si existe en ese negocio
     */
    Optional<Preparacion> porHash(UUID negocioId, String tokenHash);

    /**
     * Marca el token como usado solo si todavia no lo estaba. Debe ser atomico: es lo que hace que un token se use
     * una sola vez aunque lleguen dos confirmaciones a la vez. La fila deja de conservar el comando y el resumen (SC-10).
     * Con PostgreSQL se confirma en su propia transaccion: un fallo del comando no devuelve el token a la vida.
     *
     * @param negocioId negocio del contexto autenticado
     * @param tokenHash SHA-256 del token
     * @param ahora     instante de uso
     * @return verdadero si esta llamada lo marco; falso si otra peticion ya lo habia usado
     */
    boolean marcarUsada(UUID negocioId, String tokenHash, Instant ahora);

    /**
     * Vacia el comando y el resumen y conserva el resto de la fila. Con PostgreSQL corre en su propia transaccion.
     *
     * @param negocioId negocio del contexto autenticado
     * @param tokenHash SHA-256 del token
     */
    void vaciar(UUID negocioId, String tokenHash);

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
