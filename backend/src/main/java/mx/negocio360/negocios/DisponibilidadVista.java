package mx.negocio360.negocios;

import java.time.Instant;

/** Lo que el negocio declaro de su disponibilidad (DI-01): un nivel, una nota corta y hasta cuando vale. */
public record DisponibilidadVista(String nivel, String nota, Instant declaradaEn, Instant venceEn) {
}
