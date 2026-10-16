package mx.negocio360.negocios.aplicacion;

/**
 * Topes de la superficie publica (PU-05, SG-06 a SG-09): se aplican en codigo, no solo en el esquema, y valen aunque
 * el cliente pida mas. Ver la seccion 9.7 del diseno.
 */
public final class LimitesPublicos {

    /** Cuantos negocios devuelve una pagina si el cliente no dice (el valor por omision del esquema). */
    public static final int POR_PAGINA_INICIAL = 20;
    /** Lo maximo que devuelve una pagina, aunque pidan mas. */
    public static final int POR_PAGINA_MAXIMA = 20;
    /** Radio maximo de la busqueda por cercania. */
    public static final int RADIO_MAXIMO_KM = 25;
    /** Caracteres maximos del texto de busqueda. */
    public static final int TEXTO_MAXIMO = 80;

    private LimitesPublicos() {
    }
}
