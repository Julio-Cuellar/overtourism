package mx.negocio360.caja.aplicacion;

import java.math.BigDecimal;
import java.util.UUID;
import mx.negocio360.compartido.comandos.Comando;

/**
 * CF-11 RegistrarMovimientoCaja: entrada o retiro de efectivo (CJ-05, CJ-10).
 *
 * @param turnoId turno ABIERTO
 * @param tipo    entrada o retiro
 * @param monto   mayor que 0, hasta 2 decimales
 * @param nota    por que, de 1 a 200 caracteres
 */
public record RegistrarMovimientoCajaComando(UUID turnoId, TipoMovimientoManual tipo, BigDecimal monto, String nota)
        implements Comando {
}
