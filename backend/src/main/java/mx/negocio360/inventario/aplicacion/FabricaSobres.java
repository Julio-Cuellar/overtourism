package mx.negocio360.inventario.aplicacion;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;

/** Arma el sobre comun de los eventos que publica Inventario: UUIDv7 nuevo y origen {@code inventario}. */
final class FabricaSobres {

    static final String ORIGEN = "inventario";

    private FabricaSobres() {
    }

    static SobreEvento de(UUID negocioId, Instant ocurridoEn, Instant publicadoEn, UUID correlacionId, UUID causaId,
                          String tipo, int version, Map<String, Object> datos) {
        return new SobreEvento(UuidV7.generar(publicadoEn), tipo, version, negocioId, ORIGEN, ocurridoEn,
                correlacionId, causaId, datos);
    }
}
