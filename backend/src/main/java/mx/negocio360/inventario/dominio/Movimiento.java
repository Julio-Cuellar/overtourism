package mx.negocio360.inventario.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Movimiento de inventario (tabla {@code movimiento}). Solo se inserta: nunca se modifica ni se borra; los errores se
 * corrigen con movimientos nuevos (C-07). Guarda lo capturado y el factor aplicado para que la conversion sea
 * auditable (U-05), y el costo aplicado, que no cambia despues (C-04).
 *
 * @param cantidadBase    con signo: positiva si entra, negativa si sale
 * @param costoUnitario   costo por unidad base aplicado al movimiento
 * @param costoTotal      cantidad base por costo unitario, en dinero y con el mismo signo
 * @param causaId         origen externo (por ejemplo una venta); nulo en los movimientos manuales
 * @param motivo          solo en mermas
 * @param ocurridoEn      cuando paso el hecho; puede ser anterior al registro
 */
public record Movimiento(UUID id, UUID negocioId, UUID articuloId, TipoMovimiento tipo, BigDecimal cantidadCapturada,
                         String unidadCapturada, BigDecimal factorAplicado, BigDecimal cantidadBase,
                         BigDecimal costoUnitario, BigDecimal costoTotal, BigDecimal cppResultante,
                         BigDecimal saldoResultante, CausaTipo causaTipo, UUID causaId, MotivoMerma motivo,
                         Instant ocurridoEn, Instant registradoEn, String registradoPor) {

    public Movimiento {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        Objects.requireNonNull(articuloId, "articuloId");
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(cantidadCapturada, "cantidadCapturada");
        Objects.requireNonNull(unidadCapturada, "unidadCapturada");
        Objects.requireNonNull(factorAplicado, "factorAplicado");
        Objects.requireNonNull(cantidadBase, "cantidadBase");
        Objects.requireNonNull(costoUnitario, "costoUnitario");
        Objects.requireNonNull(costoTotal, "costoTotal");
        Objects.requireNonNull(cppResultante, "cppResultante");
        Objects.requireNonNull(saldoResultante, "saldoResultante");
        Objects.requireNonNull(causaTipo, "causaTipo");
        Objects.requireNonNull(ocurridoEn, "ocurridoEn");
        Objects.requireNonNull(registradoEn, "registradoEn");
        Objects.requireNonNull(registradoPor, "registradoPor");
        if (tipo.entra() != (cantidadBase.signum() > 0)) {
            throw new IllegalArgumentException("El signo de la cantidad no corresponde al tipo " + tipo);
        }
    }
}
