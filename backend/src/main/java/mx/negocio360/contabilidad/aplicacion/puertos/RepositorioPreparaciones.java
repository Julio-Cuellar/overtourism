package mx.negocio360.contabilidad.aplicacion.puertos;

import java.time.Instant;
import java.util.Optional;
import mx.negocio360.contabilidad.aplicacion.Preparacion;

/** Puerto de la tabla {@code contabilidad.preparacion}: el token de un solo uso del paso preparar -> confirmar. */
public interface RepositorioPreparaciones {

    /**
     * Guarda una preparacion nueva.
     *
     * @param preparacion fila a guardar
     */
    void guardar(Preparacion preparacion);

    /**
     * Busca una preparacion por el hash de su token.
     *
     * @param tokenHash SHA-256 en hexadecimal
     * @return la fila, o vacio
     */
    Optional<Preparacion> porHash(String tokenHash);

    /**
     * Marca el token como usado solo si todavia no lo estaba y devuelve verdadero. Si otra peticion ya lo uso,
     * devuelve falso. Debe ser atomico: es lo que hace que un token se use una sola vez aunque lleguen dos
     * confirmaciones a la vez.
     */
    boolean marcarUsada(String tokenHash, Instant ahora);

    /** Vacia el comando y el resumen, y conserva el resto de la fila.
     *
     * @param tokenHash SHA-256 en hexadecimal
     */
    void vaciar(String tokenHash);
}
