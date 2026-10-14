package mx.negocio360.identidad.dominio;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Nombre visible de una cuenta (AC-04): de 2 a 80 caracteres, sin caracteres de control, de formato (por ejemplo
 * los que invierten el sentido del texto), separadores de línea ni rellenos invisibles, y con al menos una letra o un
 * dígito. Recorta los extremos y colapsa los espacios repetidos. Es el único dato personal obligatorio además del
 * teléfono.
 */
public final class NombreVisible {

    private static final int MINIMO = 2;
    private static final int MAXIMO = 80;
    /** Tope de la entrada antes de mirarla con expresiones regulares: el nombre cabe con holgura aunque traiga espacios de más. */
    private static final int LARGO_BRUTO_MAXIMO = 400;
    private static final Pattern ESPACIOS = Pattern.compile("[ \\p{Zs}]{2,}");
    /** Letras sin trazo visible: rellenos de hangul (usables para fingir un nombre vacío). */
    private static final Set<Integer> RELLENOS = Set.of(0x115F, 0x1160, 0x3164, 0xFFA0);

    private NombreVisible() {
    }

    public static Optional<String> normalizar(String entrada) {
        if (entrada == null || entrada.length() > LARGO_BRUTO_MAXIMO || tieneCaracteresNoPermitidos(entrada)) {
            return Optional.empty();
        }
        var limpio = ESPACIOS.matcher(entrada.strip()).replaceAll(" ");
        var largo = limpio.codePointCount(0, limpio.length());
        var tieneLetraODigito = limpio.codePoints().anyMatch(Character::isLetterOrDigit);
        return largo >= MINIMO && largo <= MAXIMO && tieneLetraODigito ? Optional.of(limpio) : Optional.empty();
    }

    private static boolean tieneCaracteresNoPermitidos(String texto) {
        return texto.codePoints().anyMatch(NombreVisible::noPermitido);
    }

    private static boolean noPermitido(int codigo) {
        var tipo = Character.getType(codigo);
        return Character.isISOControl(codigo) || tipo == Character.FORMAT || tipo == Character.LINE_SEPARATOR
                || tipo == Character.PARAGRAPH_SEPARATOR || RELLENOS.contains(codigo);
    }
}
