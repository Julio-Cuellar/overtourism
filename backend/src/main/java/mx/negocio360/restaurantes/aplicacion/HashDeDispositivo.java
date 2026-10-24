package mx.negocio360.restaurantes.aplicacion;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * El hash con clave del identificador de dispositivo que manda la pagina del comensal (QR-12): HMAC-SHA256 en base64url.
 * El servidor guarda solo este hash y nunca el identificador, de modo que una copia de la base no permite reconocer al
 * dispositivo. Con la misma clave, el mismo dispositivo da siempre el mismo hash.
 */
public final class HashDeDispositivo {

    private static final String ALGORITMO = "HmacSHA256";
    private static final int BYTES_DE_CLAVE_AL_AZAR = 32;

    private final byte[] clave;

    private HashDeDispositivo(byte[] clave) {
        this.clave = clave;
    }

    /** Un hash con esa clave. */
    public static HashDeDispositivo conClave(byte[] clave) {
        Objects.requireNonNull(clave, "clave");
        if (clave.length == 0) {
            throw new IllegalArgumentException("La clave del hash del dispositivo no puede estar vacia.");
        }
        return new HashDeDispositivo(clave.clone());
    }

    /**
     * Un hash con una clave al azar de este arranque. Los hashes no sobreviven a un reinicio: el comensal deja de ver sus
     * lineas anteriores. En produccion se da una clave fija.
     */
    public static HashDeDispositivo alAzar() {
        var bytes = new byte[BYTES_DE_CLAVE_AL_AZAR];
        new SecureRandom().nextBytes(bytes);
        return new HashDeDispositivo(bytes);
    }

    /** El hash del identificador de un dispositivo. */
    public String de(String dispositivoId) {
        Objects.requireNonNull(dispositivoId, "dispositivoId");
        try {
            var mac = Mac.getInstance(ALGORITMO);
            mac.init(new SecretKeySpec(clave, ALGORITMO));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(dispositivoId.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el hash del dispositivo.", e);
        }
    }

    /** Nunca muestra la clave. */
    @Override
    public String toString() {
        return "HashDeDispositivo";
    }
}
