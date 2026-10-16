package mx.negocio360.compartido.tiempo;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Zona horaria de la plataforma. Las reglas que hablan de "hoy" (por ejemplo, la fecha de nacimiento no puede ser
 * futura, I-08) usan el calendario de Mexico, sin importar la zona del reloj que se inyecte: un reloj en UTC
 * aceptaria como "hoy" la fecha de manana desde las 18:00 hora de Mexico.
 */
public final class ZonaMexico {

    public static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private ZonaMexico() {
    }

    public static LocalDate hoy(Clock reloj) {
        return LocalDate.now(reloj.withZone(ZONA));
    }
}
