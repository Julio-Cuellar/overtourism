package mx.negocio360.negocios;

import java.time.LocalDate;
import java.util.UUID;

/** Un dia distinto al horario de la semana. Cerrado no lleva horas. */
public record ExcepcionVista(UUID id, LocalDate fecha, boolean cerrado, String abre, String cierra, String motivo) {
}
