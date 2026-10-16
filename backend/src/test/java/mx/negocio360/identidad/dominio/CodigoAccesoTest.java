package mx.negocio360.identidad.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** IDN-1.3 · AS-01, AS-02: codigo de un solo uso, que vence a los 5 minutos y se agota a los 5 intentos. */
class CodigoAccesoTest {

    private static final Instant AHORA = Instant.parse("2026-10-14T16:00:00Z");
    private static final Duration VIGENCIA = Duration.ofMinutes(5);
    private static final String CORREO = "ana@example.com";

    private CodigoAcceso nuevo() {
        return CodigoAcceso.emitir(UUID.randomUUID(), CORREO, "482913", AHORA, VIGENCIA);
    }

    @Test
    @DisplayName("emitir fija el vencimiento a 5 minutos, cero intentos y sin uso")
    void emitir_codigoNuevo_fijaVencimientoIntentosYUso() {
        var codigo = nuevo();

        assertThat(codigo.expiraEn()).isEqualTo(AHORA.plus(VIGENCIA));
        assertThat(codigo.intentos()).isZero();
        assertThat(codigo.usadoEn()).isNull();
        assertThat(codigo.destino()).isEqualTo(CORREO);
        assertThat(codigo.creadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("coincide con el codigo emitido y con ningun otro")
    void coincide_codigoEmitidoYOtro_aceptaSoloElEmitido() {
        var codigo = nuevo();

        assertThat(codigo.coincide("482913")).isTrue();
        assertThat(codigo.coincide("482914")).isFalse();
        assertThat(codigo.coincide(null)).isFalse();
    }

    @Test
    @DisplayName("vence exactamente a los 5 minutos")
    void vencido_alCumplirseLaVigencia_esVerdadero() {
        var codigo = nuevo();

        assertThat(codigo.vencido(AHORA.plus(VIGENCIA).minusSeconds(1))).isFalse();
        assertThat(codigo.vencido(AHORA.plus(VIGENCIA))).isTrue();
    }

    @Test
    @DisplayName("un intento fallido suma uno y devuelve otro codigo sin mutar el original")
    void conIntentoFallido_suma1SinMutarLaOriginal() {
        var original = nuevo();

        var conIntento = original.conIntentoFallido();

        assertThat(conIntento.intentos()).isEqualTo(1);
        assertThat(original.intentos()).isZero();
    }

    @Test
    @DisplayName("se agota al llegar al maximo de intentos")
    void agotado_alLlegarAlMaximo_esVerdadero() {
        var codigo = nuevo();
        for (var i = 0; i < 4; i++) {
            codigo = codigo.conIntentoFallido();
        }
        assertThat(codigo.agotado(5)).isFalse();

        assertThat(codigo.conIntentoFallido().agotado(5)).isTrue();
    }

    @Test
    @DisplayName("usado marca el momento de uso y no muta el original")
    void usado_marcaElMomentoSinMutarLaOriginal() {
        var original = nuevo();

        var usado = original.usado(AHORA.plusSeconds(30));

        assertThat(usado.usadoEn()).isEqualTo(AHORA.plusSeconds(30));
        assertThat(original.usadoEn()).isNull();
    }

    @Test
    @DisplayName("toString no revela el correo ni el hash del codigo")
    void toString_nuncaRevelaCorreoNiHash() {
        var codigo = nuevo();

        assertThat(codigo.toString()).doesNotContain("ana@example.com").doesNotContain("482913");
    }
}
