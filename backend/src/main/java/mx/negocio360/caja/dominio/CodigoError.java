package mx.negocio360.caja.dominio;

/**
 * Codigos de error fijos de Caja ({@code docs/diseno/caja.md}, seccion 5.4). Cada codigo dice la regla que lo
 * produce (D-142). {@code CAJA_DUPLICADA}, {@code NOMBRE_CAJA_INVALIDO} y {@code TURNO_NO_ENCONTRADO} se agregaron al
 * programar y estan anotados en la Bitacora.
 */
public enum CodigoError {
    // Caja y turno
    /** La caja no existe en este negocio. */
    CAJA_NO_ENCONTRADA("CF-10"),
    /** La caja esta desactivada. */
    CAJA_INACTIVA("CF-10"),
    /** Ya hay una caja con ese nombre en el negocio. */
    CAJA_DUPLICADA("CF-14"),
    /** Nombre vacio o de mas de 60 caracteres. */
    NOMBRE_CAJA_INVALIDO("CF-14"),
    /** El negocio ya tiene 50 cajas. */
    DEMASIADAS_CAJAS("CF-14"),
    /** El turno no existe en este negocio. */
    TURNO_NO_ENCONTRADO("CF-02"),
    /** La caja ya tiene un turno abierto o en corte. */
    TURNO_YA_ABIERTO("CJ-01"),
    /** El turno no existe o ya esta cerrado. */
    TURNO_NO_ABIERTO("CJ-09"),
    /** El turno esta en corte y no acepta movimientos. */
    TURNO_EN_CORTE("CJ-10"),
    /** Fondo negativo, desmedido o con mas de 2 decimales. */
    FONDO_INVALIDO("CJ-02"),
    // Movimientos
    /** Monto nulo, cero, negativo, desmedido o con mas de 2 decimales. */
    MONTO_INVALIDO("CF-11"),
    /** El retiro dejaria el efectivo esperado bajo cero. */
    RETIRO_MAYOR_AL_EFECTIVO("CJ-05"),
    /** Falta la nota del movimiento o pasa de 200 caracteres. */
    NOTA_REQUERIDA("CF-11"),
    // Consultas
    /** Tamano de pagina fuera de 1 a 100, o cursor que no emitimos nosotros. */
    PAGINACION_INVALIDA("CF-03"),
    /** Filtro de cortes con rango de mas de 92 dias o al reves. */
    FILTRO_INVALIDO("CF-03"),
    // Corte
    /** Hay ventas preparadas sin confirmar en el turno. */
    CORTE_VENTAS_PENDIENTES("CJ-07"),
    /** Caja aun no aplica todos los eventos de venta del turno. */
    CORTE_EVENTOS_PENDIENTES("CJ-07"),
    /** La diferencia supera el umbral y falta la autorizacion de un encargado. */
    CORTE_REQUIERE_AUTORIZACION("CJ-08"),
    /** Conteo ausente, negativo, desmedido o con mas de 2 decimales. */
    CONTEO_INVALIDO("CJ-12"),
    // Preparar y confirmar
    /** Token vacio, desconocido o ajeno. */
    TOKEN_INVALIDO("D-23"),
    /** Token vencido. */
    TOKEN_EXPIRADO("D-23"),
    /** Token ya usado. */
    TOKEN_USADO("D-23"),
    /** Lo preparado ya no coincide con el mundo (por ejemplo llego una venta) o el comando se altero. */
    COMANDO_CAMBIO("D-23"),
    // Reintentables
    /** Otra peticion cambio el turno antes. */
    CONCURRENCIA("CJ-09"),
    /** Falla temporal. */
    ERROR_TEMPORAL("CJ-07");

    private final String regla;

    CodigoError(String regla) {
        this.regla = regla;
    }

    /**
     * Regla del diseno que produce este codigo (D-142).
     *
     * @return regla o funcion, por ejemplo {@code CJ-07}
     */
    public String regla() {
        return regla;
    }
}
