package mx.negocio360.identidad;

import java.util.UUID;

/** Lo que la propia persona ve de su cuenta (AP-03). {@code toString} no revela datos personales. */
public record CuentaVista(UUID id, String nombre, String apellidos, String telefono, String correo) {

    @Override
    public String toString() {
        return "CuentaVista[id=" + id + "]";
    }
}
