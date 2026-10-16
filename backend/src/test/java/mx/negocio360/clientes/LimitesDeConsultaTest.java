package mx.negocio360.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import mx.negocio360.clientes.consultas.LimitesDeConsulta;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** SC-03 y SG-07: topes contra la extraccion masiva de datos personales. Los resolvedores futuros los aplican. */
class LimitesDeConsultaTest {

    @Test
    @DisplayName("los topes son los de SC-03")
    void limites_delContrato_sonLosDeSc03() {
        assertThat(LimitesDeConsulta.POR_PAGINA_INICIAL).isEqualTo(20);
        assertThat(LimitesDeConsulta.MAX_POR_PAGINA).isEqualTo(50);
        assertThat(LimitesDeConsulta.MAX_SUGERENCIAS).isEqualTo(5);
        assertThat(LimitesDeConsulta.MIN_TEXTO_SUGERENCIA).isEqualTo(2);
        assertThat(LimitesDeConsulta.MAX_TEXTO_BUSQUEDA).isEqualTo(80);
    }

    @ParameterizedTest
    @CsvSource({"1,1", "20,20", "50,50", "51,50", "1000000,50"})
    @DisplayName("la pagina pedida se recorta al maximo")
    void paginaEfectiva_pedidoMayorAlMaximo_serecorta(int pedido, int esperado) {
        assertThat(LimitesDeConsulta.paginaEfectiva(pedido)).isEqualTo(esperado);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    @DisplayName("una pagina de cero o menos se rechaza")
    void paginaEfectiva_cero_oNegativo_lanzaIllegalArgument(int pedido) {
        assertThatThrownBy(() -> LimitesDeConsulta.paginaEfectiva(pedido))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @CsvSource({"a,false", "ab,true", "'  a ',false", "'  ab ',true"})
    @DisplayName("la sugerencia exige al menos dos caracteres sin contar espacios de los extremos")
    void textoDeSugerencia_largoMinimo(String texto, boolean valido) {
        assertThat(LimitesDeConsulta.textoDeSugerenciaValido(texto)).isEqualTo(valido);
    }

    @Test
    @DisplayName("el texto de busqueda no pasa de 80 caracteres y no puede ser nulo")
    void textoDeSugerencia_largoMaximoYNulo() {
        assertThat(LimitesDeConsulta.textoDeSugerenciaValido("a".repeat(80))).isTrue();
        assertThat(LimitesDeConsulta.textoDeSugerenciaValido("a".repeat(81))).isFalse();
        assertThat(LimitesDeConsulta.textoDeSugerenciaValido(null)).isFalse();
    }
}
