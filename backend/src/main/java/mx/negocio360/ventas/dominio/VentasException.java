package mx.negocio360.ventas.dominio;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Error de negocio de Ventas. El mensaje es el codigo y la regla que lo produce (por ejemplo
 * {@code PAGOS_NO_CUADRAN (VT-04)}); nunca lleva montos ni datos de la persona. Cuando ayuda, lleva el id del
 * recurso involucrado (por ejemplo el producto que Catalogo no conoce).
 */
public final class VentasException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Codigo fijo del error. */
    private final CodigoError codigo;
    /** Producto, venta o cupon involucrado, o nulo. */
    private final UUID recursoId;

    /**
     * Error sin recurso asociado.
     *
     * @param codigo codigo fijo del error
     */
    public VentasException(CodigoError codigo) {
        this(codigo, null);
    }

    /**
     * Error con el id del recurso involucrado.
     *
     * @param codigo    codigo fijo del error
     * @param recursoId id del producto, venta o cupon, o nulo
     */
    public VentasException(CodigoError codigo, UUID recursoId) {
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
     * @return id del producto, venta o cupon, si lo hay
     */
    public Optional<UUID> recursoId() {
        return Optional.ofNullable(recursoId);
    }
}
