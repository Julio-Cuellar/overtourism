package mx.negocio360.compartido.seguridad;

/**
 * La persona no tiene el permiso que pide la operacion (AZ-01). La capa REST o GraphQL la convierte en 403. El mensaje
 * es fijo y no dice que permiso falto: quien pide no debe aprender el catalogo; el permiso queda para los registros
 * internos.
 */
public final class PermisoDenegadoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String permiso;

    public PermisoDenegadoException(String permiso) {
        super("PERMISO_DENEGADO");
        this.permiso = permiso;
    }

    public String permiso() {
        return permiso;
    }
}
