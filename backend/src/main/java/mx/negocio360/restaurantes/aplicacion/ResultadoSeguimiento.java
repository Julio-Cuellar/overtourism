package mx.negocio360.restaurantes.aplicacion;

/** Lo que hizo {@link SeguimientoDeNegocios} con un evento. Nunca lanza por datos malos. */
public enum ResultadoSeguimiento {
    /** El evento cambio la extension del restaurante. */
    APLICADO,
    /** No era para Restaurantes: otro tipo, otro giro, o un negocio que no es restaurante. */
    IGNORADO,
    /** Ese mismo evento ya se habia procesado (R3). */
    YA_PROCESADO,
    /** Llego despues de uno mas nuevo: no se aplica (no hay orden global de eventos). */
    OBSOLETO,
    /** No cumple el contrato v1 o no lo publico Negocios. */
    DATOS_INVALIDOS
}
