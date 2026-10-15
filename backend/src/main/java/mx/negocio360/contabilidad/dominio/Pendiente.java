package mx.negocio360.contabilidad.dominio;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * Evento que no se pudo contabilizar solo y espera a una persona (tabla {@code pendiente_contable}, E-04).
 *
 * @param id                  UUIDv7
 * @param negocioId           negocio
 * @param eventoId            evento original
 * @param tipoEvento          tipo del evento
 * @param datos               solo los campos del contrato que usa Contabilidad; sin datos personales
 * @param motivo              REGLA_FALTANTE, CUENTA_INACTIVA, DATOS_INVALIDOS o VERSION_NO_SOPORTADA
 * @param fechaEvento         fecha contable del evento, para saber a que mes pertenece (T-03)
 * @param estado              abierto, contabilizado o descartado
 * @param resueltoConPolizaId poliza con que se resolvio, o nula
 * @param motivoResolucion    motivo del descarte, o nulo
 * @param creadoEn            cuando se creo
 * @param resueltoEn          cuando se resolvio, o nulo
 */
public record Pendiente(UUID id, UUID negocioId, UUID eventoId, String tipoEvento, Map<String, Object> datos,
                        String motivo, LocalDate fechaEvento, EstadoPendiente estado, UUID resueltoConPolizaId,
                        String motivoResolucion, Instant creadoEn, Instant resueltoEn) {

    public Pendiente {
        datos = Map.copyOf(datos);
    }

    /**
     * El pendiente resuelto.
     *
     * @param estado           CONTABILIZADO o DESCARTADO
     * @param polizaId         poliza con que se resolvio, o nula
     * @param motivoResolucion motivo del descarte, o nulo
     * @param ahora            momento de la resolucion
     * @return la copia resuelta
     */
    public Pendiente resuelto(EstadoPendiente estado, UUID polizaId, String motivoResolucion, Instant ahora) {
        return new Pendiente(id, negocioId, eventoId, tipoEvento, datos, motivo, fechaEvento, estado, polizaId,
                motivoResolucion, creadoEn, ahora);
    }
}
