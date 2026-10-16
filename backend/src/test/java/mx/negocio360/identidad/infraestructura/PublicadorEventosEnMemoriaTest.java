package mx.negocio360.identidad.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** El publicador de desarrollo guarda en memoria y no crece sin limite (revision de seguridad, 2026-10-15). */
class PublicadorEventosEnMemoriaTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T16:00:00Z");

    private static SobreEvento evento(int numero) {
        return new SobreEvento(UuidV7.generar(AHORA), "prueba.ocurrio", 1, UuidV7.generar(AHORA), "identidad", AHORA,
                UuidV7.generar(AHORA), null, Map.of("numero", numero));
    }

    @Test
    @DisplayName("guarda los eventos en el orden en que se publicaron")
    void publicar_guardaEnOrden() {
        var publicador = new PublicadorEventosEnMemoria();

        publicador.publicar(evento(1));
        publicador.publicar(evento(2));

        assertThat(publicador.eventos()).extracting(e -> e.datos().get("numero")).containsExactly(1, 2);
    }

    @Test
    @DisplayName("solo conserva los ultimos 1000: el mas antiguo se descarta")
    void publicar_conservaSoloLosUltimosMil() {
        var publicador = new PublicadorEventosEnMemoria();
        for (var i = 1; i <= 1_001; i++) {
            publicador.publicar(evento(i));
        }

        assertThat(publicador.eventos()).hasSize(1_000);
        assertThat(publicador.eventos().get(0).datos()).containsEntry("numero", 2);
        assertThat(publicador.eventos().get(999).datos()).containsEntry("numero", 1_001);
    }

    @Test
    @DisplayName("limpiar vacia la lista")
    void limpiar_vacia() {
        var publicador = new PublicadorEventosEnMemoria();
        publicador.publicar(evento(1));

        publicador.limpiar();

        assertThat(publicador.eventos()).isEmpty();
    }
}
