package mx.negocio360.contabilidad.dominio;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Error de negocio del modulo. El mensaje es solo el codigo. Cuando ayuda a la persona, lleva el id del recurso
 * involucrado (por ejemplo, la poliza que ya fue revertida).
 */
public final class ContabilidadException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final CodigoError codigo;
    private final transient UUID recursoId;

    /**
     * Crea el error sin recurso asociado.
     *
     * @param codigo codigo fijo del error; no puede ser nulo
     */
    public ContabilidadException(CodigoError codigo) {
        this(codigo, null);
    }

    /**
     * Crea el error con el id del recurso involucrado.
     *
     * @param codigo    codigo fijo del error; no puede ser nulo
     * @param recursoId id del recurso involucrado, o nulo
     */
    public ContabilidadException(CodigoError codigo, UUID recursoId) {
        super(Objects.requireNonNull(codigo, "codigo").name());
        this.codigo = codigo;
        this.recursoId = recursoId;
    }

    /**
     * Codigo del error.
     *
     * @return el codigo, nunca nulo
     */
    public CodigoError codigo() {
        return codigo;
    }

    /**
     * Id del recurso involucrado, si lo hay.
     *
     * @return el id, o vacio
     */
    public Optional<UUID> recursoId() {
        return Optional.ofNullable(recursoId);
    }
}
