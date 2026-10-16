package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;
import mx.negocio360.inventario.dominio.Rastreo;
import org.springframework.modulith.NamedInterface;

/**
 * Lo que un modulo vertical necesita saber de un articulo para armar y costear una receta (R-I1 y R-I3): sin saldo ni
 * minimos, solo identidad, unidad base, costo, si sigue activo y como se rastrea (D-27). Las cifras las calcula el
 * servidor.
 *
 * @param id         id del articulo
 * @param nombre     nombre para mostrar
 * @param unidadBase codigo de la unidad en la que Inventario lleva el articulo
 * @param cpp        costo promedio ponderado por unidad base; cero si todavia no tiene costo
 * @param activo     falso si lo dieron de baja (A-04)
 * @param rastreo    como se rastrea (L-01); un articulo de serie no puede ser insumo de una receta (S-05)
 */
@NamedInterface("api")
public record ArticuloBase(UUID id, String nombre, String unidadBase, BigDecimal cpp, boolean activo,
                           Rastreo rastreo) {
}
