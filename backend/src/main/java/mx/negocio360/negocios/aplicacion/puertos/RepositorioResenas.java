package mx.negocio360.negocios.aplicacion.puertos;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.negocios.dominio.Resena;
import mx.negocio360.negocios.dominio.ResumenCalificacion;

/** Reseñas y su resumen, con seguridad por fila por negocio, y su lectura publica por la vista sin autor (RS-08, SN-14). */
public interface RepositorioResenas {

    /** Una reseña como la ve el publico: sin el autor. */
    record Publica(UUID id, int calificacion, String comentario, Instant creadaEn, String respuesta,
                   Instant respondidaEn, boolean demostracion) {

        /** Sin el comentario en el texto (SN-13). */
        @Override
        public String toString() {
            return "Publica[id=" + id + "]";
        }
    }

    /** {@code false} si ese autor ya tenia una reseña en ese negocio (RS-02): no cambia nada. */
    boolean insertar(Resena resena);

    Optional<Resena> resena(UUID negocioId, UUID resenaId);

    boolean existePorAutor(UUID negocioId, UUID visitanteId);

    /** De la mas nueva a la mas antigua. {@code sinRespuesta} deja solo las publicadas que el negocio no ha respondido. */
    List<Resena> pagina(UUID negocioId, boolean sinRespuesta, int limite, int desplazamiento);

    /** Guarda la respuesta si la reseña esta publicada y aun no tiene (RS-04). {@code false} si no se pudo. */
    boolean responder(UUID negocioId, UUID resenaId, String respuesta, UUID respondidaPor, Instant ahora);

    /** Oculta o restituye (RS-05). {@code false} si la reseña no existe en ese negocio. */
    boolean cambiarEstado(UUID negocioId, UUID resenaId, Resena.Estado estado, Instant ahora);

    /** Recalcula el resumen a partir de las reseñas publicadas, en la misma transaccion (RS-06). */
    ResumenCalificacion recalcularResumen(UUID negocioId, Instant ahora);

    ResumenCalificacion resumen(UUID negocioId);

    /** Lo publico de un negocio publico y activo, por la vista y con el rol de solo lectura. */
    List<Publica> publicas(UUID negocioId, int limite, int desplazamiento);

    /** Borra todas las reseñas de un negocio (solo lo usa el restablecimiento de la demo, DM-04). Devuelve cuantas. */
    int borrarTodas(UUID negocioId);
}
