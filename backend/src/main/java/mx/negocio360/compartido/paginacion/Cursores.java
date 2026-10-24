package mx.negocio360.compartido.paginacion;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Paginacion por cursor opaco para las listas de Ventas y Caja (D-131): maximo {@value #MAXIMO} por pagina,
 * {@value #INICIAL} por omision. El cursor es un desplazamiento codificado: el cliente no debe interpretarlo. Sirve a los
 * almacenes en memoria ({@link #pagina(List, int, String)}) y a los de PostgreSQL ({@link #pagina(Consulta, int, String)},
 * con {@code LIMIT} y {@code OFFSET}): el cursor es el mismo en los dos, asi que el contrato no cambia. Un cursor por llave
 * es una mejora posible para listas muy largas.
 */
public final class Cursores {

    /** Tope de elementos por pagina. */
    public static final int MAXIMO = 100;
    /** Elementos por pagina cuando el cliente no pide un numero. */
    public static final int INICIAL = 20;
    private static final String PREFIJO = "p1:";

    private Cursores() {
    }

    /**
     * Dice si el tamano y el cursor son aceptables.
     *
     * @param primeros cuantos elementos se piden, de 1 a {@value #MAXIMO}
     * @param despues  cursor de la pagina anterior, o nulo para la primera
     * @return falso si el tamano esta fuera de rango o el cursor no lo emitimos nosotros
     */
    public static boolean valido(int primeros, String despues) {
        return primeros >= 1 && primeros <= MAXIMO && (despues == null || desplazamiento(despues) >= 0);
    }

    /**
     * Corta una pagina de una lista ya ordenada.
     *
     * @param ordenada lista completa en el orden de la consulta
     * @param primeros cuantos elementos se piden; ya validado con {@link #valido}
     * @param despues  cursor de la pagina anterior, o nulo
     * @param <T>      tipo de elemento
     * @return la pagina pedida
     * @throws IllegalArgumentException si el tamano o el cursor no son validos
     */
    public static <T> Pagina<T> pagina(List<T> ordenada, int primeros, String despues) {
        if (!valido(primeros, despues)) {
            throw new IllegalArgumentException("Paginacion invalida");
        }
        var desde = despues == null ? 0 : desplazamiento(despues);
        var inicio = Math.min(desde, ordenada.size());
        var fin = Math.min(inicio + primeros, ordenada.size());
        var hayMas = fin < ordenada.size();
        return new Pagina<>(ordenada.subList(inicio, fin), hayMas, hayMas ? codificar(fin) : null);
    }

    /**
     * Consulta de una pagina: la pide el almacen de PostgreSQL con {@code LIMIT} y {@code OFFSET}.
     *
     * @param <T> tipo de elemento
     */
    @FunctionalInterface
    public interface Consulta<T> {

        /**
         * Trae filas en el orden de la consulta.
         *
         * @param limite        cuantas filas como maximo
         * @param desplazamiento cuantas filas saltar
         * @return las filas pedidas
         */
        List<T> filas(int limite, int desplazamiento);
    }

    /**
     * Corta una pagina pidiendole al almacen una fila de mas para saber si quedan elementos.
     *
     * @param consulta  trae filas en el orden de la consulta
     * @param primeros  cuantos elementos se piden; ya validado con {@link #valido}
     * @param despues   cursor de la pagina anterior, o nulo
     * @param <T>       tipo de elemento
     * @return la pagina pedida
     * @throws IllegalArgumentException si el tamano o el cursor no son validos; no se consulta nada
     */
    public static <T> Pagina<T> pagina(Consulta<T> consulta, int primeros, String despues) {
        if (!valido(primeros, despues)) {
            throw new IllegalArgumentException("Paginacion invalida");
        }
        var desde = despues == null ? 0 : desplazamiento(despues);
        var filas = consulta.filas(primeros + 1, desde);
        var hayMas = filas.size() > primeros;
        return new Pagina<>(hayMas ? filas.subList(0, primeros) : filas, hayMas, hayMas ? codificar(desde + primeros) : null);
    }

    private static String codificar(int desplazamiento) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString((PREFIJO + desplazamiento).getBytes(StandardCharsets.US_ASCII));
    }

    /** Desplazamiento que lleva el cursor, o -1 si no es uno de los nuestros. */
    private static int desplazamiento(String cursor) {
        if (cursor.length() > 40) {
            return -1;
        }
        try {
            var texto = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.US_ASCII);
            if (!texto.startsWith(PREFIJO)) {
                return -1;
            }
            var numero = Integer.parseInt(texto.substring(PREFIJO.length()));
            return numero >= 0 ? numero : -1;
        } catch (IllegalArgumentException e) {
            return -1;
        }
    }
}
