package mx.negocio360.negocios.aplicacion.puertos;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Reseñas preparadas y aun sin confirmar (RS-09, D-23). Guarda solo el hash del token; el comentario a medias se vacia al
 * confirmar (SN-09). Se busca por el hash, no por negocio.
 */
public interface RepositorioPreparacionesResena {

    record Preparada(String tokenHash, UUID negocioId, UUID visitanteId, int calificacion, String comentario,
                     Instant creadaEn, Instant expiraEn, Instant usadaEn) {

        /** Sin el hash ni el comentario en el texto. */
        @Override
        public String toString() {
            return "Preparada[negocioId=" + negocioId + ", usada=" + (usadaEn != null) + "]";
        }
    }

    void guardar(Preparada preparada);

    Optional<Preparada> porHash(String tokenHash);

    /** Marca el token como usado y vacia el comentario. {@code false} si ya estaba usado: decide quien gana una carrera. */
    boolean marcarUsada(String tokenHash, Instant ahora);

    /** Cuantas preparo ese dispositivo desde ese momento, para el limite por hora (RS-11). */
    int preparadasDesde(UUID visitanteId, Instant desde);

    /** Borra las vencidas antes de ese momento: un comentario a medias no se guarda para siempre (SN-13). */
    int borrarVencidas(Instant antesDe);

    /** Borra las preparaciones de un negocio (restablecimiento de la demo). Devuelve cuantas. */
    int borrarDeNegocio(UUID negocioId);
}
