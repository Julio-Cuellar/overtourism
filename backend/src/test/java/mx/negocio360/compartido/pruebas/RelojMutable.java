package mx.negocio360.compartido.pruebas;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * Reloj de pruebas que solo avanza cuando la prueba lo manda. Usa la zona de Mexico, como la plataforma. Es la
 * misma idea que {@code clientes.fakes.RelojMutable}, pero en {@code compartido} para que Ventas y Caja no dependan
 * de las pruebas de otro modulo.
 */
public final class RelojMutable extends Clock {

    /** Instante fijo de las pruebas: 15 de octubre de 2026, 11:00 hora de Mexico. */
    public static final Instant AHORA = Instant.parse("2026-10-15T17:00:00Z");

    private static final ZoneId ZONA = ZoneId.of("America/Mexico_City");

    private Instant instante;

    public RelojMutable(Instant inicial) {
        this.instante = inicial;
    }

    public RelojMutable() {
        this(AHORA);
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
