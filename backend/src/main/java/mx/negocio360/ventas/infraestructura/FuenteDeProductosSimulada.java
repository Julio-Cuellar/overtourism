package mx.negocio360.ventas.infraestructura;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.catalogo.FuenteDeProductos;
import mx.negocio360.compartido.catalogo.ProductoParaVenta;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Productos <b>simulados</b> para que el panel pueda cobrar mientras Misceláneos y Restaurantes no entreguen los suyos
 * (D-128, F4 del plan de integracion). Solo existe con {@code ventas.productos-simulados=true} y nunca con el perfil
 * {@code produccion}. Son los mismos seis para cualquier negocio y cada nombre lo dice: {@code [SIMULADO]}.
 */
@Component
@Profile("!produccion")
@ConditionalOnProperty(name = "ventas.productos-simulados", havingValue = "true")
public final class FuenteDeProductosSimulada implements FuenteDeProductos {

    private static final Map<UUID, ProductoParaVenta> PRODUCTOS = productos();

    private static Map<UUID, ProductoParaVenta> productos() {
        var todos = List.of(
                producto("0192f000-0000-7000-8000-0000000000b1", "[SIMULADO] Taco de canasta", "pieza", "12.00", false),
                producto("0192f000-0000-7000-8000-0000000000b2", "[SIMULADO] Mole poblano (porción)", "pieza", "95.00",
                        false),
                producto("0192f000-0000-7000-8000-0000000000b3", "[SIMULADO] Camote poblano", "kg", "60.00", true),
                producto("0192f000-0000-7000-8000-0000000000b4", "[SIMULADO] Dulce de tejocote", "pieza", "15.00",
                        false),
                producto("0192f000-0000-7000-8000-0000000000b5", "[SIMULADO] Boleto de museo", "pieza", "45.00", false),
                producto("0192f000-0000-7000-8000-0000000000b6", "[SIMULADO] Amaranto a granel", "kg", "80.00", true));
        var porId = new HashMap<UUID, ProductoParaVenta>();
        todos.forEach(p -> porId.put(p.productoId(), p));
        return Map.copyOf(porId);
    }

    private static ProductoParaVenta producto(String id, String descripcion, String unidad, String precio,
                                              boolean fraccion) {
        return new ProductoParaVenta(UUID.fromString(id), descripcion, unidad, new BigDecimal(precio), fraccion, true);
    }

    @Override
    public Map<UUID, ProductoParaVenta> preciosParaVenta(UUID negocioId, Set<UUID> productoIds) {
        var encontrados = new HashMap<UUID, ProductoParaVenta>();
        for (var id : productoIds) {
            var producto = PRODUCTOS.get(id);
            if (producto != null) {
                encontrados.put(id, producto);
            }
        }
        return Map.copyOf(encontrados);
    }

    @Override
    public List<ProductoParaVenta> buscar(UUID negocioId, String texto, int limite) {
        var parte = texto == null ? "" : texto.strip().toLowerCase(Locale.ROOT);
        return PRODUCTOS.values().stream()
                .filter(p -> p.descripcion().toLowerCase(Locale.ROOT).contains(parte))
                .sorted(java.util.Comparator.comparing(ProductoParaVenta::descripcion))
                .limit(Math.max(0, limite))
                .toList();
    }
}
