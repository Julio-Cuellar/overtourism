package mx.negocio360.ventas.infraestructura;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.ventas.aplicacion.puertos.RepositorioVentas;
import mx.negocio360.ventas.dominio.Venta;

/**
 * Tablas {@code venta}, {@code linea_venta}, {@code pago_venta} y {@code serie_folio} en memoria, para desarrollo y
 * demo (F4). Cada operacion esta sincronizada: el folio no se repite (VT-06) y la actualizacion respeta la version
 * (VT-11). Los datos se pierden al reiniciar; la version con PostgreSQL es F5.
 */
public final class VentasEnMemoria implements RepositorioVentas {

    private final Map<UUID, Venta> ventas = new LinkedHashMap<>();
    private final Map<String, Long> folios = new HashMap<>();

    @Override
    public synchronized void guardar(Venta venta) {
        ventas.put(venta.id(), venta);
    }

    @Override
    public synchronized Optional<Venta> porId(UUID negocioId, UUID ventaId) {
        return Optional.ofNullable(ventas.get(ventaId)).filter(v -> v.negocioId().equals(negocioId));
    }

    @Override
    public synchronized List<Venta> porTurno(UUID negocioId, UUID turnoId) {
        return ventas.values().stream()
                .filter(v -> v.negocioId().equals(negocioId) && v.turnoId().equals(turnoId))
                .toList();
    }

    @Override
    public synchronized long siguienteFolio(UUID negocioId, String serie) {
        return folios.merge(negocioId + "/" + serie, 1L, Long::sum);
    }

    @Override
    public synchronized boolean actualizar(Venta venta, int versionEsperada) {
        var actual = ventas.get(venta.id());
        if (actual == null || actual.version() != versionEsperada) {
            return false;
        }
        ventas.put(venta.id(), venta);
        return true;
    }
}
