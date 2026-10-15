package mx.negocio360.contabilidad.dominio;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Poliza registrada (tablas {@code poliza} y {@code movimiento}). Es inmutable: se corrige con una reversa (A-03).
 *
 * @param id               UUIDv7
 * @param negocioId        negocio
 * @param periodoId        periodo en que se registro
 * @param tipo             tipo de poliza
 * @param folio            consecutivo por negocio, periodo y tipo (A-05)
 * @param fecha            fecha contable
 * @param concepto         descripcion
 * @param origen           automatica, manual o reversa
 * @param eventoId         evento que la origino, o nulo si es manual
 * @param referenciaTipo   VENTA, CANJE, CORTE o PENDIENTE, o nulo
 * @param referenciaId     id de la referencia, o nulo
 * @param extemporanea     si vino de un periodo ya cerrado (T-05)
 * @param reviertePolizaId poliza que revierte, o nulo
 * @param total            suma de cargos
 * @param renglones        renglones, en orden
 * @param creadoPor        actor o {@code sistema}
 * @param creadoEn         momento del registro
 */
public record Poliza(UUID id, UUID negocioId, UUID periodoId, TipoPoliza tipo, int folio, LocalDate fecha,
                     String concepto, OrigenPoliza origen, UUID eventoId, String referenciaTipo, UUID referenciaId,
                     boolean extemporanea, UUID reviertePolizaId, BigDecimal total, List<Renglon> renglones,
                     String creadoPor, Instant creadoEn) {

    public Poliza {
        renglones = List.copyOf(renglones);
    }
}
