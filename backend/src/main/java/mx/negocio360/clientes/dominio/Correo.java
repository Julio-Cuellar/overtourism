package mx.negocio360.clientes.dominio;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Correo opcional (I-07): hasta 120 caracteres, siempre en minusculas y solo con caracteres ASCII de la lista
 * permitida (SG-17). La parte local admite letras, digitos y {@code . _ % + -}; el dominio, etiquetas de letras,
 * digitos y guiones que no empiezan ni terminan en guion. No admite correos con caracteres Unicode.
 */
public final class Correo {

    private static final int MAX_CARACTERES = 120;
    private static final String ETIQUETA = "[a-z0-9]([a-z0-9\\-]*[a-z0-9])?";
    private static final Pattern FORMA = Pattern.compile("[a-z0-9._%+\\-]+@" + ETIQUETA + "(\\." + ETIQUETA + ")+");

    private Correo() {
    }

    public static Optional<String> normalizar(String entrada) {
        if (entrada == null) {
            return Optional.empty();
        }
        var correo = entrada.strip().toLowerCase(Locale.ROOT);
        if (correo.length() > MAX_CARACTERES || !FORMA.matcher(correo).matches()) {
            return Optional.empty();
        }
        return Optional.of(correo);
    }
}
