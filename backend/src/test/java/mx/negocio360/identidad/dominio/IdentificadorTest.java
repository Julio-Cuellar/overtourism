package mx.negocio360.identidad.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** D-76: el id, el correo y el celular identifican a la persona; cualquiera de los tres sirve para pedir el codigo. */
class IdentificadorTest {

    @Test
    @DisplayName("un celular se normaliza a E.164")
    void de_celular_seNormaliza() {
        var identificador = Identificador.de(" +52 (222) 111-0001 ");

        assertThat(identificador.tipo()).isEqualTo(Identificador.Tipo.TELEFONO);
        assertThat(identificador.valor()).isEqualTo("+522221110001");
    }

    @Test
    @DisplayName("un correo se recorta y se pasa a minusculas")
    void de_correo_seNormaliza() {
        var identificador = Identificador.de("  Ana.Lopez@Example.COM ");

        assertThat(identificador.tipo()).isEqualTo(Identificador.Tipo.CORREO);
        assertThat(identificador.valor()).isEqualTo("ana.lopez@example.com");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "0192f39f-3333-7000-8000-00000000f006|0192f39f-3333-7000-8000-00000000f006",
        "  0192F39F-3333-7000-8000-00000000F006 |0192f39f-3333-7000-8000-00000000f006"})
    @DisplayName("un UUID se reconoce como id de la cuenta, en minusculas")
    void de_uuid_esUnId(String entrada, String esperado) {
        var identificador = Identificador.de(entrada);

        assertThat(identificador.tipo()).isEqualTo(Identificador.Tipo.ID);
        assertThat(identificador.valor()).isEqualTo(esperado);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"123", "abc", "+1 2221110001", "0192f39f-3333-7000-8000"})
    @DisplayName("lo que no es celular, ni correo, ni id falla con TELEFONO_INVALIDO")
    void de_entradaSinForma_fallaConTelefonoInvalido(String entrada) {
        assertThat(codigoDe(entrada)).isEqualTo(CodigoError.TELEFONO_INVALIDO);
    }

    @ParameterizedTest
    @ValueSource(strings = {"a@", "@x.com", "a@b", "ña@x.com"})
    @DisplayName("lo que trae arroba pero no es un correo valido falla con CORREO_INVALIDO")
    void de_correoMalFormado_fallaConCorreoInvalido(String entrada) {
        assertThat(codigoDe(entrada)).isEqualTo(CodigoError.CORREO_INVALIDO);
    }

    @Test
    @DisplayName("una entrada enorme se rechaza sin analizarla")
    void de_entradaEnorme_fallaSinAnalizarla() {
        assertThat(codigoDe("x".repeat(5_000))).isEqualTo(CodigoError.TELEFONO_INVALIDO);
        assertThat(codigoDe("a".repeat(5_000) + "@x.com")).isEqualTo(CodigoError.CORREO_INVALIDO);
    }

    @Test
    @DisplayName("SI-15: toString no revela el valor")
    void toString_noRevelaElValor() {
        assertThat(Identificador.de("ana@example.com").toString()).doesNotContain("ana").doesNotContain("example");
        assertThat(Identificador.de("2221110001").toString()).doesNotContain("2221110001");
    }

    private static CodigoError codigoDe(String entrada) {
        var error = catchThrowable(() -> Identificador.de(entrada));
        assertThat(error).isInstanceOf(IdentidadException.class);
        return ((IdentidadException) error).codigo();
    }
}
