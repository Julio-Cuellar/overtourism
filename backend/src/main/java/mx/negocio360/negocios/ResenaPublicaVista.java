package mx.negocio360.negocios;

import java.time.Instant;
import java.util.UUID;

/** Una reseña como la ve el turista (T3): sin autor y con la respuesta del negocio si la hay (RS-08). */
public record ResenaPublicaVista(UUID id, int calificacion, String comentario, Instant creadaEn, String respuesta,
                                 Instant respondidaEn, boolean demostracion) {

    @Override
    public String toString() {
        return "ResenaPublicaVista[id=" + id + "]";
    }
}
