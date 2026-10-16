package mx.negocio360.caja.aplicacion;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;

/** Arma el sobre de los eventos de Caja: UUIDv7 nuevo, origen {@code caja}, correlacion = el turno. */
final class FabricaSobres {

    static final String ORIGEN = "caja";

    private FabricaSobres() {
    }

    static SobreEvento de(UUID negocioId, Instant ahora, UUID turnoId, String tipo, int version,
                          Map<String, Object> datos) {
        return new SobreEvento(UuidV7.generar(ahora), tipo, version, negocioId, ORIGEN, ahora, turnoId, null, datos);
    }
}
