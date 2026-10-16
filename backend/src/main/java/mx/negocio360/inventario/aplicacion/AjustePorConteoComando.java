package mx.negocio360.inventario.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Ajuste por conteo (A-03, C-06): la persona captura lo contado y el sistema calcula la diferencia contra el saldo.
 * La diferencia entra o sale al CPP vigente.
 */
public record AjustePorConteoComando(UUID articuloId, BigDecimal cantidadContada, String unidad)
        implements Comando {
}
