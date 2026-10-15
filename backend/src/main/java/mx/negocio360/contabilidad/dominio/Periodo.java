package mx.negocio360.contabilidad.dominio;

import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Mes contable de un negocio (tabla {@code periodo}).
 *
 * @param id          UUIDv7
 * @param negocioId   negocio
 * @param mes         anio y mes; unico por negocio
 * @param estado      abierto o cerrado
 * @param abiertoEn   cuando se abrio
 * @param cerradoEn   cuando se cerro, o nulo
 * @param cerradoPor  quien lo cerro, o nulo
 * @param reaperturas veces que se reabrio (P2)
 * @param version     version para bloqueo optimista
 */
public record Periodo(UUID id, UUID negocioId, YearMonth mes, EstadoPeriodo estado, Instant abiertoEn,
                      Instant cerradoEn, String cerradoPor, int reaperturas, long version) {

    /**
     * Periodo recien abierto.
     *
     * @param id        UUIDv7
     * @param negocioId negocio
     * @param mes       anio y mes
     * @param ahora     momento de apertura
     * @return el periodo en estado ABIERTO
     */
    public static Periodo abierto(UUID id, UUID negocioId, YearMonth mes, Instant ahora) {
        return new Periodo(id, negocioId, mes, EstadoPeriodo.ABIERTO, ahora, null, null, 0, 0);
    }

    /**
     * Dice si el periodo recibe polizas.
     *
     * @return verdadero si esta ABIERTO
     */
    public boolean abierto() {
        return estado == EstadoPeriodo.ABIERTO;
    }
}
