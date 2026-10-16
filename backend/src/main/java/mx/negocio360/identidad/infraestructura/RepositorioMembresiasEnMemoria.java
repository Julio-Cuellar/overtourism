package mx.negocio360.identidad.infraestructura;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioMembresias;
import mx.negocio360.identidad.dominio.EstadoMembresia;
import mx.negocio360.identidad.dominio.Membresia;

/**
 * Membresias en memoria. Solo para desarrollo, demo y pruebas: se pierden al reiniciar. Imita al indice unico parcial
 * de V022: una cuenta puede tener varias membresias en un negocio (las revocadas no vuelven, se invita de nuevo), pero a
 * lo mas una no revocada.
 */
public final class RepositorioMembresiasEnMemoria implements RepositorioMembresias {

    private final Map<UUID, Membresia> porId = new LinkedHashMap<>();

    @Override
    public synchronized Membresia guardar(Membresia membresia) {
        if (membresia.estado() != EstadoMembresia.REVOCADA) {
            var otraVigente = porId.values().stream().anyMatch(m -> !m.id().equals(membresia.id())
                    && m.cuentaId().equals(membresia.cuentaId()) && m.negocioId().equals(membresia.negocioId())
                    && m.estado() != EstadoMembresia.REVOCADA);
            if (otraVigente) {
                throw new IllegalStateException("La cuenta ya tiene una membresia vigente en ese negocio");
            }
        }
        porId.put(membresia.id(), membresia);
        return membresia;
    }

    /** La vigente (activa o suspendida) si la hay; si solo hay revocadas, la mas reciente. */
    @Override
    public synchronized Optional<Membresia> porCuentaYNegocio(UUID cuentaId, UUID negocioId) {
        var todas = porId.values().stream()
                .filter(m -> m.cuentaId().equals(cuentaId) && m.negocioId().equals(negocioId)).toList();
        return todas.stream().filter(m -> m.estado() != EstadoMembresia.REVOCADA).findFirst()
                .or(() -> todas.stream().max(Comparator.comparing(Membresia::creadaEn)));
    }

    @Override
    public synchronized List<Membresia> deCuenta(UUID cuentaId) {
        return porId.values().stream().filter(m -> m.cuentaId().equals(cuentaId)).toList();
    }

    @Override
    public synchronized List<Membresia> deNegocio(UUID negocioId) {
        return porId.values().stream().filter(m -> m.negocioId().equals(negocioId)).toList();
    }
}
