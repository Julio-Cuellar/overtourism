package mx.negocio360.clientes.dominio;

/**
 * Lista de caracteres que ningun dato personal ni texto del aviso puede llevar (SG-17): controles (NUL rompe el
 * INSERT; el salto de linea falsifica lineas de registro), caracteres de formato e invisibles (controles
 * bidireccionales y de ancho cero suplantan a otro cliente) y los separadores de linea y de parrafo Unicode.
 *
 * <p>No decide como se muestra un texto: el escape al pintarlo en el panel sigue siendo obligatorio.
 */
final class CaracteresSeguros {

    private CaracteresSeguros() {
    }

    /** Recorta los extremos, incluidos los espacios Unicode (NBSP, U+202F...). Un texto vacio queda nulo. */
    static String recortar(String valor) {
        if (valor == null) {
            return null;
        }
        var inicio = 0;
        var fin = valor.length();
        while (inicio < fin && esEspacio(valor.charAt(inicio))) {
            inicio++;
        }
        while (fin > inicio && esEspacio(valor.charAt(fin - 1))) {
            fin--;
        }
        return inicio == fin ? null : valor.substring(inicio, fin);
    }

    /** Cambia cada espacio Unicode (NBSP, U+202F...) por un espacio normal. */
    static String conEspaciosNormales(String valor) {
        var texto = new StringBuilder(valor.length());
        valor.chars().forEach(c -> texto.append(esEspacioUnicode(c) ? ' ' : (char) c));
        return texto.toString();
    }

    /** Verdadero si lleva algun caracter prohibido. Con {@code permitirSaltos}, se admiten \n, \r y \t. */
    static boolean contieneProhibidos(String valor, boolean permitirSaltos) {
        return valor.codePoints().anyMatch(c -> esProhibido(c, permitirSaltos));
    }

    static String sinProhibidos(String valor, boolean permitirSaltos) {
        var texto = new StringBuilder(valor.length());
        valor.codePoints().filter(c -> !esProhibido(c, permitirSaltos)).forEach(texto::appendCodePoint);
        return texto.toString();
    }

    private static boolean esEspacio(char c) {
        return Character.isWhitespace(c) || esEspacioUnicode(c);
    }

    private static boolean esEspacioUnicode(int c) {
        return Character.getType(c) == Character.SPACE_SEPARATOR;
    }

    private static boolean esProhibido(int c, boolean permitirSaltos) {
        if (permitirSaltos && (c == '\n' || c == '\r' || c == '\t')) {
            return false;
        }
        var tipo = Character.getType(c);
        return tipo == Character.CONTROL || tipo == Character.FORMAT
                || tipo == Character.LINE_SEPARATOR || tipo == Character.PARAGRAPH_SEPARATOR;
    }
}
