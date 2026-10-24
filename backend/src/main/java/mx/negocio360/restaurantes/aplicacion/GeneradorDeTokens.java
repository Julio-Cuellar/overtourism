package mx.negocio360.restaurantes.aplicacion;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * De donde salen los tokens opacos del QR de cada mesa (QR-01): 128 bits aleatorios en base64url, sin relleno. No contienen
 * el id de la mesa ni el del negocio. Es una interfaz para que las pruebas usen tokens predecibles.
 */
@FunctionalInterface
public interface GeneradorDeTokens {

    /** El generador real: {@link SecureRandom}, 16 bytes, 22 caracteres. */
    GeneradorDeTokens ALEATORIO = new GeneradorDeTokens() {
        private final SecureRandom azar = new SecureRandom();
        private final Base64.Encoder codificador = Base64.getUrlEncoder().withoutPadding();

        @Override
        public String nuevo() {
            var bytes = new byte[16];
            azar.nextBytes(bytes);
            return codificador.encodeToString(bytes);
        }
    };

    /** Un token nuevo. */
    String nuevo();
}
