package mx.negocio360.restaurantes.dominio;

/**
 * Por que se anula una linea o se cancela una cuenta (QR-10). El servidor compone el mensaje que ve el comensal a partir
 * del codigo; el texto libre solo acompana a {@link #OTRO}.
 */
public enum MotivoDeAnulacion {
    AGOTADO("Cancelado: agotado"),
    ERROR_DEL_PEDIDO("Cancelado: error en el pedido"),
    SIN_COMENSALES("Cancelado: no había comensales"),
    COCINA_NO_PUEDE("Cancelado: la cocina no puede prepararlo"),
    OTRO("Cancelado");

    private final String mensaje;

    MotivoDeAnulacion(String mensaje) {
        this.mensaje = mensaje;
    }

    /** El texto fijo que ve el comensal. */
    public String mensaje() {
        return mensaje;
    }
}
