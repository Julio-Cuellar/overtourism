package mx.negocio360.inventario.aplicacion;

import java.util.UUID;

/** Baja logica de un articulo; solo con saldo cero (A-04). */
public record BajaArticuloComando(UUID articuloId) implements Comando {
}
