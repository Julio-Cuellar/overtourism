package mx.negocio360.restaurantes.infraestructura;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import mx.negocio360.restaurantes.aplicacion.ConsumoFallido;
import mx.negocio360.restaurantes.aplicacion.RepositorioConsumos;

/** Los consumos fallidos y los eventos procesados en memoria, para pruebas y demostracion (R7 trae Postgres). */
public final class ConsumosEnMemoria implements RepositorioConsumos {

    private final List<ConsumoFallido> fallidos = new CopyOnWriteArrayList<>();
    private final Set<String> procesados = ConcurrentHashMap.newKeySet();

    @Override
    public boolean marcarProcesado(String consumidor, UUID eventoId) {
        return procesados.add(consumidor + "|" + eventoId);
    }

    @Override
    public boolean yaProcesado(String consumidor, UUID eventoId) {
        return procesados.contains(consumidor + "|" + eventoId);
    }

    @Override
    public void guardarFallido(ConsumoFallido fallido) {
        fallidos.add(fallido);
    }

    @Override
    public List<ConsumoFallido> fallidos(UUID negocioId) {
        return fallidos.stream().filter(f -> f.negocioId().equals(negocioId))
                .sorted(Comparator.comparing(ConsumoFallido::creadoEn).thenComparing(ConsumoFallido::id).reversed()).toList();
    }
}
