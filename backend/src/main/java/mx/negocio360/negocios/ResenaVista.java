package mx.negocio360.negocios;

import java.time.Instant;
import java.util.UUID;

/**
 * Una reseña para el panel del negocio (G9). Sin el autor: ni su id de dispositivo (RS-08). El comentario es de un
 * tercero y no se imprime (SN-13).
 */
public record ResenaVista(UUID id, int calificacion, String comentario, Instant creadaEn, String estado,
                          String respuesta, Instant respondidaEn, boolean demostracion, long version) {

    @Override
    public String toString() {
        return "ResenaVista[id=" + id + ", estado=" + estado + "]";
    }
}
