package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import mx.negocio360.compartido.graphql.PropiedadesGraphql;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SG-06, SG-08 y SG-09: los valores iniciales aceptados y la validacion de la configuracion. */
class PropiedadesGraphqlTest {

    @Test
    @DisplayName("valores iniciales: profundidad 7, complejidad 100, 10 alias, 5 campos raiz, 10000 caracteres y 10 s")
    void porOmision_sonLosValoresAceptados() {
        var propiedades = new PropiedadesGraphql(null, null, null, null, null, null);

        assertThat(propiedades.profundidadMaxima()).isEqualTo(7);
        assertThat(propiedades.complejidadMaxima()).isEqualTo(100);
        assertThat(propiedades.aliasMaximos()).isEqualTo(10);
        assertThat(propiedades.camposRaizMaximos()).isEqualTo(5);
        assertThat(propiedades.consultaMaximaCaracteres()).isEqualTo(10_000);
        assertThat(propiedades.tiempoMaximo()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("un limite cero, negativo o absurdo impide el arranque")
    void limitesInvalidos_fallan() {
        assertThatThrownBy(() -> new PropiedadesGraphql(0, null, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PropiedadesGraphql(null, -1, null, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PropiedadesGraphql(null, null, 0, null, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PropiedadesGraphql(null, null, null, 0, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PropiedadesGraphql(null, null, null, null, 0, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PropiedadesGraphql(null, null, null, null, 10_001, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PropiedadesGraphql(null, null, null, null, null, Duration.ofMillis(50)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PropiedadesGraphql(null, null, null, null, null, Duration.ofMinutes(5)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
