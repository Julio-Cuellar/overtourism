package mx.negocio360.seguridad;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Solo pruebas: un reloj que solo avanza cuando la prueba lo manda. */
final class RelojManual extends Clock {

    private Instant instante;

    RelojManual(Instant inicial) {
        this.instante = inicial;
    }

    void avanzar(Duration duracion) {
        instante = instante.plus(duracion);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
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
