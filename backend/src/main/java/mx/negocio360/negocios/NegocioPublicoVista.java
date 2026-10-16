package mx.negocio360.negocios;

import java.util.List;
import java.util.UUID;

/**
 * La ficha publica del negocio (T2): lista permitida de campos (PU-03). Nunca lleva empleados, notas, ids de cuenta ni
 * el contacto del dueño; el telefono y el correo del negocio solo salen si el dueño los marco publicos (PU-04).
 */
public record NegocioPublicoVista(UUID id, String nombre, String giro, String giroNombre, String descripcion,
                                  UbicacionPublicaVista ubicacion, HorarioPublicoVista horario, String telefono,
                                  String correo, List<SubcategoriaVista> subcategorias, AtributosVista atributos,
                                  DisponibilidadPublicaVista disponibilidad, CalificacionVista calificacion,
                                  Double distanciaKm, boolean simulado) {

    public NegocioPublicoVista {
        subcategorias = List.copyOf(subcategorias);
    }

    /** Sin el contacto en el texto (SN-06). */
    @Override
    public String toString() {
        return "NegocioPublicoVista[id=" + id + "]";
    }
}
