package mx.negocio360.contabilidad.aplicacion;

import java.time.Instant;
import java.util.List;

/**
 * Lo que recibe quien prepara un comando: el token en claro (unica vez que existe), el resumen para confirmar y
 * cuando vence.
 */
public record PreparacionCreada(String token, String resumen, Instant expiraEn, List<String> advertencias) {

    public PreparacionCreada {
        advertencias = List.copyOf(advertencias);
    }

    /** El token es una credencial de confirmacion: no se imprime. */
    @Override
    public String toString() {
        return "PreparacionCreada[expiraEn=" + expiraEn + "]";
    }
}
