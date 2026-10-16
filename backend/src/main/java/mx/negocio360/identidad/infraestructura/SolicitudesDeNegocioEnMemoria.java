package mx.negocio360.identidad.infraestructura;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import mx.negocio360.identidad.aplicacion.puertos.SolicitudesDeNegocio;
import mx.negocio360.identidad.dominio.SolicitudDeNegocio;

/** Solicitudes de negocio en memoria. Solo para desarrollo, demo y pruebas: se pierden al reiniciar. */
public final class SolicitudesDeNegocioEnMemoria implements SolicitudesDeNegocio {

    private final Map<UUID, SolicitudDeNegocio> porNegocio = new ConcurrentHashMap<>();

    @Override
    public void guardar(SolicitudDeNegocio solicitud) {
        porNegocio.put(solicitud.negocioId(), solicitud);
    }

    @Override
    public Optional<SolicitudDeNegocio> porNegocio(UUID negocioId) {
        return Optional.ofNullable(porNegocio.get(negocioId));
    }

    @Override
    public List<SolicitudDeNegocio> deCuenta(UUID cuentaId) {
        return porNegocio.values().stream().filter(s -> s.cuentaId().equals(cuentaId))
                .sorted(Comparator.comparing(SolicitudDeNegocio::creadaEn).thenComparing(SolicitudDeNegocio::negocioId))
                .toList();
    }

    @Override
    public void eliminar(UUID negocioId) {
        porNegocio.remove(negocioId);
    }
}
