package mx.negocio360.ventas.fakes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.ventas.aplicacion.puertos.CatalogoDeVentas;
import mx.negocio360.compartido.catalogo.ProductoParaVenta;

/** Doble de Catálogo: productos por negocio que la prueba puede cambiar entre preparar y confirmar. */
public final class CatalogoEnMemoria implements CatalogoDeVentas {

    private final Map<UUID, Map<UUID, ProductoParaVenta>> porNegocio = new HashMap<>();
    private int consultas;

    public CatalogoEnMemoria con(UUID negocioId, ProductoParaVenta producto) {
        porNegocio.computeIfAbsent(negocioId, n -> new HashMap<>()).put(producto.productoId(), producto);
        return this;
    }

    @Override
    public Map<UUID, ProductoParaVenta> preciosParaVenta(UUID negocioId, Set<UUID> productoIds) {
        consultas++;
        var productos = porNegocio.getOrDefault(negocioId, Map.of());
        var encontrados = new HashMap<UUID, ProductoParaVenta>();
        for (var id : productoIds) {
            var producto = productos.get(id);
            if (producto != null) {
                encontrados.put(id, producto);
            }
        }
        return Map.copyOf(encontrados);
    }

    @Override
    public List<ProductoParaVenta> buscar(UUID negocioId, String texto, int limite) {
        var minusculas = texto == null ? "" : texto.toLowerCase(java.util.Locale.ROOT);
        return porNegocio.getOrDefault(negocioId, Map.of()).values().stream()
                .filter(ProductoParaVenta::activo)
                .filter(p -> p.descripcion().toLowerCase(java.util.Locale.ROOT).contains(minusculas))
                .sorted(java.util.Comparator.comparing(ProductoParaVenta::descripcion))
                .limit(limite)
                .toList();
    }

    /** Cuantas veces se consulto Catalogo. */
    public int consultas() {
        return consultas;
    }
}
