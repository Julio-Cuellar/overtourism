package mx.negocio360.clientes.dominio;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Telefono mexicano en formato E.164 (I-02): {@code +52} y 10 digitos, el primero entre 2 y 9 (ninguna lada empieza
 * en 0 o 1). Acepta espacios (tambien los no separables que trae un telefono copiado del movil), guiones, puntos y
 * parentesis, y el prefijo {@code +52} o {@code 52}. Una entrada de mas de 40 caracteres se rechaza sin analizarla.
 */
public final class Telefono {

    private static final int MAX_CARACTERES_DE_ENTRADA = 40;
    private static final Pattern SEPARADORES = Pattern.compile("[\\s\\p{Z}().\\-]");
    private static final Pattern DIEZ_DIGITOS = Pattern.compile("[2-9]\\d{9}");
    private static final String PREFIJO_MEXICO = "52";
    private static final int LARGO_CON_PREFIJO = 12;
    private static final String MASCARA = "•••• ";
    private static final int DIGITOS_VISIBLES = 4;

    private Telefono() {
    }

    public static Optional<String> normalizar(String entrada) {
        if (entrada == null || entrada.length() > MAX_CARACTERES_DE_ENTRADA) {
            return Optional.empty();
        }
        var limpio = SEPARADORES.matcher(entrada.strip()).replaceAll("");
        if (limpio.startsWith("+")) {
            if (!limpio.startsWith("+" + PREFIJO_MEXICO)) {
                return Optional.empty();
            }
            limpio = limpio.substring(1 + PREFIJO_MEXICO.length());
        } else if (limpio.length() == LARGO_CON_PREFIJO && limpio.startsWith(PREFIJO_MEXICO)) {
            limpio = limpio.substring(PREFIJO_MEXICO.length());
        }
        return DIEZ_DIGITOS.matcher(limpio).matches() ? Optional.of("+" + PREFIJO_MEXICO + limpio) : Optional.empty();
    }

    /** Muestra solo los ultimos 4 digitos (P-05, SC-02). Recibe un telefono ya normalizado. */
    public static String enmascarar(String telefonoE164) {
        return MASCARA + telefonoE164.substring(telefonoE164.length() - DIGITOS_VISIBLES);
    }
}
