package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/** Agrega una unidad propia al articulo (U-03): {@code nombre} vale {@code factor} unidades base. */
public record DefinirUnidadPropiaComando(UUID articuloId, String nombre, BigDecimal factor) implements Comando {
}
