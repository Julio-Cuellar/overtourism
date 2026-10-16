package mx.negocio360.identidad.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioPreregistros;
import mx.negocio360.identidad.dominio.Preregistro;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

/** D-84 · la limpieza borra los pre-registros vencidos y un fallo suyo ni se propaga ni escribe datos personales. */
class LimpiezaDePreregistrosTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T16:00:00Z");
    private static final Duration VIGENCIA = Duration.ofMinutes(15);
    private static final Clock RELOJ = Clock.fixed(AHORA, ZoneOffset.UTC);

    private Logger registro;
    private ListAppender<ILoggingEvent> eventos;

    @BeforeEach
    void conectar() {
        registro = (Logger) LoggerFactory.getLogger(LimpiezaDePreregistros.class);
        eventos = new ListAppender<>();
        eventos.start();
        registro.addAppender(eventos);
    }

    @AfterEach
    void desconectar() {
        registro.detachAppender(eventos);
    }

    private static Preregistro de(String correo, Instant creado) {
        return Preregistro.nuevo(UUID.randomUUID(), correo, "+522221110001", "Ana", "López", 1, null, null, "123456",
                creado, VIGENCIA);
    }

    @Test
    @DisplayName("borra los vencidos a la hora del reloj y deja los vigentes")
    void borrarVencidos_quitaLosVencidos() {
        var repositorio = new RepositorioPreregistrosEnMemoria();
        repositorio.guardar(de("viejo@example.com", AHORA.minus(Duration.ofMinutes(20))));
        repositorio.guardar(de("vigente@example.com", AHORA.minusSeconds(60)));

        new LimpiezaDePreregistros(repositorio, RELOJ).borrarVencidos();

        assertThat(repositorio.porCorreo("viejo@example.com")).isEmpty();
        assertThat(repositorio.porCorreo("vigente@example.com")).isPresent();
    }

    @Test
    @DisplayName("cuando borra algo lo cuenta en el registro, sin correos ni nombres")
    void borrarVencidos_registraCuantosBorro() {
        var repositorio = new RepositorioPreregistrosEnMemoria();
        repositorio.guardar(de("viejo@example.com", AHORA.minus(Duration.ofMinutes(20))));

        new LimpiezaDePreregistros(repositorio, RELOJ).borrarVencidos();

        assertThat(eventos.list).singleElement().satisfies(evento -> {
            assertThat(evento.getLevel()).isEqualTo(Level.INFO);
            assertThat(evento.getFormattedMessage()).contains("1").doesNotContain("viejo@example.com");
        });
    }

    @Test
    @DisplayName("si no hay nada que borrar no escribe nada")
    void borrarVencidos_sinNada_noEscribe() {
        new LimpiezaDePreregistros(new RepositorioPreregistrosEnMemoria(), RELOJ).borrarVencidos();

        assertThat(eventos.list).isEmpty();
    }

    @Test
    @DisplayName("un fallo del repositorio no se propaga (para que las siguientes pasadas corran) y no escribe su mensaje")
    void borrarVencidos_fallo_noSePropagaNiFiltraElMensaje() {
        RepositorioPreregistros roto = new RepositorioPreregistros() {
            @Override
            public void guardar(Preregistro preregistro) {
            }

            @Override
            public Optional<Preregistro> porCorreo(String correo) {
                return Optional.empty();
            }

            @Override
            public boolean eliminar(UUID id) {
                return false;
            }

            @Override
            public int borrarVencidos(Instant ahora) {
                throw new IllegalStateException("fallo con ana@example.com");
            }
        };

        assertThatCode(() -> new LimpiezaDePreregistros(roto, RELOJ).borrarVencidos()).doesNotThrowAnyException();

        assertThat(eventos.list).singleElement().satisfies(evento -> {
            assertThat(evento.getLevel()).isEqualTo(Level.WARN);
            assertThat(evento.getFormattedMessage()).contains("IllegalStateException").doesNotContain("ana@example.com");
        });
    }
}
