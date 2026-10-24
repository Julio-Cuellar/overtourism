package mx.negocio360.compartido.paginacion;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

/**
 * Paginacion por cursor opaco para las listas de Ventas y Caja (D-131): maximo {@value #MAXIMO} por pagina,
 * {@value #INICIAL} por omision. El cursor es un desplazamiento codificado: el cliente no debe interpretarlo. Sirve a los
 * almacenes en memoria; los de PostgreSQL usaran un cursor por llave (F6) sin cambiar el contrato.
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
