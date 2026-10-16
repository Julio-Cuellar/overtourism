package mx.negocio360.negocios;

import java.time.LocalDate;

/** Una excepcion al horario tal como llega de la app (HO-03). Cerrada no lleva horas; abierta las exige. */
public record ExcepcionComando(LocalDate fecha, boolean cerrado, String abre, String cierra, String motivo) {
}
