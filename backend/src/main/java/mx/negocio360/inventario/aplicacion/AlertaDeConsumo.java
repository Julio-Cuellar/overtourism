package mx.negocio360.inventario.aplicacion;

import java.util.UUID;
import org.springframework.modulith.NamedInterface;

/**
 * Una alerta de saldo que disparo el consumo (A-02).
 *
 * @param articuloId articulo cuyo saldo cruzo el umbral
 * @param tipo       uno de {@code inventario.bajo_minimo}, {@code inventario.repuesto} o {@code inventario.saldo_negativo}
 */
@NamedInterface("api")
public record AlertaDeConsumo(UUID articuloId, String tipo) {
}
