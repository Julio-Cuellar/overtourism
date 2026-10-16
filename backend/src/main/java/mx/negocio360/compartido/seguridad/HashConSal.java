package mx.negocio360.compartido.seguridad;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Hash con sal de un secreto corto, como el codigo de acceso (AS-01, SI-01). Nunca se guarda el secreto: solo la sal
 * (16 bytes aleatorios) y el SHA-256 de {@code sal:secreto}, ambos en hexadecimal. Como el codigo vive 5 minutos y
 * admite 5 intentos, el hash rapido es suficiente; el limite de intentos es el control real contra la fuerza bruta.
 */
public record HashConSal(String sal, String hash) {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int BYTES_DE_SAL = 16;
    private static final Pattern SAL_VALIDA = Pattern.compile("[0-9a-f]{32}");
    private static final Pattern HASH_VALIDO = Pattern.compile("[0-9a-f]{64}");

    public HashConSal {
        Objects.requireNonNull(sal, "sal");
        Objects.requireNonNull(hash, "hash");
        if (!SAL_VALIDA.matcher(sal).matches()) {
            throw new IllegalArgumentException("La sal debe ser hexadecimal de 32 caracteres");
        }
        if (!HASH_VALIDO.matcher(hash).matches()) {
            throw new IllegalArgumentException("El hash debe ser hexadecimal de 64 caracteres");
        }
    }

    public static HashConSal de(String secreto) {
        Objects.requireNonNull(secreto, "secreto");
        var bytes = new byte[BYTES_DE_SAL];
        ALEATORIO.nextBytes(bytes);
        var sal = HexFormat.of().formatHex(bytes);
        return new HashConSal(sal, calcular(sal, secreto));
    }

    /** Compara en tiempo constante. Un intento nulo o vacio nunca coincide. */
    public boolean verifica(String intento) {
        if (intento == null || intento.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(hash.getBytes(UTF_8), calcular(sal, intento).getBytes(UTF_8));
    }

    private static String calcular(String sal, String secreto) {
        return TokenOpaco.hash(sal + ":" + secreto);
    }

    @Override
    public String toString() {
        return "HashConSal[protegido]";
    }
}
