package mx.negocio360.identidad.dominio;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Lo que una persona escribe para identificarse (D-76): su celular, su correo o el id de su cuenta. Ya viene
 * normalizado (celular en E.164, correo y UUID en minusculas). Si trae arroba se trata como correo; si tiene forma de
 * UUID, como id; el resto, como celular. La entrada enorme se rechaza sin analizarla. {@code toString} no revela el
 * valor (SI-15).
 */
public record Identificador(Tipo tipo, String valor) {

    public enum Tipo { TELEFONO, CORREO, ID }

    private static final int MAX_CARACTERES_DE_ENTRADA = 200;
    private static final Pattern FORMA_DE_UUID =
            Pattern.compile("[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    public Identificador {
        Objects.requireNonNull(tipo, "tipo");
        Objects.requireNonNull(valor, "valor");
    }

    /** @throws IdentidadException TELEFONO_INVALIDO o CORREO_INVALIDO si no tiene una forma valida */
    public static Identificador de(String entrada) {
        if (entrada != null && entrada.contains("@")) {
            if (entrada.length() > MAX_CARACTERES_DE_ENTRADA) {
                throw new IdentidadException(CodigoError.CORREO_INVALIDO);
            }
            var correo = Correo.normalizar(entrada).orElseThrow(() -> new IdentidadException(CodigoError.CORREO_INVALIDO));
            return new Identificador(Tipo.CORREO, correo);
        }
        if (entrada != null && entrada.length() <= MAX_CARACTERES_DE_ENTRADA && FORMA_DE_UUID.matcher(entrada.strip()).matches()) {
            return new Identificador(Tipo.ID, UUID.fromString(entrada.strip()).toString().toLowerCase(Locale.ROOT));
        }
        var telefono = Telefono.normalizar(entrada).orElseThrow(() -> new IdentidadException(CodigoError.TELEFONO_INVALIDO));
        return new Identificador(Tipo.TELEFONO, telefono);
    }

    @Override
    public String toString() {
        return "Identificador[" + tipo + "]";
    }
}
