package mx.negocio360.caja.dominio;

/**
 * Ciclo de vida de un turno: ABIERTO → EN_CORTE → CERRADO. EN_CORTE vuelve a ABIERTO si el corte se descarta o vence
 * sin confirmarse (D-140, CF-13).
 */
public enum EstadoTurno {
    /** Acepta ventas y movimientos. */
    ABIERTO,
    /** Corte preparado: no acepta ventas nuevas ni movimientos manuales (CJ-06, CJ-10). */
    EN_CORTE,
    /** Corte confirmado; el turno ya no cambia (CJ-09). */
    CERRADO
}
