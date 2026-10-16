package mx.negocio360.inventario.infraestructura;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.inventario.aplicacion.ArticuloConSaldo;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioArticulos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioConsumos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioMovimientos;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioSaldos;
import mx.negocio360.inventario.dominio.Articulo;
import mx.negocio360.inventario.dominio.CodigoError;
import mx.negocio360.inventario.dominio.Consumo;
import mx.negocio360.inventario.dominio.InventarioException;
import mx.negocio360.inventario.dominio.Movimiento;
import mx.negocio360.inventario.dominio.Saldo;

/**
 * Las tablas {@code articulo}, {@code saldo}, {@code movimiento} y {@code consumo} en memoria, para desarrollo y demo.
 * Cada operacion esta sincronizada y respeta el bloqueo optimista por version, igual que lo hara la version con
 * PostgreSQL. Los datos se pierden al reiniciar y no hay transaccion que junte varias escrituras, asi que la atomicidad
 * del consumo (K-02) solo la da la version persistente.
 */
public final class InventarioEnMemoria implements RepositorioArticulos, RepositorioSaldos, RepositorioMovimientos,
        RepositorioConsumos {

    private record Llave(UUID negocioId, UUID causaId, String consumidor) {
    }

    private final Map<UUID, Articulo> articulos = new LinkedHashMap<>();
    private final Map<UUID, Saldo> saldos = new LinkedHashMap<>();
    private final List<Movimiento> movimientos = new ArrayList<>();
    private final Map<Llave, Consumo> consumos = new HashMap<>();

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

    @Override
    public synchronized Optional<Consumo> porLlave(UUID negocioId, UUID causaId, String consumidor) {
        return Optional.ofNullable(consumos.get(new Llave(negocioId, causaId, consumidor)));
    }

    @Override
    public synchronized void insertar(Consumo consumo) {
        var llave = new Llave(consumo.negocioId(), consumo.causaId(), consumo.consumidor());
        if (consumos.putIfAbsent(llave, consumo) != null) {
            throw new InventarioException(CodigoError.CONCURRENCIA);
        }
    }

    @Override
    public synchronized void actualizar(Consumo consumo) {
        var llave = new Llave(consumo.negocioId(), consumo.causaId(), consumo.consumidor());
        if (!consumos.containsKey(llave)) {
            throw new InventarioException(CodigoError.CONCURRENCIA);
        }
        consumos.put(llave, consumo);
    }
}
