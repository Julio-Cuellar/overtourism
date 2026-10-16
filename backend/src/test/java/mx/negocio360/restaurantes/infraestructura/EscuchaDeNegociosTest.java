package mx.negocio360.restaurantes.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.restaurantes.aplicacion.SeguimientoDeNegocios;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** La escucha del bus solo le pasa el evento al seguimiento y no lanza nunca, aunque el evento venga mal. */
class EscuchaDeNegociosTest {

    private static final UUID NEGOCIO = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000a");
    private static final Instant T0 = Instant.parse("2026-10-16T10:00:00Z");

    private final RestaurantesEnMemoria restaurantes = new RestaurantesEnMemoria();
    private final EscuchaDeNegocios escucha = new EscuchaDeNegocios(new SeguimientoDeNegocios(restaurantes));

    private static SobreEvento evento(Map<String, Object> datos) {
        return new SobreEvento(UuidV7.generar(T0), "negocio.registrado", 1, NEGOCIO, "negocios", T0, NEGOCIO, null, datos);
    }

    @Test
    @DisplayName("un negocio.registrado de un restaurante crea su extension")
    void recibe() {
        escucha.alRecibir(evento(Map.of("giro", "RESTAURANTE")));

        assertThat(restaurantes.porNegocio(NEGOCIO)).isPresent();
    }

    @Test
    @DisplayName("un evento mal formado no lanza ni cambia nada")
    void malFormado() {
        escucha.alRecibir(evento(Map.of("otro", "dato")));

        assertThat(restaurantes.porNegocio(NEGOCIO)).isEmpty();
    }
}
