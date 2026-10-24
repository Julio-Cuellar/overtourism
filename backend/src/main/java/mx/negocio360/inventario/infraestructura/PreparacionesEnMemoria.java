package mx.negocio360.inventario.infraestructura;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import mx.negocio360.inventario.aplicacion.Preparacion;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioPreparaciones;

/**
 * Tabla {@code preparacion} de Inventario en memoria. Solo guarda el hash del token, como la tabla real. Cada operacion
 * esta sincronizada para que un token se use una sola vez aunque lleguen dos confirmaciones a la vez (D-23).
 */
public final class PreparacionesEnMemoria implements RepositorioPreparaciones {

    private final Map<String, Preparacion> filas = new LinkedHashMap<>();

    @Override
    public synchronized void guardar(Preparacion preparacion) {
        filas.put(preparacion.tokenHash(), preparacion);
    }

    @Override
    public synchronized Optional<Preparacion> porHash(String tokenHash) {
        return Optional.ofNullable(filas.get(tokenHash));
    }

    @Override
    public synchronized boolean marcarUsada(String tokenHash, Instant ahora) {
        var fila = filas.get(tokenHash);
        if (fila == null || fila.usadoEn() != null) {
            return false;
        }
        filas.put(tokenHash, fila.usada(ahora));
        return true;
    }
}
