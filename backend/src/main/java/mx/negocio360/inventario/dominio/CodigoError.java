package mx.negocio360.inventario.dominio;

import org.springframework.modulith.NamedInterface;

/**
 * Codigos de error fijos del modulo (6.5 del diseno). Solo estan los que usa el nucleo; los de lotes y series se
 * agregan con su tramo. {@code NOMBRE_*}, {@code CATEGORIA_INVALIDA}, {@code UNIDAD_PROPIA_INVALIDA},
 * {@code FECHA_FUTURA}, {@code MOTIVO_REQUERIDO} y {@code CONTEO_SIN_DIFERENCIA} no estan en el diseno: los agrega este
 * tramo (D-76). {@code RASTREO_NO_SOPORTADO} (crear un articulo de lote o de serie antes de que existan) tampoco esta
 * en el diseno. {@code CONSUMIDOR_INVALIDO} (un consumidor que no es {@code vinculo} ni {@code restaurantes}) tampoco.
 */
@NamedInterface("api")
public enum CodigoError {
    // Articulo
    ARTICULO_NO_ENCONTRADO,
    ARTICULO_INACTIVO,
    NOMBRE_INVALIDO,
    NOMBRE_DUPLICADO,
    CATEGORIA_INVALIDA,
    RASTREO_NO_SOPORTADO,
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
    CONSUMIDOR_INVALIDO,
    // Preparar y confirmar
    TOKEN_INVALIDO,
    TOKEN_EXPIRADO,
    TOKEN_USADO,
    COMANDO_CAMBIO,
    // Reintentables
    CONCURRENCIA,
    ERROR_TEMPORAL
}
