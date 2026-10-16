package mx.negocio360.identidad.aplicacion.puertos;

/** El canal de codigos no pudo enviar el mensaje. El mensaje nunca debe llevar el telefono ni el codigo. */
public final class CanalNoDisponibleException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public CanalNoDisponibleException(String mensaje) {
        super(mensaje);
    }
}
