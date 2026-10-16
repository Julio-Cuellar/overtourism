package mx.negocio360.negocios.dominio;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Un dia distinto al horario semanal: un festivo cerrado o un horario especial. Una cerrada no lleva horas y una
 * abierta las exige (HO-03); el motivo es opcional y de hasta 80 caracteres.
 */
public record ExcepcionHorario(UUID id, LocalDate fecha, boolean cerrado, LocalTime abre, LocalTime cierra,
                               String motivo) {

    public static final int MAXIMO_MOTIVO = 80;

    public ExcepcionHorario {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(fecha, "fecha");
        if (cerrado ? (abre != null || cierra != null) : (abre == null || cierra == null || abre.equals(cierra))) {
            throw new NegociosException(CodigoError.HORARIO_INVALIDO);
        }
        if (motivo != null && (motivo.length() > MAXIMO_MOTIVO || motivo.codePoints().anyMatch(Character::isISOControl))) {
            throw new NegociosException(CodigoError.HORARIO_INVALIDO);
        }
        if (motivo != null && motivo.isBlank()) {
            motivo = null;
        }
    }

    /** Desde lo que escribe una persona: las horas como {@code HH:mm}. Cerrada no lleva horas; abierta las exige. */
    public static ExcepcionHorario de(UUID id, LocalDate fecha, boolean cerrado, String abre, String cierra,
                                      String motivo) {
        if (cerrado) {
            if (abre != null || cierra != null) {
                throw new NegociosException(CodigoError.HORARIO_INVALIDO);
            }
            return cerrada(id, fecha, motivo);
        }
        return abierta(id, fecha, motivo, Tramo.de(abre, cierra));
    }

    public static ExcepcionHorario cerrada(UUID id, LocalDate fecha, String motivo) {
        return new ExcepcionHorario(id, fecha, true, null, null, motivo);
    }

    public static ExcepcionHorario abierta(UUID id, LocalDate fecha, String motivo, Tramo tramo) {
        return new ExcepcionHorario(id, fecha, false, tramo.abre(), tramo.cierra(), motivo);
    }

    /** El tramo de esa excepcion, si esta abierta. */
    public Tramo tramo() {
        return cerrado ? null : new Tramo(abre, cierra);
    }
}
