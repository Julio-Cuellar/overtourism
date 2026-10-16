package mx.negocio360.inventario.fakes;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioConsumos;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.Consumo;
import mx.negocio360.inventario.dominio.InventarioException;

/** Doble en memoria de la tabla {@code consumo}: una fila por (negocio, causa, consumidor). */
public final class RepositorioConsumosEnMemoria implements RepositorioConsumos {

    private record Llave(UUID negocioId, UUID causaId, String consumidor) {
    }

    private final Map<Llave, Consumo> filas = new HashMap<>();

    @Override
    public Optional<Consumo> porLlave(UUID negocioId, UUID causaId, String consumidor) {
        return Optional.ofNullable(filas.get(new Llave(negocioId, causaId, consumidor)));
    }

    @Override
    public void insertar(Consumo consumo) {
        var llave = new Llave(consumo.negocioId(), consumo.causaId(), consumo.consumidor());
        if (filas.putIfAbsent(llave, consumo) != null) {
            throw new InventarioException(CodigoError.CONCURRENCIA);
        }
    }

    @Override
    public void actualizar(Consumo consumo) {
        var llave = new Llave(consumo.negocioId(), consumo.causaId(), consumo.consumidor());
        if (!filas.containsKey(llave)) {
            throw new InventarioException(CodigoError.CONCURRENCIA);
        }
        filas.put(llave, consumo);
    }

    public int cuantos() {
        return filas.size();
    }
}
