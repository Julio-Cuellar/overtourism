package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

/**
 * Una linea de {@code registrarConsumo}: cuanto de un articulo se gasto, en cualquier unidad que se convierta a la base.
 * Los numeros de serie (K-05) llegan con su tramo y no se aceptan todavia.
 *
 * @param articuloId articulo del que se consume
 * @param cantidad   cantidad positiva, con hasta 4 decimales
 * @param unidad     unidad de la cantidad: estandar de su dimension o propia del articulo
 */
@NamedInterface("api")
public record LineaDeConsumo(UUID articuloId, BigDecimal cantidad, String unidad) {
}
