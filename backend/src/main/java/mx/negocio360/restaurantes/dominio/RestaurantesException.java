package mx.negocio360.restaurantes.dominio;

import java.util.Objects;

/** Un error de negocio de Restaurantes. El mensaje es el texto fijo del codigo y nunca incluye datos de la peticion. */
public final class RestaurantesException extends RuntimeException {

    private final CodigoError codigo;

    /** @param codigo que fallo */
    public RestaurantesException(CodigoError codigo) {
        super(Objects.requireNonNull(codigo, "codigo").mensaje());
        this.codigo = codigo;
    }

    /** Codigo estable del error. */
    public CodigoError codigo() {
        return codigo;
    }
}
