package mx.negocio360.identidad.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** IDN-1.2 · AC-04: el nombre visible es obligatorio, de 2 a 80 caracteres, y no hay otros datos obligatorios. */
class NombreVisibleTest {

    @ParameterizedTest
    @ValueSource(strings = {"Al", "Ana", "José Ñandú", "María de los Ángeles"})
    @DisplayName("un nombre de 2 a 80 caracteres con letras de cualquier idioma es valido")
    void normalizar_nombresValidos_losConserva(String nombre) {
        assertThat(NombreVisible.normalizar(nombre)).contains(nombre);
    }

    @Test
    @DisplayName("recorta espacios en los extremos y colapsa los repetidos")
    void normalizar_espaciosSobrantes_losLimpia() {
        assertThat(NombreVisible.normalizar("  Ana    López  ")).contains("Ana López");
    }

    @Test
    @DisplayName("80 caracteres es el maximo")
    void normalizar_ochentaCaracteres_esValido() {
        assertThat(NombreVisible.normalizar("a".repeat(80))).isPresent();
        assertThat(NombreVisible.normalizar("a".repeat(81))).isEmpty();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "A", " A ", "Ana\u0000", "Ana\nLópez", "Ana\tLópez", "Ana‮López"})
    @DisplayName("nulo, vacio, de un caracter o con caracteres de control se rechaza")
    void normalizar_entradasInvalidas_devuelveVacio(String nombre) {
        assertThat(NombreVisible.normalizar(nombre)).isEmpty();
    }
}
