package mx.negocio360.ventas.fakes;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.paginacion.Cursores;
import mx.negocio360.compartido.paginacion.Pagina;
import mx.negocio360.ventas.FiltroVentas;
import mx.negocio360.ventas.aplicacion.puertos.RepositorioVentas;
import mx.negocio360.ventas.dominio.Venta;

/** Doble de las tablas {@code venta}, {@code linea_venta}, {@code pago_venta} y {@code serie_folio}. */
public final class RepositorioVentasEnMemoria implements RepositorioVentas {

    private final Map<UUID, Venta> ventas = new LinkedHashMap<>();
    private final Map<String, Long> folios = new HashMap<>();

    /** Cuando es verdadero, otra peticion cambio la venta antes: actualizar responde falso. */
    public boolean simularConcurrencia;

    @Override
    public void guardar(Venta venta) {
        ventas.put(venta.id(), venta);
    }

    @Override
    public Optional<Venta> porId(UUID negocioId, UUID ventaId) {
        return Optional.ofNullable(ventas.get(ventaId)).filter(v -> v.negocioId().equals(negocioId));
    }

    @Override
    public List<Venta> porTurno(UUID negocioId, UUID turnoId) {
        return ventas.values().stream()
                .filter(v -> v.negocioId().equals(negocioId) && v.turnoId().equals(turnoId))
                .toList();
    }

    @Override
    public Pagina<Venta> buscar(UUID negocioId, FiltroVentas filtro, int primeros, String despues) {
        var coinciden = ventas.values().stream()
                .filter(v -> v.negocioId().equals(negocioId))
                .filter(v -> filtro.turnoId() == null || v.turnoId().equals(filtro.turnoId()))
                .filter(v -> filtro.estado() == null || v.estado() == filtro.estado())
                .filter(v -> filtro.desde() == null || !v.ocurridoEn().isBefore(filtro.desde()))
                .filter(v -> filtro.hasta() == null || !v.ocurridoEn().isAfter(filtro.hasta()))
                .filter(v -> filtro.serie() == null || v.serie().equals(filtro.serie()))
                .filter(v -> filtro.folio() == null || v.folio() == filtro.folio())
                .sorted(java.util.Comparator.comparing(Venta::ocurridoEn).thenComparingLong(Venta::folio).reversed())
                .toList();
        return Cursores.pagina(coinciden, primeros, despues);
    }

    @Override
    public long siguienteFolio(UUID negocioId, String serie) {
        return folios.merge(negocioId + "/" + serie, 1L, Long::sum);
    }

    @Override
    public boolean actualizar(Venta venta, int versionEsperada) {
        var actual = ventas.get(venta.id());
        if (simularConcurrencia || actual == null || actual.version() != versionEsperada) {
            return false;
        }
        ventas.put(venta.id(), venta);
        return true;
    }

    public List<Venta> todas() {
        return List.copyOf(ventas.values());
    }
}
