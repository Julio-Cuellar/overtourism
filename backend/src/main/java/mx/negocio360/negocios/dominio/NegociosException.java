package mx.negocio360.negocios.dominio;

import java.util.List;
import java.util.Objects;

/**
 * Error de negocio con un codigo fijo (seccion 7.8). El mensaje sale del codigo y nunca repite lo que escribio la
 * persona. {@code faltantes} solo lo usa {@link CodigoError#PUBLICACION_INCOMPLETA}, para decir que falta.
 */
public final class NegociosException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final CodigoError codigo;
    private final List<String> faltantes;

    public NegociosException(CodigoError codigo) {
        this(codigo, List.of());
    }

    public NegociosException(CodigoError codigo, List<String> faltantes) {
        super(Objects.requireNonNull(codigo, "codigo").name());
        this.codigo = codigo;
        this.faltantes = List.copyOf(faltantes);
    }

    public CodigoError codigo() {
        return codigo;
    }

    public List<String> faltantes() {
        return faltantes;
    }
}
