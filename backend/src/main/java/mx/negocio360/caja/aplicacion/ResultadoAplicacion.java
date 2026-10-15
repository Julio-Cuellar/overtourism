package mx.negocio360.caja.aplicacion;

/** Lo que hizo {@link ConsumidorVentas} con un evento (CF-20, CF-21). */
public enum ResultadoAplicacion {
    /** Los pagos de la venta entraron al turno. */
    APLICADA,
    /** Ese mismo evento ya se habia procesado (R3). */
    YA_PROCESADO,
    /** La misma venta ya se habia aplicado con otro evento. */
    YA_APLICADA,
    /** La venta llego despues de su anulacion: no suma (D-19). */
    OMITIDA_ANULADO_PREVIO,
    /** La anulacion compenso la venta. */
    REVERTIDA,
    /** La venta ya estaba revertida o marcada. */
    YA_REVERTIDA,
    /** La anulacion llego antes que la venta: queda la marca. */
    MARCADA_ANULADO_PREVIO,
    /** El turno no existe en ese negocio o ya cerro; pendiente CJ-11 (P2). */
    SIN_TURNO_ABIERTO,
    /** No es un evento que Caja escuche. */
    IGNORADO
}
