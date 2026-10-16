package mx.negocio360.negocios;

import java.util.List;

/** Los tramos de un dia tal como llegan de la app. Sin tramos el dia queda cerrado. */
public record DiaHorarioComando(int dia, List<TramoComando> tramos) {
}
