package mx.negocio360.compartido.dinero;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Unica gramatica del dinero y las cantidades en la plataforma: decimal plano, hasta 14 enteros y 8 decimales,
 * sin exponente, sin signo mas, sin espacios. La usan el escalar de GraphQL y el JSON de los eventos, para que no
 * existan dos formas distintas de leer un monto. El limite de largo evita construir decimales gigantes.
 */
public final class DecimalPlano {

    private static final Pattern FORMA_PLANA = Pattern.compile("-?\\d{1,14}(\\.\\d{1,8})?");

    private DecimalPlano() {
    }

    /** Lee un decimal plano, o devuelve vacio si el texto no cumple la gramatica. */
    public static Optional<BigDecimal> leer(String texto) {
        if (texto == null || !FORMA_PLANA.matcher(texto).matches()) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(texto));
    }

    /** Escribe un decimal siempre en forma plana, sin notacion cientifica. */
    public static String escribir(BigDecimal valor) {
        return valor.toPlainString();
    }
}
