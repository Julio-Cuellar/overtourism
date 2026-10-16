package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import mx.negocio360.compartido.seguridad.PropiedadesHttp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SG-09 y SG-14: la configuracion de la puerta HTTP falla cerrada si se configura mal. */
class PropiedadesHttpTest {

    @Test
    @DisplayName("por omision: cuerpo de 64 KB y ningun origen de navegador permitido")
    void porOmision_cuerpoDe64KbYSinOrigenes() {
        var propiedades = new PropiedadesHttp(null, null);

        assertThat(propiedades.cuerpoMaximoBytes()).isEqualTo(65_536);
        assertThat(propiedades.origenesPermitidos()).isEmpty();
    }

    @Test
    @DisplayName("los origenes se normalizan: minusculas, sin espacios y sin diagonal final")
    void origenes_seNormalizan() {
        var propiedades = new PropiedadesHttp(1024, List.of(" HTTPS://Panel.Ejemplo.test/ ", "http://localhost:3000"));

        assertThat(propiedades.origenesPermitidos()).containsExactly("https://panel.ejemplo.test",
                "http://localhost:3000");
    }

    @Test
    @DisplayName("un comodin, un origen con ruta o uno sin esquema impiden el arranque")
    void origenesInvalidos_fallan() {
        for (var invalido : List.of("*", "https://*.ejemplo.test", "https://panel.ejemplo.test/ruta", "panel.ejemplo.test",
                "null", "")) {
            assertThatThrownBy(() -> new PropiedadesHttp(1024, List.of(invalido)))
                    .as("origen %s", invalido).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    @DisplayName("el tope del cuerpo debe estar entre 1 byte y 1 MiB")
    void topeDelCuerpo_tieneLimites() {
        assertThatThrownBy(() -> new PropiedadesHttp(0, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PropiedadesHttp(-5, List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PropiedadesHttp(1_048_577, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(new PropiedadesHttp(1_048_576, List.of()).cuerpoMaximoBytes()).isEqualTo(1_048_576);
    }
}
