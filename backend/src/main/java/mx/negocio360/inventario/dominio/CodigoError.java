package mx.negocio360.inventario.dominio;

/**
 * Codigos de error fijos del modulo (6.5 del diseno). Solo estan los que usa el nucleo; los de lotes y series se
 * agregan con su tramo. {@code NOMBRE_*}, {@code CATEGORIA_INVALIDA}, {@code UNIDAD_PROPIA_INVALIDA},
 * {@code FECHA_FUTURA}, {@code MOTIVO_REQUERIDO} y {@code CONTEO_SIN_DIFERENCIA} no estan en el diseno: los agrega este
 * tramo (D-76).
 */
public enum CodigoError {
    // Articulo
    ARTICULO_NO_ENCONTRADO,
    ARTICULO_INACTIVO,
    NOMBRE_INVALIDO,
    NOMBRE_DUPLICADO,
    CATEGORIA_INVALIDA,
    BAJA_CON_SALDO,
    // Unidades y cantidades
    UNIDAD_NO_CONVERTIBLE,
    UNIDAD_PROPIA_INVALIDA,
    CANTIDAD_INVALIDA,
    COSTO_INVALIDO,
    // Movimientos
    FECHA_FUTURA,
    MOTIVO_REQUERIDO,
    CONTEO_SIN_DIFERENCIA,
    // Preparar y confirmar
    TOKEN_INVALIDO,
    TOKEN_EXPIRADO,
    TOKEN_USADO,
    COMANDO_CAMBIO,
    // Reintentables
    CONCURRENCIA,
    ERROR_TEMPORAL
}
