package mx.negocio360.caja.dominio;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Error de negocio de Caja. El mensaje es el codigo y su regla (por ejemplo
 * {@code CORTE_EVENTOS_PENDIENTES (CJ-07)}); nunca lleva montos ni datos de la persona.
 */
public final class CajaException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Codigo fijo del error. */
    private final CodigoError codigo;
    /** Caja o turno involucrado, o nulo. */
    private final UUID recursoId;

    /**
     * Error sin recurso asociado.
     *
     * @param codigo codigo fijo del error
     */
    public CajaException(CodigoError codigo) {
        this(codigo, null);
    }

    /**
     * Error con el id del recurso involucrado.
     *
     * @param codigo    codigo fijo del error
     * @param recursoId id de la caja o del turno, o nulo
     */
    public CajaException(CodigoError codigo, UUID recursoId) {
        super(Objects.requireNonNull(codigo, "codigo").name() + " (" + codigo.regla() + ")");
        this.codigo = codigo;
        this.recursoId = recursoId;
    }

    /**
     * Codigo del error, para elegir la plantilla de texto.
     *
     * @return codigo fijo del error
     */
    public CodigoError codigo() {
        return codigo;
    }

    /**
     * Recurso involucrado, cuando ayuda a la persona.
     *
     * @return id de la caja o del turno, si lo hay
     */
    public Optional<UUID> recursoId() {
        return Optional.ofNullable(recursoId);
    }
}
