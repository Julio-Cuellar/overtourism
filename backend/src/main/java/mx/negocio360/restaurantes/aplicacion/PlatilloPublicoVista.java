package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.restaurantes.dominio.EtiquetaPlatillo;

/**
 * Un platillo en el menu publico (PB-03): solo los campos permitidos. Nunca lleva costos, recetas, modo de inventario ni
 * estados internos. Las etiquetas son declaradas por el negocio, no verificadas.
 *
 * @param traducido falso si el texto esta en español porque no hay traduccion al idioma pedido (PB-02)
 */
public record PlatilloPublicoVista(UUID id, String nombre, String descripcion, BigDecimal precio,
                                   Set<EtiquetaPlatillo> etiquetas, boolean agotado, boolean traducido) {
}
