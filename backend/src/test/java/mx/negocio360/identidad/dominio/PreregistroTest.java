package mx.negocio360.identidad.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * D-84 · el pre-registro es el formulario de una persona que todavia no tiene cuenta, con el codigo que se le mando
 * al correo. Vive 15 minutos, admite 5 intentos y solo guarda el hash del codigo.
 */
class PreregistroTest {

    private static final Instant AHORA = Instant.parse("2026-10-15T16:00:00Z");
    private static final Duration VIGENCIA = Duration.ofMinutes(15);
    private static final UUID ID = UUID.fromString("0192f39f-3333-7000-8000-00000000f001");

    private static Preregistro nuevo(String negocioNombre, String negocioGiro) {
        return Preregistro.nuevo(ID, "ana@example.com", "+522221110001", "Ana", "López Pérez", 1, negocioNombre,
                negocioGiro, "123456", AHORA, VIGENCIA);
    }

    private static Preregistro sinNegocio() {
        return nuevo(null, null);
    }

    @Test
    @DisplayName("nuevo guarda los datos del formulario, acepta el aviso en ese momento y vence a la vigencia")
    void nuevo_guardaLosDatosYElVencimiento() {
        var pre = nuevo("Fonda Doña Chela", "RESTAURANTE");

        assertThat(pre.id()).isEqualTo(ID);
        assertThat(pre.correo()).isEqualTo("ana@example.com");
        assertThat(pre.telefono()).isEqualTo("+522221110001");
        assertThat(pre.nombre()).isEqualTo("Ana");
        assertThat(pre.apellidos()).isEqualTo("López Pérez");
        assertThat(pre.avisoVersion()).isEqualTo(1);
        assertThat(pre.avisoAceptadoEn()).isEqualTo(AHORA);
        assertThat(pre.creadoEn()).isEqualTo(AHORA);
        assertThat(pre.venceEn()).isEqualTo(AHORA.plus(VIGENCIA));
        assertThat(pre.intentos()).isZero();
        assertThat(pre.pideNegocio()).isTrue();
        assertThat(pre.negocioNombre()).isEqualTo("Fonda Doña Chela");
        assertThat(pre.negocioGiro()).isEqualTo("RESTAURANTE");
    }

    @Test
    @DisplayName("sin negocio no pide ninguno")
    void sinNegocio_noPideNinguno() {
        var pre = sinNegocio();

        assertThat(pre.pideNegocio()).isFalse();
        assertThat(pre.negocioNombre()).isNull();
        assertThat(pre.negocioGiro()).isNull();
    }

    @Test
    @DisplayName("el nombre y el giro del negocio van juntos o no va ninguno")
    void negocio_incompleto_seRechaza() {
        assertThatThrownBy(() -> nuevo("Fonda", null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> nuevo(null, "MUSEO")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("el codigo en claro no se guarda: solo coincide el correcto")
    void coincide_soloConElCodigoCorrecto() {
        var pre = sinNegocio();

        assertThat(pre.coincide("123456")).isTrue();
        assertThat(pre.coincide("123457")).isFalse();
        assertThat(pre.coincide("")).isFalse();
        assertThat(pre.coincide(null)).isFalse();
    }

    @Test
    @DisplayName("vence exactamente a los 15 minutos: un segundo antes sirve y a la hora justa ya no")
    void vencido_enElLimite() {
        var pre = sinNegocio();

        assertThat(pre.vencido(AHORA.plus(VIGENCIA).minusSeconds(1))).isFalse();
        assertThat(pre.vencido(AHORA.plus(VIGENCIA))).isTrue();
    }

    @Test
    @DisplayName("cada intento fallido suma uno y a los 5 se agota")
    void intentos_seAgotanAlQuinto() {
        var pre = sinNegocio();
        for (var i = 0; i < 4; i++) {
            pre = pre.conIntentoFallido();
        }
        assertThat(pre.agotado(5)).isFalse();

        pre = pre.conIntentoFallido();

        assertThat(pre.intentos()).isEqualTo(5);
        assertThat(pre.agotado(5)).isTrue();
    }

    @Test
    @DisplayName("un codigo nuevo reemplaza al anterior, reinicia los intentos y mueve el vencimiento; los datos no cambian")
    void conCodigoNuevo_reemplazaElCodigoYReiniciaLosIntentos() {
        var pre = nuevo("Fonda", "MUSEO").conIntentoFallido().conIntentoFallido();
        var despues = AHORA.plus(Duration.ofMinutes(10));

        var renovado = pre.conCodigoNuevo("654321", despues, VIGENCIA);

        assertThat(renovado.coincide("654321")).isTrue();
        assertThat(renovado.coincide("123456")).as("el codigo anterior ya no sirve").isFalse();
        assertThat(renovado.intentos()).isZero();
        assertThat(renovado.venceEn()).isEqualTo(despues.plus(VIGENCIA));
        assertThat(renovado.creadoEn()).isEqualTo(AHORA);
        assertThat(renovado.id()).isEqualTo(ID);
        assertThat(renovado.correo()).isEqualTo("ana@example.com");
        assertThat(renovado.negocioNombre()).isEqualTo("Fonda");
        assertThat(renovado.avisoAceptadoEn()).isEqualTo(AHORA);
    }

    @Test
    @DisplayName("los campos obligatorios no pueden ser nulos y los intentos no pueden ser negativos")
    void campos_obligatorios() {
        assertThatThrownBy(() -> Preregistro.nuevo(null, "a@example.com", "+522221110001", "Ana", "López", 1, null, null,
                "123456", AHORA, VIGENCIA)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> Preregistro.nuevo(ID, null, "+522221110001", "Ana", "López", 1, null, null, "123456",
                AHORA, VIGENCIA)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Preregistro(ID, "a@example.com", "+522221110001", "Ana", "López", 1, AHORA, null,
                null, sinNegocio().codigo(), -1, AHORA.plus(VIGENCIA), AHORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("SI-15: toString no revela el correo, el celular, el nombre ni el codigo")
    void toString_noRevelaDatos() {
        var texto = nuevo("Fonda Secreta", "MUSEO").toString();

        assertThat(texto).doesNotContain("ana@example.com").doesNotContain("2221110001").doesNotContain("Ana")
                .doesNotContain("López").doesNotContain("123456").doesNotContain("Fonda Secreta");
    }
}
