package mx.negocio360.restaurantes.infraestructura;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import mx.negocio360.restaurantes.aplicacion.RepositorioSalon;
import mx.negocio360.restaurantes.dominio.Mesa;
import mx.negocio360.restaurantes.dominio.Servicio;
import mx.negocio360.restaurantes.dominio.Zona;

/** Zonas, mesas y servicio en memoria, para pruebas y demostracion (R7 trae Postgres). Atomico por operacion. */
public final class SalonEnMemoria implements RepositorioSalon {

    private final Map<UUID, Zona> zonas = new ConcurrentHashMap<>();
    private final Map<UUID, Mesa> mesas = new ConcurrentHashMap<>();
    private final Map<UUID, Servicio> servicios = new ConcurrentHashMap<>();

    @Override
    public List<Zona> zonas(UUID negocioId) {
        return new ArrayList<>(zonas.values().stream().filter(z -> z.negocioId().equals(negocioId)).toList());
    }

    @Override
    public Optional<Zona> zona(UUID negocioId, UUID id) {
        return Optional.ofNullable(zonas.get(id)).filter(z -> z.negocioId().equals(negocioId));
    }

    @Override
    public void insertar(Zona zona) {
        zonas.put(zona.id(), zona);
    }

    @Override
    public boolean reemplazar(Zona nueva, long versionAnterior) {
        var resultado = new boolean[1];
        zonas.computeIfPresent(nueva.id(), (id, actual) -> {
            if (actual.version() == versionAnterior && actual.negocioId().equals(nueva.negocioId())) {
                resultado[0] = true;
                return nueva;
            }
            return actual;
        });
        return resultado[0];
    }

    @Override
    public List<Mesa> mesas(UUID negocioId) {
        return new ArrayList<>(mesas.values().stream().filter(m -> m.negocioId().equals(negocioId)).toList());
    }

    @Override
    public Optional<Mesa> mesa(UUID negocioId, UUID id) {
        return Optional.ofNullable(mesas.get(id)).filter(m -> m.negocioId().equals(negocioId));
    }

    @Override
    public void insertar(Mesa mesa) {
        mesas.put(mesa.id(), mesa);
    }

    @Override
    public boolean reemplazar(Mesa nueva, long versionAnterior) {
        var resultado = new boolean[1];
        mesas.computeIfPresent(nueva.id(), (id, actual) -> {
            if (actual.version() == versionAnterior && actual.negocioId().equals(nueva.negocioId())) {
                resultado[0] = true;
                return nueva;
            }
            return actual;
        });
        return resultado[0];
    }

    @Override
    public Optional<Mesa> mesaPorToken(String token) {
        return mesas.values().stream().filter(m -> m.activa() && m.qrToken().equals(token)).findFirst();
    }

    @Override
    public Servicio servicio(UUID negocioId) {
        return servicios.getOrDefault(negocioId, Servicio.inicial(negocioId));
    }

    @Override
    public void guardar(Servicio servicio) {
        servicios.put(servicio.negocioId(), servicio);
    }
}
