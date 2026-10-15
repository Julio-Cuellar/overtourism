package mx.negocio360.caja.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Una entrada o salida de dinero del turno. Solo se inserta; nunca se edita ni se borra.
 *
 * @param id            UUIDv7
 * @param negocioId     negocio dueño
 * @param turnoId       turno
 * @param tipo          origen del movimiento
 * @param metodo        metodo de pago
 * @param monto         escala 2, con signo: negativo si sale
 * @param ventaId       venta que lo provoco, o nula
 * @param eventoId      evento que lo provoco, o nulo
 * @param nota          nota de la persona, o nula
 * @param registradoEn  UTC
 * @param registradoPor actor, o nulo si lo creo un evento
 */
public record MovimientoCaja(UUID id, UUID negocioId, UUID turnoId, TipoMovimiento tipo, MetodoPago metodo,
                             BigDecimal monto, UUID ventaId, UUID eventoId, String nota, Instant registradoEn,
                             String registradoPor) {

    /** Exige todo salvo venta, evento, nota y actor, que dependen del origen del movimiento. */
    public MovimientoCaja {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(turnoId, "turnoId");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(metodo, "metodo");
        Objects.requireNonNull(monto, "monto");
        Objects.requireNonNull(registradoEn, "registradoEn");
    }
}
