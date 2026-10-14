package mx.negocio360.identidad;

import java.util.UUID;

/** Lo que la propia persona ve de su cuenta (AP-03). {@code toString} no revela nombre ni telefono. */
public record CuentaVista(UUID id, String nombre, String telefono) {

    @Override
    public String toString() {
        return "CuentaVista[id=" + id + "]";
    }
}
