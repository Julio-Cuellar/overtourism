package mx.negocio360.contabilidad.dominio;

import java.time.Instant;
import java.util.UUID;

/**
 * Fila de {@code evento_procesado}: la marca de idempotencia de los listeners (E-01).
 *
 * @param negocioId  negocio del sobre
 * @param eventoId   UUIDv7 del productor
 * @param tipo       tipo del evento
 * @param version    version del contrato
 * @param recibidoEn cuando se proceso
 * @param resultado  aplicado, omitido o pendiente
 * @param motivo     motivo del resultado, o nulo
 */
public record EventoProcesado(UUID negocioId, UUID eventoId, String tipo, int version, Instant recibidoEn,
                              ResultadoEvento.Resultado resultado, String motivo) {
}
