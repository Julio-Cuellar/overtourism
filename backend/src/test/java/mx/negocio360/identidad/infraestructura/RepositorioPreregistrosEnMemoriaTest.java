package mx.negocio360.identidad.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import mx.negocio360.identidad.dominio.Preregistro;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** El almacen de pre-registros en memoria cumple el mismo contrato que el de Postgres (D-84, D-86). */
class RepositorioPreregistrosEnMemoriaTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T16:00:00Z");
    private static final Duration VIGENCIA = Duration.ofMinutes(15);

    private final RepositorioPreregistrosEnMemoria repositorio = new RepositorioPreregistrosEnMemoria();

    private static Preregistro de(String correo, Instant creado) {
        return Preregistro.nuevo(UUID.randomUUID(), correo, "+522221110001", "Ana", "López", 1, null, null, "123456",
                creado, VIGENCIA);
    }

    @Test
    @DisplayName("lo guardado se busca por correo; un correo desconocido no devuelve nada")
    void guardar_yBuscarPorCorreo() {
        var pre = de("ana@example.com", AHORA);

        repositorio.guardar(pre);

        assertThat(repositorio.porCorreo("ana@example.com")).contains(pre);
        assertThat(repositorio.porCorreo("otra@example.com")).isEmpty();
    }

    @Test
    @DisplayName("guardar con el mismo correo reemplaza al anterior: hay un pre-registro por correo")
    void guardar_mismoCorreo_reemplaza() {
        var primero = de("ana@example.com", AHORA);
        var segundo = de("ana@example.com", AHORA.plusSeconds(60));
        repositorio.guardar(primero);

        repositorio.guardar(segundo);

        assertThat(repositorio.porCorreo("ana@example.com")).contains(segundo);
        assertThat(repositorio.todos()).hasSize(1);
        assertThat(repositorio.eliminar(primero.id())).as("el viejo ya no existe").isFalse();
    }

    @Test
    @DisplayName("eliminar dice si borro algo: solo la primera de dos eliminaciones gana")
    void eliminar_soloUnaVezGana() {
        var pre = de("ana@example.com", AHORA);
        repositorio.guardar(pre);

        assertThat(repositorio.eliminar(pre.id())).isTrue();
        assertThat(repositorio.eliminar(pre.id())).isFalse();
        assertThat(repositorio.porCorreo("ana@example.com")).isEmpty();
    }

    @Test
    @DisplayName("borrarVencidos quita los que ya vencieron, incluido el que vence justo ahora, y cuenta cuantos")
    void borrarVencidos_quitaSoloLosVencidos() {
        repositorio.guardar(de("viejo@example.com", AHORA.minus(Duration.ofMinutes(20))));
        repositorio.guardar(de("justo@example.com", AHORA.minus(VIGENCIA)));
        repositorio.guardar(de("vigente@example.com", AHORA.minusSeconds(60)));

        var borrados = repositorio.borrarVencidos(AHORA);

        assertThat(borrados).isEqualTo(2);
        assertThat(repositorio.porCorreo("vigente@example.com")).isPresent();
        assertThat(repositorio.porCorreo("viejo@example.com")).isEmpty();
        assertThat(repositorio.porCorreo("justo@example.com")).isEmpty();
    }
}
