package mx.negocio360.clientes.dominio;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Error de negocio del modulo. El mensaje es solo el codigo: nunca lleva nombre, telefono ni correo (P-06). Cuando
 * ayuda a la persona, lleva el id del recurso involucrado (por ejemplo, el cliente que ya tenia ese telefono).
 */
public final class ClientesException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final CodigoError codigo;
    private final UUID recursoId;

    public ClientesException(CodigoError codigo) {
        this(codigo, null);
    }

    public ClientesException(CodigoError codigo, UUID recursoId) {
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
