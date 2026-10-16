package mx.negocio360.ventas;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * VF-03: lo que Caja necesita de Ventas para el corte (CJ-06, CJ-07). Vive en el paquete base de {@code ventas}
 * porque lo consume otro modulo: es parte de la API publica.
 *
 * @param turnoId               turno consultado
 * @param ventasRegistradas     ventas que siguen REGISTRADAS
 * @param ventasAnuladas        ventas ANULADAS de ese turno
 * @param totalesPorMetodo      suma de pagos de las ventas registradas, por metodo ({@code EFECTIVO},
 *                              {@code TARJETA}, {@code TRANSFERENCIA}), escala 2; los metodos sin pagos no aparecen
 * @param preparacionesVigentes ventas preparadas sin confirmar ni vencer en ese turno
 */
public record ResumenTurno(UUID turnoId, int ventasRegistradas, int ventasAnuladas,
                           Map<String, BigDecimal> totalesPorMetodo, long preparacionesVigentes) {

    /** Exige el turno y copia los totales para que el resumen sea inmutable. */
    public ResumenTurno {
        Objects.requireNonNull(turnoId, "turnoId");
        totalesPorMetodo = Map.copyOf(totalesPorMetodo);
    }
}
