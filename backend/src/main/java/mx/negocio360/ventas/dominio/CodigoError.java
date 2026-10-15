package mx.negocio360.ventas.dominio;

/**
 * Codigos de error fijos de Ventas ({@code docs/diseno/ventas.md}, seccion 5.3). Cada codigo dice la regla que lo
 * produce, para ir de un error en el registro a su regla en el diseno (D-83).
 */
public enum CodigoError {
    // Datos de la venta
    /** No hay lineas que vender. */
    VENTA_SIN_LINEAS("VT-03"),
    /** Cantidad nula, cero, negativa, con mas de 4 decimales o desmedida. */
    CANTIDAD_INVALIDA("VT-03"),
    /** Cantidad con fraccion en un producto que no la admite. */
    CANTIDAD_NO_FRACCIONABLE("VT-03"),
    /** Catalogo no conoce el producto en este negocio. */
    PRODUCTO_NO_ENCONTRADO("VT-02"),
    /** El producto existe pero no se vende. */
    PRODUCTO_INACTIVO("VT-02"),
    // Pagos
    /** La suma de pagos no es el total, o no hay pagos. */
    PAGOS_NO_CUADRAN("VT-04"),
    /** El efectivo recibido es menor que el monto de ese pago. */
    EFECTIVO_INSUFICIENTE("VT-04"),
    /** Pago sin metodo, con monto no positivo o con mas de 2 decimales, o con recibido fuera del efectivo. */
    METODO_PAGO_INVALIDO("VT-04"),
    // Cupon
    /** Pasaporte no reconoce el codigo. */
    CUPON_INVALIDO("VT-07"),
    /** El cupon ya se canjeo. */
    CUPON_YA_CANJEADO("VT-07"),
    /** El cupon no aplica a esta venta. */
    CUPON_NO_APLICA("VT-07"),
    /** El descuento supera el subtotal. */
    DESCUENTO_MAYOR_AL_SUBTOTAL("VT-08"),
    // Turno
    /** La venta no trae turno (el canal valida que este ABIERTO). */
    TURNO_NO_ABIERTO("VT-01"),
    // Anulacion
    /** La venta no existe en este negocio. */
    VENTA_NO_ENCONTRADA("VT-09"),
    /** La venta ya estaba anulada. */
    VENTA_YA_ANULADA("VT-09"),
    /** Falta el motivo de anulacion o pasa de 200 caracteres. */
    MOTIVO_REQUERIDO("VT-09"),
    /** El actor no tiene permiso de anular. */
    SIN_PERMISO_ANULAR("VT-09"),
    // Preparar y confirmar
    /** Token vacio, desconocido o ajeno. */
    TOKEN_INVALIDO("D-23"),
    /** Token vencido. */
    TOKEN_EXPIRADO("D-23"),
    /** Token ya usado. */
    TOKEN_USADO("D-23"),
    /** Lo preparado ya no coincide con el mundo (precio, cupon) o el comando se altero. */
    COMANDO_CAMBIO("VT-05"),
    // Reintentables
    /** Otra peticion cambio la venta antes. */
    CONCURRENCIA("VT-11"),
    /** Falla temporal de un modulo vecino (por ejemplo Pasaporte). */
    ERROR_TEMPORAL("VT-07");

    private final String regla;

    CodigoError(String regla) {
        this.regla = regla;
    }

    /**
     * Regla del diseno que produce este codigo.
     *
     * @return identificador de la regla, por ejemplo {@code VT-04}
     */
    public String regla() {
        return regla;
    }

    /**
     * Dice si el llamante puede reintentar.
     *
     * @return verdadero para {@link #CONCURRENCIA} y {@link #ERROR_TEMPORAL}
     */
    public boolean reintentable() {
        return this == CONCURRENCIA || this == ERROR_TEMPORAL;
    }
}
