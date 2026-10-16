package mx.negocio360.identidad;

import java.util.UUID;

/** Un negocio donde la cuenta tiene membresia, con su rol. Se llena desde IDN-2; hoy siempre llega vacio. */
public record NegocioVista(UUID id, String nombre, String rol) {

    @Override
    public String toString() {
        return "NegocioVista[id=" + id + "]";
    }
}
