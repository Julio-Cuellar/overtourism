package mx.negocio360.clientes.aplicacion;

import static mx.negocio360.clientes.fakes.Afirmaciones.fallaCon;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import mx.negocio360.clientes.dominio.CodigoError;
import mx.negocio360.clientes.dominio.FinalidadAviso;
import mx.negocio360.clientes.fakes.Escenario;
import mx.negocio360.clientes.fakes.Fixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** HTC-25 · L-01, L-02: PublicarAvisoPrivacidad con validacion y versionado. */
class PublicarAvisoTest {

    private final Escenario e = new Escenario();

    private static PublicarAvisoComando aviso(String responsable, String domicilio, List<FinalidadAviso> finalidades,
                                              String simplificado, String integral, String contacto) {
        return new PublicarAvisoComando(responsable, domicilio, finalidades, simplificado, integral, contacto);
    }

    static Stream<Arguments> avisosIncompletos() {
        var f = List.of(new FinalidadAviso("VENTA", false));
        return Stream.of(
                Arguments.of("sin responsable", aviso("  ", "Calle 1", f, "s", "i", "c")),
                Arguments.of("responsable de mas de 120", aviso("r".repeat(121), "Calle 1", f, "s", "i", "c")),
                Arguments.of("sin domicilio", aviso("Taller", "", f, "s", "i", "c")),
                Arguments.of("domicilio de mas de 200", aviso("Taller", "d".repeat(201), f, "s", "i", "c")),
                Arguments.of("sin finalidades", aviso("Taller", "Calle 1", List.of(), "s", "i", "c")),
                Arguments.of("finalidades nulas", aviso("Taller", "Calle 1", null, "s", "i", "c")),
                Arguments.of("finalidad sin codigo",
                        aviso("Taller", "Calle 1", List.of(new FinalidadAviso(" ", true)), "s", "i", "c")),
                Arguments.of("finalidad repetida", aviso("Taller", "Calle 1",
                        List.of(new FinalidadAviso("VENTA", false), new FinalidadAviso("venta", true)), "s", "i", "c")),
                Arguments.of("sin texto simplificado", aviso("Taller", "Calle 1", f, " ", "i", "c")),
                Arguments.of("sin texto integral", aviso("Taller", "Calle 1", f, "s", null, "c")),
                Arguments.of("texto integral enorme", aviso("Taller", "Calle 1", f, "s", "i".repeat(20_001), "c")),
                Arguments.of("sin contacto ARCO", aviso("Taller", "Calle 1", f, "s", "i", "")));
    }

    @Test
    @DisplayName("sin aviso vigente, registrar un cliente falla con AVISO_NO_VIGENTE")
    void registrarCliente_sinAvisoVigente_fallaConAvisoNoVigente() {
        fallaCon(() -> e.api.preparar(e.ctxA, Fixtures.registrar("Ana")), CodigoError.AVISO_NO_VIGENTE);
    }

    @Test
    @DisplayName("el primer aviso completo queda como version 1 vigente")
    void publicar_primerAviso_quedaComoVersion1Vigente() {
        var resultado = e.confirmar(e.ctxA, Fixtures.avisoValido());

        var vigente = e.api.avisoVigente(e.ctxA).orElseThrow();
        assertThat(resultado.codigo()).isEqualTo("OK");
        assertThat(resultado.recursoId()).isEqualTo(vigente.id());
        assertThat(vigente.version()).isEqualTo(1);
        assertThat(vigente.vigente()).isTrue();
        assertThat(vigente.vigenteDesde()).isEqualTo(Fixtures.AHORA);
        assertThat(vigente.publicadoPor()).isEqualTo("locatario-a");
        assertThat(vigente.responsableNombre()).isEqualTo("Taller Mixcoatl");
        assertThat(vigente.finalidades()).hasSize(2);
        assertThat(vigente.negocioId()).isEqualTo(e.negocioA);
    }

    @Test
    @DisplayName("un aviso nuevo crea la version 2 y cierra la 1 sin cambiar su contenido")
    void publicar_segundoAviso_creaVersion2YCierraLaAnterior() {
        e.conAviso(e.ctxA);
        var primero = e.api.avisoVigente(e.ctxA).orElseThrow();
        e.reloj.avanzar(Duration.ofHours(1));

        e.confirmar(e.ctxA, aviso("Taller Mixcoatl", "Calle 9, Puebla", List.of(new FinalidadAviso("VENTA", false)),
                "nuevo simplificado", "nuevo integral", "privacidad@taller.test"));

        var historial = e.avisos.historial(e.negocioA);
        assertThat(historial).extracting("version").containsExactly(1, 2);
        assertThat(historial.get(0).vigenteHasta()).isEqualTo(Fixtures.AHORA.plus(Duration.ofHours(1)));
        assertThat(historial.get(0)).usingRecursiveComparison().ignoringFields("vigenteHasta").isEqualTo(primero);
        assertThat(historial.get(1).vigente()).isTrue();
        assertThat(e.api.avisoVigente(e.ctxA).orElseThrow().version()).isEqualTo(2);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("avisosIncompletos")
    @DisplayName("un aviso al que le falta algo falla con AVISO_INCOMPLETO al preparar")
    void preparar_avisoIncompleto_fallaConAvisoIncompleto(String caso, PublicarAvisoComando comando) {
        fallaCon(() -> e.api.preparar(e.ctxA, comando), CodigoError.AVISO_INCOMPLETO);

        assertThat(e.preparaciones.todas()).isEmpty();
    }

    @Test
    @DisplayName("preparar no publica nada")
    void preparar_avisoValido_noEscribeNada() {
        e.api.preparar(e.ctxA, Fixtures.avisoValido());

        assertThat(e.api.avisoVigente(e.ctxA)).isEmpty();
        assertThat(e.avisos.historial(e.negocioA)).isEmpty();
    }

    @Test
    @DisplayName("el aviso de un negocio no vale para otro")
    void avisoVigente_otroNegocio_noLoVe() {
        e.conAviso(e.ctxA);

        assertThat(e.api.avisoVigente(e.ctxB)).isEmpty();
        fallaCon(() -> e.api.preparar(e.ctxB, Fixtures.registrar("Ana")), CodigoError.AVISO_NO_VIGENTE);
        assertThat(e.api.preparar(e.ctxA, Fixtures.registrar("Ana")).token()).isNotBlank();
    }

    @Test
    @DisplayName("publicar un aviso no emite eventos")
    void publicar_aviso_noPublicaEventos() {
        e.conAviso(e.ctxA);

        assertThat(e.eventos.todos()).isEmpty();
    }

    @Test
    @DisplayName("la version se calcula al confirmar: dos avisos preparados juntos se publican como 1 y 2")
    void publicar_dosPreparadosJuntos_seNumeranAlConfirmar() {
        var primera = e.api.preparar(e.ctxA, Fixtures.avisoValido());
        var segunda = e.api.preparar(e.ctxA, Fixtures.avisoValido());

        e.api.confirmar(e.ctxA, primera.token());
        e.api.confirmar(e.ctxA, segunda.token());

        assertThat(e.avisos.historial(e.negocioA)).extracting("version").containsExactly(1, 2);
        assertThat(e.avisos.historial(e.negocioA).get(0).vigente()).isFalse();
    }
}
