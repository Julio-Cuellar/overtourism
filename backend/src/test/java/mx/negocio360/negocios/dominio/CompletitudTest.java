package mx.negocio360.negocios.dominio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** NG-11: la completitud del perfil se calcula solo con codigo. */
class CompletitudTest {

    @Test
    @DisplayName("un negocio recien dado de alta vale 40 % y le faltan los cuatro")
    void recienDadoDeAlta_valeCuarentaPorCiento() {
        var completitud = Completitud.de(false, false, false, false);

        assertThat(completitud.porcentaje()).isEqualTo(40);
        assertThat(completitud.faltantes()).containsExactly(Completitud.Faltante.UBICACION,
                Completitud.Faltante.HORARIO, Completitud.Faltante.CONTACTO, Completitud.Faltante.LOGO);
    }

    @Test
    @DisplayName("ubicacion y horario valen 20 cada uno; contacto y logo, 10")
    void cadaPieza_sumaSuPeso() {
        assertThat(Completitud.de(true, false, false, false).porcentaje()).isEqualTo(60);
        assertThat(Completitud.de(false, true, false, false).porcentaje()).isEqualTo(60);
        assertThat(Completitud.de(false, false, true, false).porcentaje()).isEqualTo(50);
        assertThat(Completitud.de(false, false, false, true).porcentaje()).isEqualTo(50);
        assertThat(Completitud.de(true, true, true, true).porcentaje()).isEqualTo(100);
        assertThat(Completitud.de(true, true, true, true).faltantes()).isEmpty();
    }

    @Test
    @DisplayName("los faltantes salen en un orden fijo")
    void faltantes_ordenFijo() {
        assertThat(Completitud.de(true, false, true, false).faltantes())
                .isEqualTo(List.of(Completitud.Faltante.HORARIO, Completitud.Faltante.LOGO));
    }
}
