package mx.negocio360.compartido.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

/** IDN-1.3 · AS-05, SI-04: token opaco de 256 bits, solo su hash se guarda y se compara en tiempo constante. */
class TokenOpacoTest {

    @Test
    @DisplayName("nuevo devuelve 32 bytes aleatorios en base64 URL, sin relleno (43 caracteres)")
    void nuevo_siempre_devuelveBase64UrlDe43Caracteres() {
        assertThat(TokenOpaco.nuevo()).matches("[A-Za-z0-9_-]{43}");
    }

    @Test
    @DisplayName("mil tokens nuevos son todos distintos")
    void nuevo_milVeces_nuncaRepite() {
        var tokens = new HashSet<String>();
        for (var i = 0; i < 1000; i++) {
            tokens.add(TokenOpaco.nuevo());
        }

        assertThat(tokens).hasSize(1000);
    }

    @Test
    @DisplayName("hash es SHA-256 en hexadecimal minusculas (vector conocido de «abc»)")
    void hash_deAbc_coincideConElVectorSha256() {
        assertThat(TokenOpaco.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }

    @Test
    @DisplayName("hash es determinista y no contiene el token")
    void hash_mismoToken_mismoHashSinElToken() {
        var token = TokenOpaco.nuevo();

        assertThat(TokenOpaco.hash(token)).isEqualTo(TokenOpaco.hash(token))
                .matches("[0-9a-f]{64}").doesNotContain(token);
    }

    @Test
    @DisplayName("hash de nulo falla: nunca se aceptan tokens ausentes en silencio")
    void hash_nulo_lanzaNullPointer() {
        assertThatThrownBy(() -> TokenOpaco.hash(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("coincide con el token del que salio el hash")
    void coincide_tokenOriginal_esVerdadero() {
        var token = TokenOpaco.nuevo();

        assertThat(TokenOpaco.coincide(TokenOpaco.hash(token), token)).isTrue();
    }

    @Test
    @DisplayName("no coincide con otro token, ni con el hash en lugar del token")
    void coincide_otroTokenOElHash_esFalso() {
        var token = TokenOpaco.nuevo();
        var hash = TokenOpaco.hash(token);

        assertThat(TokenOpaco.coincide(hash, TokenOpaco.nuevo())).isFalse();
        assertThat(TokenOpaco.coincide(hash, hash)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("un token nulo o vacio nunca coincide")
    void coincide_tokenNuloOVacio_esFalso(String token) {
        assertThat(TokenOpaco.coincide(TokenOpaco.hash("x"), token)).isFalse();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @DisplayName("un hash guardado nulo o vacio nunca coincide")
    void coincide_hashNuloOVacio_esFalso(String hash) {
        assertThat(TokenOpaco.coincide(hash, TokenOpaco.nuevo())).isFalse();
    }
}
