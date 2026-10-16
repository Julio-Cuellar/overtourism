package mx.negocio360.negocios.dominio;

import java.util.regex.Pattern;

/**
 * Telefono y correo de contacto del negocio (NG-09). Son los del negocio, no los del dueño, y solo se muestran al
 * turista si el dueño lo marco (PU-04). El mensaje de error nunca repite lo que se escribio.
 */
public final class ContactoNegocio {

    private static final Pattern DIEZ_DIGITOS = Pattern.compile("^(?:\\+?52)?(\\d{10})$");
    private static final Pattern SEPARADORES = Pattern.compile("[\\s().\\-]");
    private static final Pattern CORREO = Pattern.compile("^[^\\s@]+@[^\\s@.]+(\\.[^\\s@.]+)+$");
    private static final int MAXIMO_CORREO = 120;
    private static final int MAXIMO_ENTRADA = 60;

    private ContactoNegocio() {
    }

    /** Telefono mexicano de diez digitos, con o sin +52, guardado como {@code +52} y diez digitos (E.164). */
    public static String telefono(String entrada) {
        if (entrada == null || entrada.length() > MAXIMO_ENTRADA) {
            throw new NegociosException(CodigoError.CONTACTO_INVALIDO);
        }
        var sinSeparadores = SEPARADORES.matcher(entrada.strip()).replaceAll("");
        var coincidencia = DIEZ_DIGITOS.matcher(sinSeparadores);
        if (!coincidencia.matches()) {
            throw new NegociosException(CodigoError.CONTACTO_INVALIDO);
        }
        return "+52" + coincidencia.group(1);
    }

    /** Correo en minusculas, de hasta 120 caracteres y con una sola arroba. */
    public static String correo(String entrada) {
        if (entrada == null) {
            throw new NegociosException(CodigoError.CONTACTO_INVALIDO);
        }
        var limpio = entrada.strip().toLowerCase(java.util.Locale.ROOT);
        if (limpio.isEmpty() || limpio.length() > MAXIMO_CORREO || !CORREO.matcher(limpio).matches()
                || limpio.codePoints().anyMatch(Character::isISOControl)) {
            throw new NegociosException(CodigoError.CONTACTO_INVALIDO);
        }
        return limpio;
    }
}
