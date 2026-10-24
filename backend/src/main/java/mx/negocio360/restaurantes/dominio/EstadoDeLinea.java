package mx.negocio360.restaurantes.dominio;

/**
 * Los estados de una linea (CT-03). Las del personal nacen {@code PENDIENTE} y se quitan libremente; las del QR nacen
 * {@code ENVIADA}. Anular una enviada exige motivo.
 */
public enum EstadoDeLinea {
    PENDIENTE,
    ENVIADA,
    ANULADA
}
