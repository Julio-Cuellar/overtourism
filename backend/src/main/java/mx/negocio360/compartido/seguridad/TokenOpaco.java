package mx.negocio360.compartido.seguridad;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Objects;

/**
 * Token opaco de 256 bits (AS-05, SI-04). El token en claro solo viaja al cliente: en la base de datos se guarda su
 * hash SHA-256, que basta para buscarlo porque el token es aleatorio y de alta entropia. Quien busca por hash en el
 * almacen (como hace Identidad) no compara tokens: lo que se compara es un hash, que no revela nada del token. Para
 * comparar a mano un token contra un hash guardado, {@link #coincide} lo hace en tiempo constante.
 */
public final class TokenOpaco {

    private static final SecureRandom ALEATORIO = new SecureRandom();
    private static final int BYTES_DEL_TOKEN = 32;

    private TokenOpaco() {
    }

    /** Token nuevo: 32 bytes aleatorios en base64 URL sin relleno (43 caracteres). */
    public static String nuevo() {
        var bytes = new byte[BYTES_DEL_TOKEN];
        ALEATORIO.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** SHA-256 del texto, en hexadecimal minusculas (64 caracteres). */
    public static String hash(String texto) {
        Objects.requireNonNull(texto, "texto");
        try {
            var resumen = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(UTF_8));
            return HexFormat.of().formatHex(resumen);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no esta disponible", e);
        }
    }

    /** Dice si el token en claro corresponde al hash guardado. Un valor nulo o vacio nunca coincide. */
    public static boolean coincide(String hashGuardado, String token) {
        if (hashGuardado == null || hashGuardado.isEmpty() || token == null || token.isEmpty()) {
            return false;
        }
        return MessageDigest.isEqual(hashGuardado.getBytes(UTF_8), hash(token).getBytes(UTF_8));
    }
}
