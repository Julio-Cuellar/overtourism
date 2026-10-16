package mx.negocio360.compartido.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** IDN-1.3 · AS-01, SI-01: los codigos de acceso se guardan solo como hash con sal. */
class HashConSalTest {

    @Test
    @DisplayName("de genera sal de 16 bytes y hash SHA-256, ambos en hexadecimal, sin el secreto")
    void de_unSecreto_guardaSalYHashSinElSecreto() {
        var resultado = HashConSal.de("123456");

        assertThat(resultado.sal()).matches("[0-9a-f]{32}");
        assertThat(resultado.hash()).matches("[0-9a-f]{64}").doesNotContain("123456");
    }

    @Test
    @DisplayName("el mismo secreto produce sal y hash distintos cada vez")
    void de_mismoSecretoDosVeces_salYHashDistintos() {
        var primero = HashConSal.de("123456");
        var segundo = HashConSal.de("123456");

        assertThat(primero.sal()).isNotEqualTo(segundo.sal());
        assertThat(primero.hash()).isNotEqualTo(segundo.hash());
    }

    @Test
    @DisplayName("verifica acepta el secreto original y rechaza otro")
    void verifica_secretoOriginalYOtro_aceptaSoloElOriginal() {
        var resultado = HashConSal.de("123456");

        assertThat(resultado.verifica("123456")).isTrue();
        assertThat(resultado.verifica("123457")).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" 123456", "123456 ", "12345", "1234567"})
    @DisplayName("verifica rechaza nulo, vacio y variantes del secreto")
    void verifica_variantesDelSecreto_laRechaza(String intento) {
        assertThat(HashConSal.de("123456").verifica(intento)).isFalse();
    }

    @Test
    @DisplayName("la sal cambia el hash: el mismo secreto con otra sal no coincide")
    void verifica_conOtraSal_noCoincide() {
        var original = HashConSal.de("123456");
        var otraSal = new HashConSal("0".repeat(32), original.hash());

        assertThat(otraSal.verifica("123456")).isFalse();
    }

    @Test
    @DisplayName("toString no revela la sal ni el hash")
    void toString_nuncaRevelaSalNiHash() {
        var resultado = HashConSal.de("123456");

        assertThat(resultado.toString()).doesNotContain(resultado.sal()).doesNotContain(resultado.hash());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "xyz", "ABCDEF0123456789ABCDEF0123456789", "00"})
    @DisplayName("una sal mal formada se rechaza al reconstruir desde el almacenamiento")
    void constructor_salMalFormada_lanzaExcepcion(String sal) {
        var hash = "a".repeat(64);

        assertThatThrownBy(() -> new HashConSal(sal, hash)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @MethodSource("hashesMalFormados")
    @DisplayName("un hash mal formado se rechaza al reconstruir desde el almacenamiento")
    void constructor_hashMalFormado_lanzaExcepcion(String hash) {
        var sal = "a".repeat(32);

        assertThatThrownBy(() -> new HashConSal(sal, hash)).isInstanceOf(IllegalArgumentException.class);
    }

    static Stream<String> hashesMalFormados() {
        return Stream.of("", "xyz", "G".repeat(64), "a".repeat(63), "A".repeat(64));
    }

    @Test
    @DisplayName("de con secreto nulo falla")
    void de_secretoNulo_lanzaNullPointer() {
        assertThatThrownBy(() -> HashConSal.de(null)).isInstanceOf(NullPointerException.class);
    }
}
