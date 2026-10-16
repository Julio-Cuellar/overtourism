package mx.negocio360.inventario.dominio;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.modulith.NamedInterface;

/**
 * Error de negocio del modulo. El mensaje es solo el codigo. Cuando ayuda a la persona, lleva el id del recurso
 * involucrado (por ejemplo, el articulo que ya tenia ese nombre).
 */
@NamedInterface("api")
public final class InventarioException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final CodigoError codigo;
    private final UUID recursoId;

    public InventarioException(CodigoError codigo) {
        this(codigo, null);
    }

    public InventarioException(CodigoError codigo, UUID recursoId) {
        super(Objects.requireNonNull(codigo, "codigo").name());
        this.codigo = codigo;
        this.recursoId = recursoId;
    }

    public CodigoError codigo() {
        return codigo;
    }

    public Optional<UUID> recursoId() {
        return Optional.ofNullable(recursoId);
    }
}
