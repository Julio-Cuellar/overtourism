package mx.negocio360.restaurantes.infraestructura;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import mx.negocio360.restaurantes.aplicacion.ConsultaDeRecetas;
import mx.negocio360.restaurantes.aplicacion.RepositorioRecetas;
import mx.negocio360.restaurantes.dominio.Receta;

/**
 * Las recetas en memoria, para pruebas y demostracion mientras la persistencia (R7) no llega. Tambien responde al menu
 * cuales platillos tienen receta activa. Todo se filtra por negocio.
 */
public final class RecetasEnMemoria implements RepositorioRecetas, ConsultaDeRecetas {

    private final Map<UUID, Receta> porPlatillo = new ConcurrentHashMap<>();

    @Override
    public Optional<Receta> porPlatillo(UUID negocioId, UUID platilloId) {
        return Optional.ofNullable(porPlatillo.get(platilloId)).filter(r -> r.negocioId().equals(negocioId));
    }

    @Override
    public Map<UUID, Receta> porPlatillos(UUID negocioId, Set<UUID> platilloIds) {
        var respuesta = new HashMap<UUID, Receta>();
        for (var id : platilloIds) {
            porPlatillo(negocioId, id).ifPresent(r -> respuesta.put(id, r));
        }
        return respuesta;
    }

    @Override
    public void insertar(Receta receta) {
        if (porPlatillo.putIfAbsent(receta.platilloId(), receta) != null) {
            throw new IllegalStateException("El platillo ya tiene receta");
        }
    }

    @Override
    public boolean reemplazar(Receta nueva, long versionAnterior) {
        var guardada = porPlatillo.get(nueva.platilloId());
        if (guardada == null || !guardada.negocioId().equals(nueva.negocioId()) || guardada.version() != versionAnterior) {
            return false;
        }
        return porPlatillo.replace(nueva.platilloId(), guardada, nueva);
    }

    @Override
    public boolean tieneRecetaActiva(UUID negocioId, UUID platilloId) {
        return porPlatillo(negocioId, platilloId).filter(Receta::activa).isPresent();
    }

    @Override
    public Set<UUID> platillosConReceta(UUID negocioId) {
        return porPlatillo.values().stream().filter(r -> r.negocioId().equals(negocioId) && r.activa())
                .map(Receta::platilloId).collect(Collectors.toUnmodifiableSet());
    }
}
