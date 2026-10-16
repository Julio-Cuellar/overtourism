package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entrada de mercancia (C-02, C-03, C-08). El costo es por unidad capturada ("180 por kg"). {@code ocurridoEn} nulo
 * es ahora; no puede ser futuro.
 */
public record RegistrarEntradaComando(UUID articuloId, BigDecimal cantidad, String unidad,
                                      BigDecimal costoUnitario, Instant ocurridoEn) implements Comando {
}
