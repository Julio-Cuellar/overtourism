package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import mx.negocio360.compartido.seguridad.LimitadorDeTasa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SG-10: el limitador de tasa. Una cubeta de fichas por clave que se rellena con el tiempo; acotado en memoria. */
class LimitadorDeTasaTest {

    private final RelojManual reloj = new RelojManual(Instant.parse("2026-10-16T12:00:00Z"));

    private LimitadorDeTasa limitador(int capacidad) {
        return new LimitadorDeTasa(capacidad, Duration.ofMinutes(1), 1_000, reloj);
    }

    @Test
    @DisplayName("deja pasar hasta la capacidad y niega la siguiente, diciendo cuanto esperar")
    void consumir_hastaLaCapacidad_yNiegaLaSiguiente() {
        var limitador = limitador(60);

        for (var i = 0; i < 60; i++) {
            assertThat(limitador.consumir("cuenta-1").permitido()).as("peticion %d", i + 1).isTrue();
        }
        var negada = limitador.consumir("cuenta-1");

        assertThat(negada.permitido()).isFalse();
        assertThat(negada.reintentarEnSegundos()).isEqualTo(1);
    }

    @Test
    @DisplayName("la cubeta se rellena con el tiempo: 60 por minuto devuelve una ficha por segundo")
    void consumir_tiempoDespues_recuperaFichas() {
        var limitador = limitador(60);
        for (var i = 0; i < 60; i++) {
            limitador.consumir("a");
        }

        reloj.avanzar(Duration.ofMillis(999));
        assertThat(limitador.consumir("a").permitido()).isFalse();
        reloj.avanzar(Duration.ofMillis(1));
        assertThat(limitador.consumir("a").permitido()).isTrue();
        assertThat(limitador.consumir("a").permitido()).isFalse();
    }

    @Test
    @DisplayName("nunca acumula mas que la capacidad, por mucho que pase el tiempo")
    void consumir_tiempoLargo_noPasaDeLaCapacidad() {
        var limitador = limitador(5);

        reloj.avanzar(Duration.ofHours(10));
        var permitidas = 0;
        for (var i = 0; i < 20; i++) {
            if (limitador.consumir("a").permitido()) {
                permitidas++;
            }
        }

        assertThat(permitidas).isEqualTo(5);
    }

    @Test
    @DisplayName("cada clave tiene su propia cubeta")
    void clavesIndependientes() {
        var limitador = limitador(2);
        limitador.consumir("a");
        limitador.consumir("a");

        assertThat(limitador.consumir("a").permitido()).isFalse();
        assertThat(limitador.consumir("b").permitido()).isTrue();
    }

    @Test
    @DisplayName("probar no consume ni crea la clave; sirve para saber si ya se agoto")
    void probar_noConsumeNiCrea() {
        var limitador = limitador(2);

        assertThat(limitador.probar("nueva").permitido()).isTrue();
        assertThat(limitador.claves()).isZero();

        limitador.consumir("a");
        limitador.consumir("a");
        var agotada = limitador.probar("a");
        assertThat(agotada.permitido()).isFalse();
        assertThat(agotada.reintentarEnSegundos()).isEqualTo(30);
        reloj.avanzar(Duration.ofSeconds(30));
        assertThat(limitador.probar("a").permitido()).isTrue();
        assertThat(limitador.probar("a").permitido()).isTrue();
    }

    @Test
    @DisplayName("la memoria esta acotada: al pasar el maximo de claves se descarta la menos usada")
    void maximoDeClaves_descartaLaMenosUsada() {
        var limitador = new LimitadorDeTasa(2, Duration.ofMinutes(1), 3, reloj);
        limitador.consumir("a");
        limitador.consumir("a");
        assertThat(limitador.consumir("a").permitido()).isFalse();

        limitador.consumir("b");
        limitador.consumir("c");
        limitador.consumir("d");

        assertThat(limitador.claves()).isEqualTo(3);
        assertThat(limitador.consumir("a").permitido()).as("a fue descartada y empieza de nuevo").isTrue();
    }

    @Test
    @DisplayName("con hilos en paralelo no se pasa de la capacidad ni se pierde ninguna ficha")
    void consumir_enParalelo_respetaLaCapacidad() throws Exception {
        var limitador = limitador(100);
        var ejecutor = Executors.newFixedThreadPool(8);
        try {
            var tareas = new ArrayList<Callable<Boolean>>();
            for (var i = 0; i < 400; i++) {
                tareas.add(() -> limitador.consumir("compartida").permitido());
            }
            var permitidas = ejecutor.invokeAll(tareas).stream().filter(futuro -> {
                try {
                    return futuro.get();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }).count();

            assertThat(permitidas).isEqualTo(100);
        } finally {
            ejecutor.shutdownNow();
        }
    }

    @Test
    @DisplayName("una configuracion imposible falla al construirse")
    void configuracionInvalida_falla() {
        assertThatThrownBy(() -> new LimitadorDeTasa(0, Duration.ofMinutes(1), 10, reloj))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LimitadorDeTasa(5, Duration.ZERO, 10, reloj))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LimitadorDeTasa(5, Duration.ofMinutes(1), 0, reloj))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
