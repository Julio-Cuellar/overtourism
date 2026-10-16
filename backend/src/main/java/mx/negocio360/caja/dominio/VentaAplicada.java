package mx.negocio360.caja.dominio;

import java.util.Objects;
import java.util.UUID;

/**
 * Fila de {@code venta_aplicada}: idempotencia por venta y marca de anulacion previa (CJ-04).
 *
 * @param negocioId negocio dueño
 * @param ventaId   venta de Ventas (referencia opaca)
 * @param turnoId   turno de la venta
 * @param estado    que se hizo con ella
 */
public record VentaAplicada(UUID negocioId, UUID ventaId, UUID turnoId, EstadoVentaAplicada estado) {

    /** Exige negocio, venta y estado; el turno puede faltar en una marca de anulacion previa. */
    public VentaAplicada {
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(ventaId, "ventaId");
        Objects.requireNonNull(estado, "estado");
    }
}
