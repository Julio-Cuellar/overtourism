package mx.negocio360.caja;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.caja.dominio.MovimientoCaja;

/**
 * CF-02: como va la caja de un turno.
 *
 * @param turnoId           turno
 * @param cajaId            caja del turno
 * @param estado            estado efectivo (un corte vencido cuenta como ABIERTO)
 * @param esperadoPorMetodo los tres metodos, escala 2 (CJ-12)
 * @param movimientos       libro del turno, en orden
 * @param ventasAplicadas   ventas cuyos pagos entraron al turno
 */
public record SaldoTurno(UUID turnoId, UUID cajaId, EstadoTurno estado, Map<MetodoPago, BigDecimal> esperadoPorMetodo,
                         List<MovimientoCaja> movimientos, long ventasAplicadas) {

    /** Exige el turno y copia los movimientos para que el saldo sea inmutable. */
    public SaldoTurno {
        Objects.requireNonNull(turnoId, "turnoId");
        movimientos = List.copyOf(movimientos);
    }
}
