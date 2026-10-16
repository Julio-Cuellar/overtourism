package mx.negocio360.clientes.aplicacion.puertos;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.clientes.aplicacion.Preparacion;

/**
 * Puerto de la tabla {@code preparacion}: el token de un solo uso del paso preparar -> confirmar. Toda operacion lleva el
 * negocio de quien confirma, porque la fila esta bajo seguridad por fila: sin el negocio la base no la muestra. Un
 * negocio nunca ve ni toca la preparacion de otro.
 */
public interface RepositorioPreparaciones {

    void guardar(Preparacion preparacion);

    Optional<Preparacion> porHash(UUID negocioId, String tokenHash);

    /**
     * Marca el token como usado solo si todavia no lo estaba y devuelve verdadero. Si otra peticion ya lo uso,
     * devuelve falso. Debe ser atomico: es lo que hace que un token se use una sola vez aunque lleguen dos
     * confirmaciones a la vez. Se confirma en su propia transaccion, aparte de la de quien la llama, para que un fallo del
     * comando no devuelva el token a la vida. La base impone ademas que una preparacion usada no conserva el comando.
     */
    boolean marcarUsada(UUID negocioId, String tokenHash, Instant ahora);

    /**
     * Vacia el comando y el resumen, que llevan datos personales (SC-10), y conserva el resto de la fila. En su propia
     * transaccion. Una preparacion sin comando que no estuviera usada se marca usada: la base exige comando o uso, y un
     * token sin comando ya no sirve para nada.
     */
    void vaciar(UUID negocioId, String tokenHash);
}
