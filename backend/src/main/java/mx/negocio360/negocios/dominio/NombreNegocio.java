package mx.negocio360.negocios.dominio;

import java.util.regex.Pattern;

/**
 * Nombre comercial del negocio (NG-01): de 2 a 80 caracteres, sin caracteres de control ni de formato, y con los
 * espacios sobrantes colapsados. El texto ya viene limpio: quien lo usa no vuelve a normalizarlo.
 */
public record NombreNegocio(String valor) {

    public static final int MINIMO = 2;
    public static final int MAXIMO = 80;
    private static final Pattern ESPACIOS = Pattern.compile("[\\p{Zs}\\s]+");
    private static final int MAXIMO_DE_ENTRADA = 400;

    public NombreNegocio {
        if (valor == null) {
            throw new NegociosException(CodigoError.NOMBRE_INVALIDO);
        }
    }

    /** @throws NegociosException con {@code NOMBRE_INVALIDO} si no sirve */
    public static NombreNegocio de(String entrada) {
        if (entrada == null || entrada.length() > MAXIMO_DE_ENTRADA) {
            throw new NegociosException(CodigoError.NOMBRE_INVALIDO);
        }
        var recortado = entrada.strip();
        if (recortado.codePoints().anyMatch(NombreNegocio::esInvisible)) {
            throw new NegociosException(CodigoError.NOMBRE_INVALIDO);
        }
        var limpio = ESPACIOS.matcher(recortado).replaceAll(" ");
        var largo = limpio.codePointCount(0, limpio.length());
        if (largo < MINIMO || largo > MAXIMO) {
            throw new NegociosException(CodigoError.NOMBRE_INVALIDO);
        }
        return new NombreNegocio(limpio);
    }

    /** Control (incluye tabuladores y saltos de linea) o formato (invisibles, marcas de direccion). */
    private static boolean esInvisible(int c) {
        var tipo = Character.getType(c);
        return tipo == Character.CONTROL || tipo == Character.FORMAT || tipo == Character.PRIVATE_USE
                || tipo == Character.SURROGATE || tipo == Character.UNASSIGNED;
    }

    @Override
    public String toString() {
        return valor;
    }
}
