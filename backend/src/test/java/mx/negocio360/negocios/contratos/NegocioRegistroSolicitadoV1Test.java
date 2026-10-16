package mx.negocio360.negocios.contratos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Lo que Negocios lee de {@code negocio.registro_solicitado} v1 (B1, R5): lector tolerante. */
class NegocioRegistroSolicitadoV1Test {

    private static final Instant AHORA = Instant.parse("2026-10-16T10:00:00Z");
    private static final UUID NEGOCIO = UUID.fromString("0192f39f-3333-7000-8000-00000000f002");
    private static final UUID CUENTA = UUID.fromString("0192f39f-3333-7000-8000-00000000f001");

    private static SobreEvento sobre(String tipo, int version, String origen, Map<String, Object> datos) {
        return new SobreEvento(UuidV7.generar(AHORA), tipo, version, NEGOCIO, origen, AHORA, UuidV7.generar(AHORA), null,
                datos);
    }

    private static Map<String, Object> datosValidos() {
        return new HashMap<>(Map.of("cuenta_id", CUENTA.toString(), "nombre", "Fonda Doña Chela", "giro", "RESTAURANTE"));
    }

    @Test
    @DisplayName("lee el pedido valido: negocio del sobre, cuenta, nombre, giro y ids del evento")
    void lee_unPedidoValido() {
        var sobre = sobre("negocio.registro_solicitado", 1, "identidad", datosValidos());

        var pedido = NegocioRegistroSolicitadoV1.de(sobre);

        assertThat(pedido).isPresent();
        assertThat(pedido.get().negocioId()).isEqualTo(NEGOCIO);
        assertThat(pedido.get().cuentaId()).isEqualTo(CUENTA);
        assertThat(pedido.get().nombre()).isEqualTo("Fonda Doña Chela");
        assertThat(pedido.get().giro()).isEqualTo("RESTAURANTE");
        assertThat(pedido.get().eventoId()).isEqualTo(sobre.id());
        assertThat(pedido.get().correlacionId()).isEqualTo(sobre.correlacionId());
    }

    @Test
    @DisplayName("R5: ignora los campos que no conoce")
    void ignora_camposNuevos() {
        var datos = datosValidos();
        datos.put("campo_futuro", "x");

        assertThat(NegocioRegistroSolicitadoV1.de(sobre("negocio.registro_solicitado", 1, "identidad", datos))).isPresent();
    }

    @Test
    @DisplayName("EV-03: otro tipo, otra version u otro origen devuelve vacio")
    void descarta_loQueNoEntiende() {
        assertThat(NegocioRegistroSolicitadoV1.de(sobre("negocio.registrado", 1, "identidad", datosValidos()))).isEmpty();
        assertThat(NegocioRegistroSolicitadoV1.de(sobre("negocio.registro_solicitado", 9, "identidad", datosValidos())))
                .isEmpty();
        assertThat(NegocioRegistroSolicitadoV1.de(sobre("negocio.registro_solicitado", 1, "clientes", datosValidos())))
                .isEmpty();
    }

    @Test
    @DisplayName("EV-03: datos mal formados devuelven vacio, sin lanzar")
    void descarta_datosMalFormados() {
        var sinNombre = datosValidos();
        sinNombre.remove("nombre");
        var cuentaRota = datosValidos();
        cuentaRota.put("cuenta_id", "no-es-uuid");
        var giroNumerico = datosValidos();
        giroNumerico.put("giro", 7);

        assertThat(NegocioRegistroSolicitadoV1.de(sobre("negocio.registro_solicitado", 1, "identidad", sinNombre))).isEmpty();
        assertThat(NegocioRegistroSolicitadoV1.de(sobre("negocio.registro_solicitado", 1, "identidad", cuentaRota))).isEmpty();
        assertThat(NegocioRegistroSolicitadoV1.de(sobre("negocio.registro_solicitado", 1, "identidad", giroNumerico))).isEmpty();
    }

    @Test
    @DisplayName("toString no imprime el nombre del negocio ni la cuenta")
    void toString_noImprimeDatos() {
        var pedido = NegocioRegistroSolicitadoV1.de(sobre("negocio.registro_solicitado", 1, "identidad", datosValidos())).get();

        assertThat(pedido.toString()).doesNotContain("Doña Chela").doesNotContain(CUENTA.toString());
    }
}
