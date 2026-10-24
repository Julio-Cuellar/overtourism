package mx.negocio360.inventario.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import mx.negocio360.inventario.aplicacion.Preparacion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** D-23: un token se usa una sola vez, solo se guarda su hash, y es de un negocio (otro negocio no lo ve ni lo gasta). */
class PreparacionesEnMemoriaTest {

    private static final Instant AHORA = Instant.parse("2026-10-16T12:00:00Z");
    private static final UUID NEGOCIO = UUID.randomUUID();
    private final PreparacionesEnMemoria tabla = new PreparacionesEnMemoria();

    private Preparacion fila(String hash) {
        return new Preparacion(hash, NEGOCIO, "actor", "registrar_entrada", "hash-comando", "{}", "resumen",
                AHORA.plusSeconds(300), null);
    }

    @Test
    @DisplayName("guarda, encuentra por hash y marca como usada una sola vez")
    void guardar_encontrar_usar() {
        tabla.guardar(fila("h1"));

        assertThat(tabla.porHash(NEGOCIO, "h1")).isPresent();
        assertThat(tabla.porHash(NEGOCIO, "otro")).isEmpty();
        assertThat(tabla.marcarUsada(NEGOCIO, "h1", AHORA)).isTrue();
        assertThat(tabla.marcarUsada(NEGOCIO, "h1", AHORA)).isFalse();
        assertThat(tabla.porHash(NEGOCIO, "h1").orElseThrow().usadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("marcar como usada una preparacion que no existe responde falso")
    void usar_inexistente() {
        assertThat(tabla.marcarUsada(NEGOCIO, "no-existe", AHORA)).isFalse();
    }

    @Test
    @DisplayName("otro negocio no ve la preparacion ni puede gastar su token")
    void otroNegocioNoLaVeNiLaGasta() {
        tabla.guardar(fila("h1"));
        var ajeno = UUID.randomUUID();

        assertThat(tabla.porHash(ajeno, "h1")).isEmpty();
        assertThat(tabla.marcarUsada(ajeno, "h1", AHORA)).isFalse();
        assertThat(tabla.porHash(NEGOCIO, "h1").orElseThrow().usadoEn()).isNull();
    }
}
