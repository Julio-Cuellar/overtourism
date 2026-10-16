package mx.negocio360.negocios.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * El negocio (seccion 5.2). Su {@code id} lo asigna Identidad y viaja en el sobre del evento de alta (D-89). Es un
 * registro inmutable: cada cambio devuelve una copia con {@code version} sin tocar, que el repositorio incrementa al
 * guardar con el bloqueo optimista (NG-10).
 *
 * @param duracionVisitaMin cuanto suele durar una visita (declarada, AT-03), o nulo
 * @param costoMedioMxn     gasto medio por persona en pesos (declarado, AT-03), o nulo
 * @param esSimulado        negocio de demostracion (DM-01): solo lo escribe la siembra
 */
public record Negocio(UUID id, String nombre, String giro, EstadoNegocio estado, OrigenSuspension suspendidoPor,
                      boolean publicado, String descripcion, String telefonoContacto, String correoContacto,
                      boolean contactoPublico, String zonaHoraria, UUID logoArchivoId, UUID solicitadoPor,
                      Integer duracionVisitaMin, Integer costoMedioMxn, boolean esSimulado, String motivoEstado,
                      Instant creadoEn, Instant actualizadoEn, Instant cerradoEn, long version) {

    public static final String ZONA_POR_OMISION = "America/Mexico_City";

    public Negocio {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nombre, "nombre");
        Objects.requireNonNull(giro, "giro");
        Objects.requireNonNull(estado, "estado");
        Objects.requireNonNull(zonaHoraria, "zonaHoraria");
        Objects.requireNonNull(solicitadoPor, "solicitadoPor");
        Objects.requireNonNull(creadoEn, "creadoEn");
        Objects.requireNonNull(actualizadoEn, "actualizadoEn");
    }

    /** Un negocio recien dado de alta: activo, sin publicar y con el contacto en privado (AL-01, AL-05, PU-04). */
    public static Negocio nuevo(UUID id, NombreNegocio nombre, String giro, UUID solicitadoPor, Instant ahora) {
        return new Negocio(id, nombre.valor(), giro, EstadoNegocio.ACTIVO, null, false, null, null, null, false,
                ZONA_POR_OMISION, null, solicitadoPor, null, null, false, null, ahora, ahora, null, 0);
    }

    /** Un negocio cerrado no se edita (NG-05) y uno suspendido solo admite lectura y su reanudacion (NG-14). */
    public void exigirEditable() {
        if (estado == EstadoNegocio.CERRADO) {
            throw new NegociosException(CodigoError.NEGOCIO_CERRADO);
        }
        if (estado == EstadoNegocio.SUSPENDIDO) {
            throw new NegociosException(CodigoError.NEGOCIO_SUSPENDIDO);
        }
    }

    public Negocio conPerfil(String nuevoNombre, String nuevaDescripcion, String telefono, String correo,
                             boolean nuevoContactoPublico, Instant ahora) {
        return new Negocio(id, nuevoNombre, giro, estado, suspendidoPor, publicado, nuevaDescripcion, telefono, correo,
                nuevoContactoPublico, zonaHoraria, logoArchivoId, solicitadoPor, duracionVisitaMin, costoMedioMxn,
                esSimulado, motivoEstado, creadoEn, ahora, cerradoEn, version);
    }

    public Negocio conPublicado(boolean nuevoPublicado, Instant ahora) {
        return new Negocio(id, nombre, giro, estado, suspendidoPor, nuevoPublicado, descripcion, telefonoContacto,
                correoContacto, contactoPublico, zonaHoraria, logoArchivoId, solicitadoPor, duracionVisitaMin,
                costoMedioMxn, esSimulado, motivoEstado, creadoEn, ahora, cerradoEn, version);
    }

    public Negocio conAtributos(Integer duracion, Integer costo, Instant ahora) {
        return new Negocio(id, nombre, giro, estado, suspendidoPor, publicado, descripcion, telefonoContacto,
                correoContacto, contactoPublico, zonaHoraria, logoArchivoId, solicitadoPor, duracion, costo,
                esSimulado, motivoEstado, creadoEn, ahora, cerradoEn, version);
    }

    /** Solo cambia la fecha de actualizacion: lo usan los cambios de subcategorias, que viven en otra tabla. */
    public Negocio tocado(Instant ahora) {
        return conPublicado(publicado, ahora);
    }

    public boolean tieneContacto() {
        return telefonoContacto != null || correoContacto != null;
    }

    /** Sin el nombre ni el contacto en el texto: no deben acabar en un registro. */
    @Override
    public String toString() {
        return "Negocio[id=" + id + ", estado=" + estado + ", version=" + version + "]";
    }
}
