package mx.negocio360.compartido.comandos;

import java.time.Instant;

/**
 * Lo que recibe quien prepara un comando: el token en claro (unica vez que existe), el resumen para confirmar y
 * cuando vence.
 *
 * @param token    token de un solo uso, 43 caracteres base64 URL
 * @param resumen  texto que la persona revisa antes de confirmar
 * @param expiraEn vencimiento del token
 */
public record PreparacionCreada(String token, String resumen, Instant expiraEn) {

    /** El token es una credencial de confirmacion: no se imprime. */
    @Override
    public String toString() {
        return "PreparacionCreada[expiraEn=" + expiraEn + "]";
    }
}
