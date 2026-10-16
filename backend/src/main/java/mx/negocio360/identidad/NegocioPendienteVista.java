package mx.negocio360.identidad;

import java.util.UUID;

/**
 * Un negocio que la persona pidio y que el modulo de negocios todavia no registra (D-79). Por ahora el unico estado es
 * PENDIENTE; un rechazo del modulo de negocios agregaria otro.
 */
public record NegocioPendienteVista(UUID id, String nombre, String giro, String estado) {

    public static final String PENDIENTE = "PENDIENTE";

    @Override
    public String toString() {
        return "NegocioPendienteVista[id=" + id + ", estado=" + estado + "]";
    }
}
