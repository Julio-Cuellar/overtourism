package mx.negocio360.canal.demo;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import mx.negocio360.ventas.aplicacion.puertos.CatalogoDeVentas;
import mx.negocio360.ventas.aplicacion.puertos.ProductoParaVenta;

/**
 * Catalogo <b>simulado</b> para la demo (Catalogo aun no existe): productos inventados de una fonda y tienda poblana,
 * iguales para todos los negocios. Los precios son de ejemplo, no de un negocio real, y la app los muestra rotulados
 * como simulados.
 */
public final class CatalogoSimulado implements CatalogoDeVentas {

    private static final List<ProductoParaVenta> PRODUCTOS = List.of(
            producto("0192f1b0-0001-7000-8000-00000000c001", "Mole poblano (orden)", "pieza", "120.00", false),
            producto("0192f1b0-0002-7000-8000-00000000c002", "Chiles en nogada", "pieza", "260.00", false),
            producto("0192f1b0-0003-7000-8000-00000000c003", "Cemita de milanesa", "pieza", "85.00", false),
            producto("0192f1b0-0004-7000-8000-00000000c004", "Agua de jamaica", "pieza", "30.00", false),
            producto("0192f1b0-0005-7000-8000-00000000c005", "Camote poblano a granel", "kg", "140.00", true),
            producto("0192f1b0-0006-7000-8000-00000000c006", "Taza de Talavera", "pieza", "180.00", false));

    private static final Map<UUID, ProductoParaVenta> POR_ID = PRODUCTOS.stream()
            .collect(Collectors.toUnmodifiableMap(ProductoParaVenta::productoId, Function.identity()));

    private static ProductoParaVenta producto(String id, String descripcion, String unidad, String precio,
                                              boolean admiteFraccion) {
        return new ProductoParaVenta(UUID.fromString(id), descripcion, unidad, new BigDecimal(precio), admiteFraccion,
                true);
    }

    @Override
    public Map<UUID, ProductoParaVenta> preciosParaVenta(UUID negocioId, Set<UUID> productoIds) {
        return productoIds.stream().filter(POR_ID::containsKey)
                .collect(Collectors.toUnmodifiableMap(Function.identity(), POR_ID::get));
    }

    /**
     * Todos los productos de la demo, para que el panel los liste.
     *
     * @return productos en orden fijo
     */
    public List<ProductoParaVenta> todos() {
        return PRODUCTOS;
    }
}
