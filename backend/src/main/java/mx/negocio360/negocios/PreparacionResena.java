package mx.negocio360.negocios;

import java.time.Instant;
import java.util.List;

/** Lo que recibe el turista al preparar una reseña (RS-09): el token de un solo uso, que vence a los 5 minutos. */
public record PreparacionResena(String token, String resumen, Instant expiraEn, List<String> advertencias) {

    public PreparacionResena {
        advertencias = List.copyOf(advertencias);
    }

    /** Sin el token en el texto. */
    @Override
    public String toString() {
        return "PreparacionResena[expiraEn=" + expiraEn + "]";
    }
}
