package mx.negocio360.inventario.aplicacion.puertos;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.inventario.aplicacion.Preparacion;

/** Puerto de la tabla {@code preparacion}: el token de un solo uso del paso preparar -> confirmar. */
public interface RepositorioPreparaciones {

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
     * Marca el token como usado solo si todavia no lo estaba y devuelve verdadero. Si otra peticion ya lo uso,
     * devuelve falso. Debe ser atomico: es lo que hace que un token se use una sola vez aunque lleguen dos
     * confirmaciones a la vez. Con PostgreSQL se confirma en su propia transaccion: un fallo del comando no devuelve
     * el token a la vida.
     *
     * @param negocioId negocio del contexto autenticado
     * @param tokenHash SHA-256 del token
     * @param ahora     instante de uso
     * @return verdadero si esta llamada lo marco
     */
    boolean marcarUsada(UUID negocioId, String tokenHash, Instant ahora);
}
