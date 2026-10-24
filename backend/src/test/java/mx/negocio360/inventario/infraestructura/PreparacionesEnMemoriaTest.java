package mx.negocio360.inventario.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import mx.negocio360.inventario.aplicacion.Preparacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** D-23: un token se usa una sola vez, y solo se guarda su hash. */
class PreparacionesEnMemoriaTest {

    private static final Instant AHORA = Instant.parse("2026-10-16T12:00:00Z");
    private final PreparacionesEnMemoria tabla = new PreparacionesEnMemoria();

    private Preparacion fila(String hash) {
        return new Preparacion(hash, UUID.randomUUID(), "actor", "registrar_entrada", "hash-comando", "{}", "resumen",
                AHORA.plusSeconds(300), null);
    }

    @Test
    @DisplayName("guarda, encuentra por hash y marca como usada una sola vez")
    void guardar_encontrar_usar() {
        tabla.guardar(fila("h1"));

        assertThat(tabla.porHash("h1")).isPresent();
        assertThat(tabla.porHash("otro")).isEmpty();
        assertThat(tabla.marcarUsada("h1", AHORA)).isTrue();
        assertThat(tabla.marcarUsada("h1", AHORA)).isFalse();
        assertThat(tabla.porHash("h1").orElseThrow().usadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("marcar como usada una preparacion que no existe responde falso")
    void usar_inexistente() {
        assertThat(tabla.marcarUsada("no-existe", AHORA)).isFalse();
    }
}
