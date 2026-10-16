package mx.negocio360.negocios;

/**
 * Cambios al perfil del negocio (G10). {@code version} es la que la persona vio (NG-10). Un campo nulo no cambia; un
 * texto vacio borra el campo opcional. No admite giro, estado, publicacion ni la marca de simulado (NG-13, DM-01).
 */
public record ActualizarNegocio(long version, String nombre, String descripcion, String telefonoContacto,
                                String correoContacto, Boolean contactoPublico) {

    /** Sin el contacto en el texto (SN-06). */
    @Override
    public String toString() {
        return "ActualizarNegocio[version=" + version + "]";
    }
}
