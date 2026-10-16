package mx.negocio360.identidad.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioPreregistros;
import mx.negocio360.identidad.dominio.Preregistro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.annotation.ScheduledAnnotationBeanPostProcessor;

/**
 * D-84 · con la aplicacion completa, la limpieza esta programada y borra de verdad en el Postgres de pruebas: un
 * pre-registro vencido desaparece.
 */
@SpringBootTest
class LimpiezaProgramadaTest {

    @Autowired
    private ScheduledAnnotationBeanPostProcessor programador;

    @Autowired
    private LimpiezaDePreregistros limpieza;

    @Autowired
    private RepositorioPreregistros preregistros;

    @Test
    @DisplayName("la limpieza de pre-registros vencidos esta programada para correr sola")
    void limpieza_estaProgramada() {
        assertThat(programador.getScheduledTasks()).anySatisfy(tarea ->
                assertThat(tarea.toString()).contains("LimpiezaDePreregistros.borrarVencidos"));
    }

    @Test
    @DisplayName("al correr, borra de Postgres un pre-registro vencido y deja uno vigente")
    void limpieza_borraElVencidoDePostgres() {
        var vencido = Preregistro.nuevo(UUID.randomUUID(), "vencido-" + UUID.randomUUID() + "@example.com",
                "+522221110001", "Ana", "López", 1, null, null, "123456", Instant.parse("2020-01-01T00:00:00Z"),
                Duration.ofMinutes(15));
        var vigente = Preregistro.nuevo(UUID.randomUUID(), "vigente-" + UUID.randomUUID() + "@example.com",
                "+522221110001", "Ana", "López", 1, null, null, "123456", Instant.now(), Duration.ofMinutes(15));
        preregistros.guardar(vencido);
        preregistros.guardar(vigente);

        limpieza.borrarVencidos();

        assertThat(preregistros.porCorreo(vencido.correo())).isEmpty();
        assertThat(preregistros.porCorreo(vigente.correo())).isPresent();
        preregistros.eliminar(vigente.id());
    }
}
