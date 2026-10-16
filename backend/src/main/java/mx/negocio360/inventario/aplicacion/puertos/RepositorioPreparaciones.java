package mx.negocio360.inventario.aplicacion.puertos;

import java.time.Instant;
import java.util.Optional;
import mx.negocio360.inventario.aplicacion.Preparacion;

/** Puerto de la tabla {@code preparacion}: el token de un solo uso del paso preparar -> confirmar. */
public interface RepositorioPreparaciones {

    void guardar(Preparacion preparacion);

    Optional<Preparacion> porHash(String tokenHash);

    /**
     * Marca el token como usado solo si todavia no lo estaba y devuelve verdadero. Si otra peticion ya lo uso,
     * devuelve falso. Debe ser atomico: es lo que hace que un token se use una sola vez aunque lleguen dos
     * confirmaciones a la vez.
     */
    boolean marcarUsada(String tokenHash, Instant ahora);
}
