package mx.negocio360.negocios.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.negocios.AltaVista;
import mx.negocio360.negocios.aplicacion.AltasApi;
import mx.negocio360.negocios.contratos.NegocioRegistroSolicitadoV1;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** El listener solo actua con negocio.registro_solicitado v1 de Identidad y descarta lo demas sin error (EV-03). */
class EscuchaDeIdentidadTest {

    private static final Instant AHORA = Instant.parse("2026-10-16T16:00:00Z");

    private final List<NegocioRegistroSolicitadoV1> recibidos = new ArrayList<>();
    private final AltasApi altas = new AltasApi() {
        @Override
        public ResultadoAlta registrarAlta(NegocioRegistroSolicitadoV1 pedido) {
            recibidos.add(pedido);
            return ResultadoAlta.REGISTRADO;
        }

        @Override
        public List<AltaVista> misAltas(UUID cuentaId) {
            return List.of();
        }
    };
    private final EscuchaDeIdentidad escucha = new EscuchaDeIdentidad(altas);

    private static SobreEvento sobre(String tipo, String origen, Map<String, Object> datos) {
        return new SobreEvento(UuidV7.generar(AHORA), tipo, 1, UuidV7.generar(AHORA), origen, AHORA,
                UuidV7.generar(AHORA), null, datos);
    }

    @Test
    @DisplayName("un pedido de alta valido llega al servicio")
    void pedidoValido_llegaAlServicio() {
        escucha.alRecibir(sobre("negocio.registro_solicitado", "identidad",
                Map.of("cuenta_id", UUID.randomUUID().toString(), "nombre", "Fonda", "giro", "RESTAURANTE")));

        assertThat(recibidos).hasSize(1);
    }

    @Test
    @DisplayName("otros eventos y pedidos mal formados se descartan sin error")
    void loDemas_seDescarta() {
        escucha.alRecibir(sobre("negocio.registrado", "negocios", Map.of("nombre", "x")));
        escucha.alRecibir(sobre("negocio.registro_solicitado", "identidad", Map.of("nombre", "sin cuenta")));
        escucha.alRecibir(sobre("negocio.registro_solicitado", "clientes",
                Map.of("cuenta_id", UUID.randomUUID().toString(), "nombre", "Fonda", "giro", "MUSEO")));

        assertThat(recibidos).isEmpty();
    }
}
