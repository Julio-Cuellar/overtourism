package mx.negocio360.negocios.contratos;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Lo que Negocios lee de {@code identidad.membresia_agregada} v1 (B2): solo la cuenta y de donde viene. */
class MembresiaAgregadaV1Test {

    private static final Instant AHORA = Instant.parse("2026-10-16T16:00:00Z");
    private static final UUID CUENTA = UUID.fromString("0192f39f-3333-7000-8000-00000000f001");

    private static SobreEvento sobre(String tipo, int version, String origen, Map<String, Object> datos) {
        return new SobreEvento(UuidV7.generar(AHORA), tipo, version, UuidV7.generar(AHORA), origen, AHORA,
                UuidV7.generar(AHORA), null, datos);
    }

    @Test
    @DisplayName("lee la cuenta y el origen, e ignora el rol y los campos nuevos")
    void lee_cuentaYOrigen() {
        var sobre = sobre("identidad.membresia_agregada", 1, "identidad",
                Map.of("cuenta_id", CUENTA.toString(), "rol", "DUENO", "origen", "DUENO_INICIAL", "futuro", "x"));

        var membresia = MembresiaAgregadaV1.de(sobre).orElseThrow();

        assertThat(membresia.cuentaId()).isEqualTo(CUENTA);
        assertThat(membresia.esDuenoInicial()).isTrue();
        assertThat(membresia.negocioId()).isEqualTo(sobre.negocioId());
    }

    @Test
    @DisplayName("una invitacion aceptada no es dueno inicial")
    void invitacion_noEsDuenoInicial() {
        var sobre = sobre("identidad.membresia_agregada", 1, "identidad",
                Map.of("cuenta_id", CUENTA.toString(), "rol", "CAJERO", "origen", "INVITACION"));

        assertThat(MembresiaAgregadaV1.de(sobre).orElseThrow().esDuenoInicial()).isFalse();
    }

    @Test
    @DisplayName("EV-03: otro tipo, version u origen, o datos mal formados, devuelve vacio")
    void descarta_loQueNoEntiende() {
        var datos = Map.<String, Object>of("cuenta_id", CUENTA.toString(), "origen", "DUENO_INICIAL");

        assertThat(MembresiaAgregadaV1.de(sobre("identidad.membresia_revocada", 1, "identidad", datos))).isEmpty();
        assertThat(MembresiaAgregadaV1.de(sobre("identidad.membresia_agregada", 2, "identidad", datos))).isEmpty();
        assertThat(MembresiaAgregadaV1.de(sobre("identidad.membresia_agregada", 1, "negocios", datos))).isEmpty();
        assertThat(MembresiaAgregadaV1.de(sobre("identidad.membresia_agregada", 1, "identidad",
                Map.of("origen", "DUENO_INICIAL")))).isEmpty();
        assertThat(MembresiaAgregadaV1.de(sobre("identidad.membresia_agregada", 1, "identidad",
                Map.of("cuenta_id", "no-es-uuid", "origen", "DUENO_INICIAL")))).isEmpty();
    }
}
