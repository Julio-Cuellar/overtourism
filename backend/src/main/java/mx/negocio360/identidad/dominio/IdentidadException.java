package mx.negocio360.identidad.dominio;

import java.util.Objects;

/** Error de negocio de Identidad. El mensaje es solo el codigo: nunca lleva telefono, nombre, codigo ni token. */
public final class IdentidadException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final CodigoError codigo;

    public IdentidadException(CodigoError codigo) {
        super(Objects.requireNonNull(codigo, "codigo").name());
        this.codigo = codigo;
    }

    public CodigoError codigo() {
        return codigo;
    }
}
