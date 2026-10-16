package mx.negocio360.identidad.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.util.ArrayList;
import java.util.List;
import mx.negocio360.identidad.aplicacion.puertos.PublicadorEventos;

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

    @Test
    @DisplayName("con destino, reenvia cada evento al bus y ademas lo guarda para el simulador")
    void publicar_conDestino_reenviaYGuarda() {
        var reenviados = new ArrayList<SobreEvento>();
        PublicadorEventos destino = reenviados::add;
        var publicador = new PublicadorEventosEnMemoria(destino);
        var evento = evento(1);

        publicador.publicar(evento);

        assertThat(reenviados).containsExactly(evento);
        assertThat(publicador.eventos()).containsExactly(evento);
    }

    @Test
    @DisplayName("si el destino rechaza el evento, no se guarda: el simulador solo ve lo que de verdad se publico")
    void publicar_siElDestinoFalla_noGuarda() {
        PublicadorEventos destino = evento -> {
            throw new IllegalStateException("sin transaccion");
        };
        var publicador = new PublicadorEventosEnMemoria(destino);

        assertThatThrownBy(() -> publicador.publicar(evento(1))).isInstanceOf(IllegalStateException.class);

        assertThat(publicador.eventos()).isEmpty();
    }
}
