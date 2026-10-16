package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.restaurantes.dominio.EtiquetaPlatillo;
import mx.negocio360.restaurantes.dominio.ModoInventario;
import mx.negocio360.restaurantes.dominio.Platillo;

/**
 * Un platillo tal como lo ve el panel. Las cifras y los estados los calcula el servidor; {@code ordenableHoy} y
 * {@code tieneReceta} no se guardan.
 */
public record PlatilloVista(UUID id, String nombre, String descripcion, BigDecimal precio, UUID categoriaId,
                            ModoInventario modo, boolean agotado, String temporadaDesde, String temporadaHasta,
                            boolean publicado, boolean activo, Set<EtiquetaPlatillo> etiquetas, boolean tieneReceta,
                            boolean ordenableHoy, long version) {

    /** Vista de un platillo en el dia {@code hoy}. */
    public static PlatilloVista de(Platillo p, boolean tieneReceta, LocalDate hoy) {
        return new PlatilloVista(p.id(), p.nombre(), p.descripcion(), p.precio(), p.categoriaId(), p.modo(), p.agotado(),
                p.temporada() == null ? null : p.temporada().desdeComoTexto(),
                p.temporada() == null ? null : p.temporada().hastaComoTexto(), p.publicado(), p.activo(), p.etiquetas(),
                tieneReceta, p.ordenable(hoy), p.version());
    }
}
