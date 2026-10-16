package mx.negocio360.compartido.seguridad;

/**
 * La persona tiene el permiso, pero la operacion es sensible y falta la confirmacion reforzada (SG-19). Sale como
 * {@code CONFIRMACION_REQUERIDA} para que el cliente sepa que debe iniciar ese flujo. El permiso queda solo para los
 * registros internos.
 */
public final class ConfirmacionRequeridaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String permiso;

    public ConfirmacionRequeridaException(String permiso) {
        super("CONFIRMACION_REQUERIDA");
        this.permiso = permiso;
    }

    public String permiso() {
        return permiso;
    }
}
