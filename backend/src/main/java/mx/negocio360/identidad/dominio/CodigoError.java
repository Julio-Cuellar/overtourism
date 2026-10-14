package mx.negocio360.identidad.dominio;

/**
 * Codigos de error del modulo de Identidad. Cada uno tiene un texto fijo en español de Mexico (ver
 * {@code PlantillasIdentidad}) y un estado HTTP en la capa REST. Los errores de autenticacion no distinguen la causa
 * (SI-14): un token desconocido, vencido o revocado es siempre {@link #NO_AUTENTICADO}.
 */
public enum CodigoError {
    TELEFONO_INVALIDO,
    TASA_EXCEDIDA,
    CODIGO_INVALIDO,
    CODIGO_EXPIRADO,
    CODIGO_AGOTADO,
    NOMBRE_REQUERIDO,
    NOMBRE_INVALIDO,
    NO_AUTENTICADO,
    ERROR_TEMPORAL,
    SOLICITUD_INVALIDA,
    ERROR_INTERNO
}
