package mx.negocio360.ventas.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

/**
 * Renglon que pide el panel. El precio no viene de la persona: lo pone Catalogo al preparar (VT-02).
 *
 * @param productoId producto de Catalogo
 * @param cantidad   mayor que 0, hasta 4 decimales; con fraccion solo si el producto lo admite (VT-03)
 * @param serie      numero de serie del articulo, o nulo
 */
@NamedInterface("comandos")
public record LineaSolicitada(UUID productoId, BigDecimal cantidad, String serie) {
}
