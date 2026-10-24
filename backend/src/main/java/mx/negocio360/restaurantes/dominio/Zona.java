package mx.negocio360.restaurantes.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Una zona del salon (MS-01): agrupa mesas, por ejemplo «Terraza». Es inmutable, con baja logica (MS-07).
 *
 * @param id        identificador
 * @param negocioId negocio dueño
 * @param nombre    de 1 a 40 caracteres
 * @param orden     posicion en el salon, de 0 a 999
 * @param activa    falso si se dio de baja
 * @param version   bloqueo optimista, desde 1
 * @param creadaEn  cuando se creo
 */
public record Zona(UUID id, UUID negocioId, String nombre, int orden, boolean activa, long version, Instant creadaEn) {

    /** Tope de zonas activas por negocio (MS-01). */
    public static final int MAX_POR_NEGOCIO = 10;
    private static final int MAX_NOMBRE = 40;
    private static final int MAX_ORDEN = 999;

    public Zona {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        var recortado = nombre == null ? "" : nombre.strip();
        if (recortado.isEmpty() || recortado.length() > MAX_NOMBRE) {
            throw new RestaurantesException(CodigoError.NOMBRE_INVALIDO);
        }
        nombre = recortado;
        if (orden < 0 || orden > MAX_ORDEN) {
            throw new RestaurantesException(CodigoError.CONSULTA_INVALIDA);
        }
        Objects.requireNonNull(creadaEn, "creadaEn");
    }

    /** Una zona nueva, activa y en la version 1. */
    public static Zona nueva(UUID id, UUID negocioId, String nombre, int orden, Instant ahora) {
        return new Zona(id, negocioId, nombre, orden, true, 1, ahora);
    }

    /** La misma zona dada de baja (MS-07). */
    public Zona conBaja() {
        return new Zona(id, negocioId, nombre, orden, false, version + 1, creadaEn);
    }
}
