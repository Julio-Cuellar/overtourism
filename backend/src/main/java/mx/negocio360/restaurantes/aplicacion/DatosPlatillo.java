package mx.negocio360.restaurantes.aplicacion;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.restaurantes.dominio.EtiquetaPlatillo;
import mx.negocio360.restaurantes.dominio.Temporada;

/**
 * Lo que la persona escribe de un platillo al crearlo o editarlo. El modo de inventario no va aqui: se cambia con su
 * propia operacion (MN-04).
 *
 * @param nombre      de 2 a 80 caracteres
 * @param descripcion hasta 300, o nulo
 * @param precio      precio final al publico
 * @param categoriaId categoria del menu, o nulo
 * @param etiquetas   declaradas, hasta 8
 * @param temporada   rango de mes y dia, o nulo
 */
public record DatosPlatillo(String nombre, String descripcion, BigDecimal precio, UUID categoriaId,
                            Set<EtiquetaPlatillo> etiquetas, Temporada temporada) {
}
