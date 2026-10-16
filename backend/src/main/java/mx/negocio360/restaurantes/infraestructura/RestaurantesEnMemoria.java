package mx.negocio360.restaurantes.infraestructura;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import mx.negocio360.restaurantes.aplicacion.RepositorioRestaurantes;
import mx.negocio360.restaurantes.dominio.Restaurante;

/** La extension de restaurante y los eventos procesados en memoria, para pruebas y demostracion (R7 trae Postgres). */
public final class RestaurantesEnMemoria implements RepositorioRestaurantes {

    private final Map<UUID, Restaurante> filas = new ConcurrentHashMap<>();
    private final Set<String> procesados = ConcurrentHashMap.newKeySet();

    @Override
    public Optional<Restaurante> porNegocio(UUID negocioId) {
        return Optional.ofNullable(filas.get(negocioId));
    }

    @Override
    public void guardar(Restaurante restaurante) {
        filas.put(restaurante.negocioId(), restaurante);
    }

    @Override
    public boolean marcarProcesado(String consumidor, UUID eventoId) {
        return procesados.add(consumidor + "|" + eventoId);
    }

    @Override
    public boolean yaProcesado(String consumidor, UUID eventoId) {
        return procesados.contains(consumidor + "|" + eventoId);
    }
}
