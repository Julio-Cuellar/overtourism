package mx.negocio360.caja.aplicacion;

/**
 * Lo que hizo {@link ConsumidorVentas} con un evento (CF-20, CF-21). El adaptador del bus usa {@link #completado()}:
 * si es falso, el evento <b>no</b> se da por entregado y Caja no lo marco como procesado, asi que puede volver a
 * llegar sin perderse (revision A1).
 */
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
    /**
     * El turno no existe en ese negocio o ya cerro. No se aplica ni se marca: el evento queda pendiente hasta que
     * exista CJ-11 (venta tardia como ajuste, P2) o alguien lo revise.
     */
    SIN_TURNO_ABIERTO,
    /**
     * El evento no cumple el contrato v1 (datos que faltan, metodo desconocido, montos no positivos, desmedidos o con
     * mas de 2 decimales) o no lo publico Ventas. No se aplica ni se marca: va a revision, no se reintenta igual.
     */
    DATOS_INVALIDOS,
    /** No es un evento que Caja escuche. */
    IGNORADO;

    /**
     * Dice si el evento quedo atendido y el bus puede darlo por entregado.
     *
     * @return falso para {@link #SIN_TURNO_ABIERTO} y {@link #DATOS_INVALIDOS}
     */
    public boolean completado() {
        return this != SIN_TURNO_ABIERTO && this != DATOS_INVALIDOS;
    }
}
