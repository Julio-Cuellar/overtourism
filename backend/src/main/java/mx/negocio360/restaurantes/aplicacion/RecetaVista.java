package mx.negocio360.restaurantes.aplicacion;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Una receta tal como la ve quien tiene {@code restaurantes.receta.ver}: sin ninguna cifra de costo (RC-10). Muestra la
 * version vigente o, si la receta se desactivo, la ultima que tuvo.
 */
public record RecetaVista(UUID platilloId, int version, int rendimiento, boolean activa, Instant vigenteDesde,
                          List<InsumoVista> insumos) {
}
