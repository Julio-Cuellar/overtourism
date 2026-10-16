package mx.negocio360.negocios.aplicacion;

import java.time.Instant;
import java.util.UUID;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.negocios.contratos.NegocioRegistroSolicitadoV1;

/** Datos de prueba inventados para las pruebas de Negocios. */
final class Datos {

    static final Instant AHORA = Instant.parse("2026-10-16T16:00:00Z");

    private Datos() {
    }

    static UUID id() {
        return UuidV7.generar(AHORA);
    }

    static NegocioRegistroSolicitadoV1 pedido(UUID negocio, UUID cuenta, String nombre, String giro) {
        return new NegocioRegistroSolicitadoV1(negocio, cuenta, nombre, giro, id(), id());
    }

    static NegocioRegistroSolicitadoV1 pedidoValido() {
        return pedido(id(), id(), "Fonda Doña Chela", "RESTAURANTE");
    }
}
