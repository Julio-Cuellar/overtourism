package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

/** Articulo con su saldo, tal como lo ve el panel. Todas las cifras las calcula el servidor. */
@NamedInterface("api")
public record ArticuloVista(UUID id, String nombre, String categoria, String unidadBase, BigDecimal minimoBase,
                            Map<String, BigDecimal> unidadesPropias, BigDecimal cantidadBase, BigDecimal cpp,
                            BigDecimal valor, boolean activo, boolean bajoMinimo, long version) {
}
