package mx.negocio360.restaurantes.dominio;

/**
 * Como atiende el restaurante (MS-08): con mesas, solo en mostrador (para llevar, la cuenta no lleva mesa) o las dos
 * cosas.
 */
public enum ModoServicio {
    MESAS,
    MOSTRADOR,
    AMBOS
}
