package mx.negocio360.compartido.seguridad;

import java.util.Objects;

/**
 * La operacion pide una sesion que no hay (SG-01): {@code NO_AUTENTICADO} si no hay ninguna y {@code SIN_NEGOCIO_ACTIVO}
 * si hay sesion pero todavia no eligio negocio. El mensaje es fijo: no dice nada de la cuenta ni del token.
 */
public final class NoAutenticadoException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String NO_AUTENTICADO = "NO_AUTENTICADO";
    public static final String SIN_NEGOCIO_ACTIVO = "SIN_NEGOCIO_ACTIVO";

    private final String codigo;

    public NoAutenticadoException(String codigo) {
        super(Objects.requireNonNull(codigo, "codigo"));
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }
}
