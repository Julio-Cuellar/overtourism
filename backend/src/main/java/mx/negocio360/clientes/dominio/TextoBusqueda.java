package mx.negocio360.clientes.dominio;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

/** Texto de busqueda (I-04): minusculas, sin acentos y con los espacios juntos. Equivale a lo que guardara la BD. */
public final class TextoBusqueda {

    private static final Pattern MARCAS_DE_ACENTO = Pattern.compile("\\p{M}+");
    private static final Pattern ESPACIOS = Pattern.compile("\\s+");

    private TextoBusqueda() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        var sinAcentos = MARCAS_DE_ACENTO.matcher(Normalizer.normalize(texto, Normalizer.Form.NFD)).replaceAll("");
        return ESPACIOS.matcher(sinAcentos.toLowerCase(Locale.ROOT).strip()).replaceAll(" ");
    }
}
