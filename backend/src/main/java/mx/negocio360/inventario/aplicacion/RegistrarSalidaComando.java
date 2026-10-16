package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/** Salida por consumo propio o venta no registrada. Se valua al CPP vigente (C-04) y puede dejar saldo negativo. */
public record RegistrarSalidaComando(UUID articuloId, BigDecimal cantidad, String unidad) implements Comando {
}
