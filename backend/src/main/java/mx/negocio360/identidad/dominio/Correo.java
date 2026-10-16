package mx.negocio360.identidad.dominio;

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Correo de la cuenta (D-76), que junto con el id y el celular identifica a la persona: hasta 120 caracteres, siempre
 * en minusculas y solo con caracteres ASCII de la lista permitida (SI-16). La parte local admite letras, digitos y
 * {@code . _ % + -}; el dominio, etiquetas de letras, digitos y guiones que no empiezan ni terminan en guion, y al
 * menos un punto. No admite correos con caracteres Unicode.
 *
 * <p>La logica es la misma que la de Clientes, copiada a proposito: un modulo no importa a otro (R1). Cuando haya un
 * tercer uso conviene moverla a {@code compartido} (junto con {@link Telefono}).
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

    /**
     * Version para escribir en un registro: la primera letra y el dominio, con lo demas tapado ({@code a***@example.com}).
     * Recibe un correo ya normalizado.
     */
    public static String enmascarar(String correo) {
        var arroba = correo.lastIndexOf('@');
        if (arroba < 1) {
            return "***";
        }
        return correo.charAt(0) + "***" + correo.substring(arroba);
    }
}
