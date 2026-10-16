package mx.negocio360.ventas.aplicacion;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;

/** Arma el sobre comun de los eventos de Ventas: UUIDv7 nuevo, origen {@code ventas}, correlacion = la venta. */
final class FabricaSobres {

    static final String ORIGEN = "ventas";

    private FabricaSobres() {
    }

    static SobreEvento de(UUID negocioId, Instant ahora, UUID ventaId, String tipo, int version,
                          Map<String, Object> datos) {
        return new SobreEvento(UuidV7.generar(ahora), tipo, version, negocioId, ORIGEN, ahora, ventaId, null, datos);
    }
}
