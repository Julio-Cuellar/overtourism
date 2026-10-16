package mx.negocio360.identidad;

import java.util.UUID;

/** Una persona del equipo de un negocio. El celular siempre va enmascarado (AP-03). {@code toString} no revela datos. */
public record MiembroVista(UUID cuentaId, String nombre, String apellidos, String telefonoEnmascarado, String rol,
                           String estado) {

    @Override
    public String toString() {
        return "MiembroVista[cuentaId=" + cuentaId + ", rol=" + rol + ", estado=" + estado + "]";
    }
}
