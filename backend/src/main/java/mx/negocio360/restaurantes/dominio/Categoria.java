package mx.negocio360.restaurantes.dominio;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Una categoria del menu (MN-07): agrupa platillos y decide a que estacion van (D-179). Es inmutable y su version sube con
 * cada cambio.
 *
 * @param id           identificador
 * @param negocioId    negocio dueño
 * @param nombre       de 2 a 40 caracteres
 * @param orden        posicion en el menu, de 0 a 999
 * @param estacion     cocina (por omision) o barra
 * @param activa       falso si ya no se usa
 * @param version      bloqueo optimista, desde 1
 * @param creadaEn     cuando se creo
 * @param actualizadaEn ultimo cambio
 */
public record Categoria(UUID id, UUID negocioId, String nombre, int orden, Estacion estacion, boolean activa, long version,
                        Instant creadaEn, Instant actualizadaEn) {

    /** Tope de categorias por negocio (MN-07). */
    public static final int MAX_POR_NEGOCIO = 30;
    private static final int MAX_NOMBRE = 40;
    private static final int MAX_ORDEN = 999;

    public Categoria {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(negocioId, "negocioId");
        var recortado = nombre == null ? "" : nombre.strip();
        if (recortado.length() < 2 || recortado.length() > MAX_NOMBRE) {
            throw new RestaurantesException(CodigoError.NOMBRE_INVALIDO);
        }
        nombre = recortado;
        if (orden < 0 || orden > MAX_ORDEN) {
            throw new RestaurantesException(CodigoError.CONSULTA_INVALIDA);
        }
        estacion = estacion == null ? Estacion.COCINA : estacion;
        Objects.requireNonNull(creadaEn, "creadaEn");
        Objects.requireNonNull(actualizadaEn, "actualizadaEn");
    }

    /** Una categoria nueva, activa y en la version 1. */
    public static Categoria nueva(UUID id, UUID negocioId, String nombre, int orden, Estacion estacion, Instant ahora) {
        return new Categoria(id, negocioId, nombre, orden, estacion, true, 1, ahora, ahora);
    }

    /** Cambia nombre, orden y estacion, y sube la version. */
    public Categoria conDatos(String nuevoNombre, int nuevoOrden, Estacion nuevaEstacion, Instant ahora) {
        return new Categoria(id, negocioId, nuevoNombre, nuevoOrden, nuevaEstacion, activa, version + 1, creadaEn, ahora);
    }
}
