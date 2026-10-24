package mx.negocio360.ventas.infraestructura;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.catalogo.FuenteDeProductos;
import mx.negocio360.compartido.catalogo.ProductoParaVenta;
import mx.negocio360.ventas.aplicacion.puertos.CatalogoDeVentas;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Junta las {@link FuenteDeProductos} del sistema en el unico {@link CatalogoDeVentas} que ve Ventas (D-128). Para cada
 * producto gana la primera fuente que lo conoce; un id que ninguna conoce no aparece (y Ventas responde
 * {@code PRODUCTO_NO_ENCONTRADO}). Si dos fuentes reclaman el mismo id con datos distintos es un error de programacion:
 * se queda con la primera y se deja un aviso con el id, nunca con el precio.
 */
public final class CatalogoCompuesto implements CatalogoDeVentas {

    private static final Logger REGISTRO = LoggerFactory.getLogger(CatalogoCompuesto.class);
    private static final int MAX_PRODUCTOS_POR_CONSULTA = 100;
    private static final int MAX_RESULTADOS_DE_BUSQUEDA = 50;

    private final List<FuenteDeProductos> fuentes;

    /**
     * Arma el catalogo con las fuentes disponibles.
     *
     * @param fuentes fuentes de productos, en orden de prioridad; puede estar vacia
     */
    public CatalogoCompuesto(List<FuenteDeProductos> fuentes) {
        this.fuentes = List.copyOf(Objects.requireNonNull(fuentes, "fuentes"));
    }

    @Override
    public Map<UUID, ProductoParaVenta> preciosParaVenta(UUID negocioId, Set<UUID> productoIds) {
        Objects.requireNonNull(negocioId, "negocioId");
        if (productoIds.size() > MAX_PRODUCTOS_POR_CONSULTA) {
            throw new IllegalArgumentException("Se piden mas de " + MAX_PRODUCTOS_POR_CONSULTA + " productos");
        }
        var encontrados = new LinkedHashMap<UUID, ProductoParaVenta>();
        for (var fuente : fuentes) {
            fuente.preciosParaVenta(negocioId, productoIds).forEach((id, producto) -> {
                if (!productoIds.contains(id) || !producto.productoId().equals(id)) {
                    return;
                }
                var anterior = encontrados.putIfAbsent(id, producto);
                if (anterior != null && !anterior.equals(producto)) {
                    // Error de programacion: se queda con la primera fuente y se avisa sin datos del producto.
                    REGISTRO.warn("Dos fuentes de productos reclaman el mismo producto con datos distintos: {}", id);
                }
            });
        }
        return Map.copyOf(encontrados);
    }

    @Override
    public List<ProductoParaVenta> buscar(UUID negocioId, String texto, int limite) {
        Objects.requireNonNull(negocioId, "negocioId");
        var tope = Math.max(0, Math.min(limite, MAX_RESULTADOS_DE_BUSQUEDA));
        var vistos = new LinkedHashMap<UUID, ProductoParaVenta>();
        for (var fuente : fuentes) {
            if (vistos.size() >= tope) {
                break;
            }
            for (var producto : fuente.buscar(negocioId, texto, tope)) {
                if (producto.activo()) {
                    vistos.putIfAbsent(producto.productoId(), producto);
                }
            }
        }
        return new ArrayList<>(vistos.values()).subList(0, Math.min(tope, vistos.size()));
    }
}
