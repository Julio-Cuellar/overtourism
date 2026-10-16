package mx.negocio360.identidad.aplicacion;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;

/** Arma el sobre comun de los eventos que publica Identidad: UUIDv7 nuevo y origen {@code identidad}. */
final class FabricaSobres {

    static final String ORIGEN = "identidad";

    private FabricaSobres() {
    }

    static SobreEvento de(UUID negocioId, Instant ahora, UUID correlacionId, UUID causaId, String tipo, int version,
                          Map<String, Object> datos) {
        return new SobreEvento(UuidV7.generar(ahora), tipo, version, negocioId, ORIGEN, ahora, correlacionId, causaId,
                datos);
    }
}
