package mx.negocio360.contabilidad.dominio;

/**
 * Codigos de error fijos del modulo (seccion 7.4 de {@code docs/diseno/contabilidad.md}). Cada uno tiene un texto
 * fijo en espanol de Mexico; la capa que responde no improvisa.
 */
public enum CodigoError {
    // Activacion
    CONTABILIDAD_NO_ACTIVA,
    // Catalogo
    CUENTA_NO_ENCONTRADA,
    CUENTA_NO_AFECTABLE,
    CUENTA_INACTIVA,
    CUENTA_EN_USO,
    CODIGO_CUENTA_DUPLICADO,
    NATURALEZA_INVALIDA,
    NATURALEZA_CON_MOVIMIENTOS,
    // Polizas
    POLIZA_NO_ENCONTRADA,
    POLIZA_DESCUADRADA,
    POLIZA_SIN_RENGLONES,
    MOVIMIENTO_INVALIDO,
    MONTO_INVALIDO,
    POLIZA_YA_REVERTIDA,
    // Periodos
    PERIODO_NO_ENCONTRADO,
    PERIODO_CERRADO,
    PERIODO_ANTERIOR_ABIERTO,
    PENDIENTES_SIN_RESOLVER,
    PENDIENTE_YA_RESUELTO,
    // Reglas y consultas
    REGLA_SUPERPUESTA,
    TASA_INVALIDA,
    RANGO_DEMASIADO_LARGO,
    MOTIVO_REQUERIDO,
    // Preparar y confirmar
    TOKEN_INVALIDO,
    TOKEN_EXPIRADO,
    TOKEN_USADO,
    COMANDO_CAMBIO,
    // Reintentables
    CONCURRENCIA,
    ERROR_TEMPORAL
}
