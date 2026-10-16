package mx.negocio360.compartido.comandos;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Tabla {@code preparacion} en memoria para desarrollo y demo (F4, sin base de datos). Solo guarda el hash del token,
 * como la tabla real, y sincroniza cada operacion para que {@link #marcarUsada} sea atomica (un token, un uso). Los
 * datos se pierden al reiniciar; la version con PostgreSQL es F5.
 */
public final class PreparacionesEnMemoria implements RepositorioPreparaciones {

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

    @Override
    public synchronized void vaciar(String tokenHash) {
        var fila = filas.get(tokenHash);
        if (fila != null) {
            filas.put(tokenHash, fila.vaciada());
        }
    }

    @Override
    public synchronized long vigentes(UUID negocioId, String comandoTipo, String ambito, Instant ahora) {
        return filas.values().stream()
                .filter(f -> f.negocioId().equals(negocioId) && f.comandoTipo().equals(comandoTipo))
                .filter(f -> Objects.equals(f.ambito(), ambito))
                .filter(f -> f.usadoEn() == null && ahora.isBefore(f.expiraEn()))
                .count();
    }
}
