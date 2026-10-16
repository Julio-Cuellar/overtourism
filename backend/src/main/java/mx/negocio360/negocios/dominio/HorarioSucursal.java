package mx.negocio360.negocios.dominio;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * El horario de una sucursal: la semana y las excepciones. Calcula «abierto ahora» en el servidor con la zona del
 * negocio (HO-04), nunca en el celular. La excepcion de una fecha manda sobre la semana ese dia completo, tambien sobre
 * el tramo que venia del dia anterior.
 */
public record HorarioSucursal(SemanaHorario semana, List<ExcepcionHorario> excepciones) {

    public HorarioSucursal {
        Objects.requireNonNull(semana, "semana");
        excepciones = List.copyOf(excepciones);
    }

    public boolean abiertoEn(Instant instante, ZoneId zona) {
        var local = instante.atZone(zona);
        var hoy = local.toLocalDate();
        var hora = local.toLocalTime();
        var deHoy = excepcion(hoy);
        if (deHoy != null && deHoy.cerrado()) {
            return false;
        }
        for (var tramo : tramosDe(hoy, deHoy)) {
            if (!hora.isBefore(tramo.abre()) && (tramo.cruzaMedianoche() || hora.isBefore(tramo.cierra()))) {
                return true;
            }
        }
        // El tramo que abrio ayer y cruza la medianoche sigue abierto hasta su hora de cierre.
        var ayer = hoy.minusDays(1);
        for (var tramo : tramosDe(ayer, excepcion(ayer))) {
            if (tramo.cruzaMedianoche() && hora.isBefore(tramo.cierra())) {
                return true;
            }
        }
        return false;
    }

    private ExcepcionHorario excepcion(LocalDate fecha) {
        return excepciones.stream().filter(e -> e.fecha().equals(fecha)).findFirst().orElse(null);
    }

    private List<Tramo> tramosDe(LocalDate fecha, ExcepcionHorario excepcion) {
        if (excepcion != null) {
            return excepcion.cerrado() ? List.of() : List.of(excepcion.tramo());
        }
        return semana.dia(fecha.getDayOfWeek().getValue()).tramos();
    }

    /** Una hora del dia sin zona, para escribirla como HH:mm. */
    public static String texto(LocalTime hora) {
        return hora == null ? null : String.format("%02d:%02d", hora.getHour(), hora.getMinute());
    }
}
