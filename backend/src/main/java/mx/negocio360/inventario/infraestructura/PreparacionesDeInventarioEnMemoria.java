package mx.negocio360.inventario.infraestructura;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import mx.negocio360.inventario.aplicacion.Preparacion;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioPreparaciones;

/**
 * La tabla {@code preparacion} en memoria, para desarrollo y demo. {@code marcarUsada} esta sincronizada: dos
 * confirmaciones a la vez del mismo token no pueden ganar las dos.
 */
public final class PreparacionesDeInventarioEnMemoria implements RepositorioPreparaciones {

    private final Map<String, Preparacion> filas = new HashMap<>();

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
