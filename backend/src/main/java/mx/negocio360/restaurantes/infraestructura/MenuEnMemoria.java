package mx.negocio360.restaurantes.infraestructura;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import mx.negocio360.restaurantes.aplicacion.RepositorioMenu;
import mx.negocio360.restaurantes.dominio.Categoria;
import mx.negocio360.restaurantes.dominio.Platillo;

/**
 * El menu en memoria, para pruebas y para la demostracion mientras la persistencia (R7) no llega. Todo se filtra por negocio,
 * igual que lo hara la seguridad por fila. Es seguro entre hilos; los datos se pierden al reiniciar.
 */
public final class MenuEnMemoria implements RepositorioMenu {

    private final Map<UUID, Categoria> categorias = new ConcurrentHashMap<>();
    private final Map<UUID, Platillo> platillos = new ConcurrentHashMap<>();

    @Override
    public List<Categoria> categorias(UUID negocioId) {
        return categorias.values().stream().filter(c -> c.negocioId().equals(negocioId)).toList();
    }

    @Override
    public Optional<Categoria> categoria(UUID negocioId, UUID id) {
        return Optional.ofNullable(categorias.get(id)).filter(c -> c.negocioId().equals(negocioId));
    }

    @Override
    public void insertar(Categoria categoria) {
        if (categorias.putIfAbsent(categoria.id(), categoria) != null) {
            throw new IllegalStateException("La categoria ya existe");
        }
    }

    @Override
    public boolean reemplazar(Categoria nueva, long versionAnterior) {
        var guardada = categorias.get(nueva.id());
        if (guardada == null || !guardada.negocioId().equals(nueva.negocioId()) || guardada.version() != versionAnterior) {
            return false;
        }
        return categorias.replace(nueva.id(), guardada, nueva);
    }

    @Override
    public List<Platillo> platillos(UUID negocioId) {
        return new ArrayList<>(platillos.values().stream().filter(p -> p.negocioId().equals(negocioId)).toList());
    }

    @Override
    public Optional<Platillo> platillo(UUID negocioId, UUID id) {
        return Optional.ofNullable(platillos.get(id)).filter(p -> p.negocioId().equals(negocioId));
    }

    @Override
    public Map<UUID, Platillo> platillos(UUID negocioId, Set<UUID> ids) {
        var respuesta = new HashMap<UUID, Platillo>();
        for (var id : ids) {
            platillo(negocioId, id).ifPresent(p -> respuesta.put(id, p));
        }
        return respuesta;
    }

    @Override
    public void insertar(Platillo platillo) {
        if (platillos.putIfAbsent(platillo.id(), platillo) != null) {
            throw new IllegalStateException("El platillo ya existe");
        }
    }

    @Override
    public boolean reemplazar(Platillo nuevo, long versionAnterior) {
        var guardado = platillos.get(nuevo.id());
        if (guardado == null || !guardado.negocioId().equals(nuevo.negocioId()) || guardado.version() != versionAnterior) {
            return false;
        }
        return platillos.replace(nuevo.id(), guardado, nuevo);
    }
}
