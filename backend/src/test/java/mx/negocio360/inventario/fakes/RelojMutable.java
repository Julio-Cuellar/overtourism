package mx.negocio360.inventario.fakes;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/** Reloj de pruebas que solo avanza cuando la prueba lo manda. Usa la zona de Mexico, como la plataforma. */
public final class RelojMutable extends Clock {

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private Instant instante;

    public RelojMutable(Instant inicial) {
        this.instante = inicial;
    }

    public void avanzar(Duration duracion) {
        instante = instante.plus(duracion);
    }

    @Override
    public ZoneId getZone() {
        return ZONA;
    }

    @Override
    public Clock withZone(ZoneId zona) {
        return this;
    }

    @Override
    public Instant instant() {
        return instante;
    }
}
