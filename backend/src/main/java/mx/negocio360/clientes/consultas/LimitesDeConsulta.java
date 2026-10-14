package mx.negocio360.clientes.consultas;

/**
 * Topes contra la extraccion masiva de datos personales (SC-03, SG-07). El esquema GraphQL no puede expresar un
 * maximo, asi que los resolvedores deben aplicar estos valores a cada consulta.
 */
public final class LimitesDeConsulta {

    /** Tamano de pagina cuando la consulta no pide uno. Debe coincidir con el valor por defecto del esquema. */
    public static final int POR_PAGINA_INICIAL = 20;
    public static final int MAX_POR_PAGINA = 50;
    public static final int MAX_SUGERENCIAS = 5;
    public static final int MIN_TEXTO_SUGERENCIA = 2;
    public static final int MAX_TEXTO_BUSQUEDA = 80;

    private LimitesDeConsulta() {
    }

    /** Recorta la pagina pedida al maximo. Una pagina de cero o menos es un error de quien llama. */
    public static int paginaEfectiva(int pedido) {
        if (pedido < 1) {
            throw new IllegalArgumentException("El tamano de pagina debe ser mayor que cero: " + pedido);
        }
        return Math.min(pedido, MAX_POR_PAGINA);
    }

    /** El texto de una sugerencia necesita al menos dos caracteres (sin espacios de los extremos) y no mas de 80. */
    public static boolean textoDeSugerenciaValido(String texto) {
        if (texto == null) {
            return false;
        }
        var largo = texto.strip().length();
        return largo >= MIN_TEXTO_SUGERENCIA && largo <= MAX_TEXTO_BUSQUEDA;
    }
}
