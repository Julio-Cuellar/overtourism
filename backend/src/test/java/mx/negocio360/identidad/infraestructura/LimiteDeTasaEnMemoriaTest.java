package mx.negocio360.identidad.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** IDN-1.4 · AS-03: ventana deslizante de solicitudes, segura con varios hilos. */
class LimiteDeTasaEnMemoriaTest {

    private static final Instant T0 = Instant.parse("2026-10-14T16:00:00Z");
    private static final Duration VENTANA = Duration.ofMinutes(15);

    private final LimiteDeTasaEnMemoria limite = new LimiteDeTasaEnMemoria();

    @Test
    @DisplayName("permite hasta el maximo y rechaza el siguiente")
    void intentar_alLlegarAlMaximo_rechazaElSiguiente() {
        assertThat(limite.intentar("k", 3, VENTANA, T0)).isTrue();
        assertThat(limite.intentar("k", 3, VENTANA, T0)).isTrue();
        assertThat(limite.intentar("k", 3, VENTANA, T0)).isTrue();

        assertThat(limite.intentar("k", 3, VENTANA, T0)).isFalse();
    }

    @Test
    @DisplayName("la ventana desliza: justo al cumplirse 15 minutos de la primera, vuelve a permitir")
    void intentar_alCumplirseLaVentana_vuelveAPermitir() {
        limite.intentar("k", 3, VENTANA, T0);
        limite.intentar("k", 3, VENTANA, T0.plusSeconds(60));
        limite.intentar("k", 3, VENTANA, T0.plusSeconds(120));
        assertThat(limite.intentar("k", 3, VENTANA, T0.plus(VENTANA).minusSeconds(1))).isFalse();

        assertThat(limite.intentar("k", 3, VENTANA, T0.plus(VENTANA))).isTrue();
        assertThat(limite.intentar("k", 3, VENTANA, T0.plus(VENTANA))).isFalse();
    }

    @Test
    @DisplayName("los intentos rechazados no consumen cupo ni alargan el bloqueo")
    void intentar_rechazados_noAlarganElBloqueo() {
        for (var i = 0; i < 3; i++) {
            limite.intentar("k", 3, VENTANA, T0);
        }
        for (var i = 0; i < 10; i++) {
            assertThat(limite.intentar("k", 3, VENTANA, T0.plusSeconds(600))).isFalse();
        }

        assertThat(limite.intentar("k", 3, VENTANA, T0.plus(VENTANA))).isTrue();
    }

    @Test
    @DisplayName("las claves son independientes")
    void intentar_clavesDistintas_noSeAfectan() {
        for (var i = 0; i < 3; i++) {
            limite.intentar("a", 3, VENTANA, T0);
        }

        assertThat(limite.intentar("a", 3, VENTANA, T0)).isFalse();
        assertThat(limite.intentar("b", 3, VENTANA, T0)).isTrue();
    }

    @Test
    @DisplayName("con cien hilos y un maximo de 10, exactamente 10 pasan")
    void intentar_cienHilos_exactamenteElMaximo() throws Exception {
        List<Callable<Boolean>> tareas = IntStream.range(0, 100)
                .mapToObj(i -> (Callable<Boolean>) () -> limite.intentar("k", 10, VENTANA, T0)).toList();
        var pool = Executors.newFixedThreadPool(16);

        var resultados = pool.invokeAll(tareas);
        pool.shutdown();

        var permitidos = 0;
        for (var r : resultados) {
            if (r.get()) {
                permitidos++;
            }
        }
        assertThat(permitidos).isEqualTo(10);
    }

    @Test
    @DisplayName("un maximo o una ventana no positivos se rechazan")
    void intentar_parametrosInvalidos_lanzaExcepcion() {
        assertThatThrownBy(() -> limite.intentar("k", 0, VENTANA, T0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limite.intentar("k", 3, Duration.ZERO, T0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> limite.intentar(null, 3, VENTANA, T0)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("las claves sin actividad se olvidan: la memoria no crece sin fin")
    void intentar_clavesViejas_seOlvidan() {
        for (var i = 0; i < 500; i++) {
            limite.intentar("k" + i, 3, VENTANA, T0);
        }

        limite.intentar("nueva", 3, VENTANA, T0.plus(VENTANA).plusSeconds(1));

        assertThat(limite.clavesActivas()).isLessThanOrEqualTo(1);
    }
}
