package mx.negocio360.clientes.aplicacion;

import java.util.UUID;

/**
 * Cliente tal como aparece en listas y sugerencias: solo id, nombre y telefono enmascarado. No lleva correo ni fecha
 * de nacimiento (P-05, SC-02); la ficha completa sale unicamente de la consulta por id, que deja huella de acceso.
 */
public record ClienteListado(UUID id, String nombreCompleto, String telefonoEnmascarado) {

    @Override
    public String toString() {
        return "ClienteListado[id=" + id + "]";
    }
}
