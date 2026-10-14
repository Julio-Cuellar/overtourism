package mx.negocio360.compartido.identidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** R3: todo evento y toda entidad nueva lleva un UUIDv7 (RFC 9562), ordenable por tiempo. */
class UuidV7Test {

    private static final Instant AHORA = Instant.parse("2026-10-14T16:00:00Z");

    @Test
    @DisplayName("genera version 7 con variante RFC 4122")
    void generar_siempre_esVersion7ConVarianteRfc4122() {
        var id = UuidV7.generar(AHORA);

        assertThat(id.version()).isEqualTo(7);
        assertThat(id.variant()).isEqualTo(2);
    }

    @Test
    @DisplayName("guarda el instante en milisegundos en los primeros 48 bits")
    void generar_conInstante_loGuardaEnElId() {
        var id = UuidV7.generar(AHORA);

        assertThat(UuidV7.instanteDe(id)).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("un instante posterior produce un id mayor al comparar como texto")
    void generar_instanteMayor_produceIdMayor() {
        var antes = UuidV7.generar(AHORA);
        var despues = UuidV7.generar(AHORA.plusMillis(1));

        assertThat(despues.toString()).isGreaterThan(antes.toString());
    }

    @Test
    @DisplayName("dos ids del mismo milisegundo son distintos")
    void generar_mismoInstante_noRepiteIds() {
        var ids = new HashSet<UUID>();
        for (int i = 0; i < 1_000; i++) {
            ids.add(UuidV7.generar(AHORA));
        }

        assertThat(ids).hasSize(1_000);
    }

    @Test
    @DisplayName("el sobre de eventos lo acepta")
    void generar_idGenerado_loAceptaElSobreDeEventos() {
        var sobre = new SobreEvento(UuidV7.generar(AHORA), "cliente.registrado", 1, UUID.randomUUID(), "clientes",
                AHORA, UUID.randomUUID(), null, Map.of());

        assertThat(sobre.id().version()).isEqualTo(7);
    }

    @Test
    @DisplayName("instanteDe rechaza un id que no es v7")
    void instanteDe_idQueNoEsV7_lanzaIllegalArgument() {
        var v4 = UUID.randomUUID();

        assertThatThrownBy(() -> UuidV7.instanteDe(v4)).isInstanceOf(IllegalArgumentException.class);
    }
}
