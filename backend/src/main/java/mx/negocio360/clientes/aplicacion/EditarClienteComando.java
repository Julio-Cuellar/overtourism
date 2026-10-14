package mx.negocio360.clientes.aplicacion;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Edicion de un cliente (7.2). Un campo nulo no cambia; un texto vacio en apellidos, telefono, correo o notas lo
 * borra; la fecha de nacimiento solo se puede cambiar, no borrar. Falta la referencia a una solicitud ARCO, que
 * llega con la etapa I5.
 *
 * @param version la version que la persona vio; si ya cambio, la edicion falla con CONCURRENCIA (I-06)
 */
public record EditarClienteComando(UUID clienteId, long version, String nombre, String apellidos, String telefono,
                                   String correo, LocalDate fechaNacimiento, String notas) implements Comando {

    /** Solo identifica la edicion; no imprime los datos personales (P-06). */
    @Override
    public String toString() {
        return "EditarClienteComando[clienteId=" + clienteId + ", version=" + version + "]";
    }
}
