package mx.negocio360.negocios;

import java.time.Instant;

/** La disponibilidad vigente que ve el turista. Vencida o con el negocio cerrado ahora, no se muestra (DI-03, DI-04). */
public record DisponibilidadPublicaVista(String nivel, String nota, Instant declaradaEn) {
}
