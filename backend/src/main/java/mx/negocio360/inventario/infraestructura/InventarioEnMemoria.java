package mx.negocio360.inventario.infraestructura;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.inventario.aplicacion.ArticuloConSaldo;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioArticulos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioMovimientos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioSaldos;
import mx.negocio360.inventario.dominio.Articulo;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.InventarioException;
import mx.negocio360.inventario.dominio.Movimiento;
import mx.negocio360.inventario.dominio.Saldo;

/**
 * Esquema {@code inventario} en memoria, para desarrollo y demo: articulos, saldos y movimientos en un solo lugar, para
 * que la busqueda pueda cruzar articulo y saldo como lo haria la consulta SQL. Cada operacion esta sincronizada y la
 * actualizacion respeta la version (bloqueo optimista, CONCURRENCIA). Los datos se pierden al reiniciar; la version con
 * PostgreSQL esta pendiente.
 */
public final class InventarioEnMemoria implements RepositorioArticulos, RepositorioSaldos, RepositorioMovimientos {

    private final Map<UUID, Articulo> articulos = new LinkedHashMap<>();
    private final Map<UUID, Saldo> saldos = new LinkedHashMap<>();
    private final List<Movimiento> movimientos = new ArrayList<>();

    @Override
    public synchronized Optional<Articulo> porId(UUID negocioId, UUID id) {
        return Optional.ofNullable(articulos.get(id)).filter(a -> a.negocioId().equals(negocioId));
    }

    @Override
    public synchronized Optional<Articulo> activoPorNombre(UUID negocioId, String nombre) {
        var buscado = nombre.toLowerCase(Locale.ROOT);
        return articulos.values().stream()
                .filter(a -> a.negocioId().equals(negocioId) && a.activo())
                .filter(a -> a.nombre().toLowerCase(Locale.ROOT).equals(buscado))
                .findFirst();
    }

    @Override
    public synchronized void insertar(Articulo articulo) {
        articulos.put(articulo.id(), articulo);
    }

    @Override
    public synchronized void actualizar(Articulo nuevo, long versionEsperada) {
        var actual = articulos.get(nuevo.id());
        if (actual == null || actual.version() != versionEsperada) {
            throw new InventarioException(CodigoError.CONCURRENCIA);
        }
        articulos.put(nuevo.id(), nuevo);
    }

    @Override
    public synchronized List<ArticuloConSaldo> buscar(UUID negocioId, String texto, boolean soloAlertas,
                                                      UUID despuesDe, int limite) {
        var parte = texto == null ? "" : texto.strip().toLowerCase(Locale.ROOT);
        return articulos.values().stream()
                .filter(a -> a.negocioId().equals(negocioId) && a.activo())
                .filter(a -> a.nombre().toLowerCase(Locale.ROOT).contains(parte))
                .filter(a -> despuesDe == null || a.id().compareTo(despuesDe) > 0)
                .sorted(Comparator.comparing(Articulo::id))
                .map(a -> new ArticuloConSaldo(a, saldos.get(a.id())))
                .filter(fila -> !soloAlertas || fila.enAlerta())
                .limit(limite)
                .toList();
    }

    @Override
    public synchronized Optional<Saldo> porArticulo(UUID negocioId, UUID articuloId) {
        return Optional.ofNullable(saldos.get(articuloId)).filter(s -> s.negocioId().equals(negocioId));
    }

    @Override
    public synchronized void insertar(Saldo saldo) {
        saldos.put(saldo.articuloId(), saldo);
    }

    @Override
    public synchronized void actualizar(Saldo nuevo, long versionEsperada) {
        var actual = saldos.get(nuevo.articuloId());
        if (actual == null || actual.version() != versionEsperada) {
            throw new InventarioException(CodigoError.CONCURRENCIA);
        }
        saldos.put(nuevo.articuloId(), nuevo);
    }

    @Override
    public synchronized void insertar(Movimiento movimiento) {
        movimientos.add(movimiento);
    }

    @Override
    public synchronized List<Movimiento> kardex(UUID negocioId, UUID articuloId, UUID despuesDe, int limite) {
        return movimientos.stream()
                .filter(m -> m.negocioId().equals(negocioId) && m.articuloId().equals(articuloId))
                .filter(m -> despuesDe == null || m.id().compareTo(despuesDe) > 0)
                .sorted(Comparator.comparing(Movimiento::id))
                .limit(limite)
                .toList();
    }
}
