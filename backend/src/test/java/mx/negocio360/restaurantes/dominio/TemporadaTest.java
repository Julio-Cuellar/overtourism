package mx.negocio360.restaurantes.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.MonthDay;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** MN-06 (D-177): la temporada es un rango mes-dia, sin año, que puede cruzar el fin de año. */
class TemporadaTest {

    private static final Temporada AGOSTO_A_SEPTIEMBRE = new Temporada(MonthDay.of(8, 1), MonthDay.of(9, 30));
    private static final Temporada NAVIDENA = new Temporada(MonthDay.of(12, 1), MonthDay.of(1, 6));

    @Test
    @DisplayName("un dia dentro del rango esta en temporada, incluidos el primero y el ultimo")
    void dentroDelRango() {
        assertThat(AGOSTO_A_SEPTIEMBRE.contiene(LocalDate.of(2026, 8, 1))).isTrue();
        assertThat(AGOSTO_A_SEPTIEMBRE.contiene(LocalDate.of(2026, 8, 31))).isTrue();
        assertThat(AGOSTO_A_SEPTIEMBRE.contiene(LocalDate.of(2026, 9, 30))).isTrue();
    }

    @Test
    @DisplayName("el dia anterior y el posterior al rango estan fuera de temporada")
    void fueraDelRango() {
        assertThat(AGOSTO_A_SEPTIEMBRE.contiene(LocalDate.of(2026, 7, 31))).isFalse();
        assertThat(AGOSTO_A_SEPTIEMBRE.contiene(LocalDate.of(2026, 10, 1))).isFalse();
    }

    @Test
    @DisplayName("un rango que cruza el fin de año incluye diciembre y enero y excluye el resto")
    void cruzaElFinDeAnio() {
        assertThat(NAVIDENA.contiene(LocalDate.of(2026, 12, 25))).isTrue();
        assertThat(NAVIDENA.contiene(LocalDate.of(2027, 1, 6))).isTrue();
        assertThat(NAVIDENA.contiene(LocalDate.of(2027, 1, 7))).isFalse();
        assertThat(NAVIDENA.contiene(LocalDate.of(2026, 11, 30))).isFalse();
    }

    @Test
    @DisplayName("el 29 de febrero cuenta en un rango que lo abarca, tambien en un año que no es bisiesto")
    void febreroBisiesto() {
        var invierno = new Temporada(MonthDay.of(2, 1), MonthDay.of(3, 15));

        assertThat(invierno.contiene(LocalDate.of(2028, 2, 29))).isTrue();
        assertThat(invierno.contiene(LocalDate.of(2027, 3, 1))).isTrue();
    }

    @Test
    @DisplayName("las dos fechas son obligatorias")
    void sinFechas() {
        assertThatThrownBy(() -> new Temporada(null, MonthDay.of(9, 30))).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new Temporada(MonthDay.of(8, 1), null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("se escribe y se lee como mes-dia")
    void texto() {
        assertThat(AGOSTO_A_SEPTIEMBRE.desdeComoTexto()).isEqualTo("08-01");
        assertThat(AGOSTO_A_SEPTIEMBRE.hastaComoTexto()).isEqualTo("09-30");
        assertThat(Temporada.de("08-01", "09-30")).isEqualTo(AGOSTO_A_SEPTIEMBRE);
    }

    @Test
    @DisplayName("un texto que no es mes-dia falla con TEMPORADA_INVALIDA")
    void textoInvalido() {
        assertThatThrownBy(() -> Temporada.de("2026-08-01", "09-30")).isInstanceOf(RestaurantesException.class)
                .extracting("codigo").isEqualTo(CodigoError.TEMPORADA_INVALIDA);
        assertThatThrownBy(() -> Temporada.de("13-01", "09-30")).isInstanceOf(RestaurantesException.class);
    }
}
