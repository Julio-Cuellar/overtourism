package mx.negocio360.negocios;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * El negocio para su panel (G1): perfil, categoria, subcategorias, atributos, disponibilidad, completitud y su sucursal
 * principal. Estado y giro viajan como texto para no exponer los tipos internos del modulo.
 */
public record NegocioVista(UUID id, String nombre, String giro, String giroNombre, String estado,
                           String suspendidoPor, boolean publicado, boolean contactoPublico, String descripcion,
                           String telefonoContacto, String correoContacto, String zonaHoraria,
                           CompletitudVista completitud, SucursalVista principal,
                           List<SubcategoriaVista> subcategorias, AtributosVista atributos,
                           DisponibilidadVista disponibilidad, boolean esSimulado, long version,
                           Instant actualizadoEn) {

    public NegocioVista {
        subcategorias = List.copyOf(subcategorias);
    }

    /** Sin el contacto en el texto: no debe acabar en un registro (SN-06). */
    @Override
    public String toString() {
        return "NegocioVista[id=" + id + ", estado=" + estado + ", version=" + version + "]";
    }
}
