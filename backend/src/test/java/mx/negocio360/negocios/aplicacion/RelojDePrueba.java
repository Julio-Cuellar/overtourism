package mx.negocio360.negocios.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Un reloj que las pruebas mueven a su gusto (HO-04, DI-03). */
final class RelojDePrueba extends Clock {

    private volatile Instant ahora;

    RelojDePrueba(Instant ahora) {
        this.ahora = ahora;
    }

    void fijar(Instant nuevo) {
        this.ahora = nuevo;
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return ahora;
    }
}
