package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import mx.negocio360.compartido.seguridad.PropiedadesTasa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SG-10: los valores iniciales aceptados (60 por minuto por cuenta y 20 fallos por minuto por IP). */
class PropiedadesTasaTest {

    @Test
    @DisplayName("valores iniciales: 60 por minuto por cuenta, 20 fallos por minuto por IP y 100000 claves")
    void porOmision_sonLosValoresAceptados() {
        var propiedades = new PropiedadesTasa(null, null, null);

        assertThat(propiedades.peticionesPorMinutoCuenta()).isEqualTo(60);
        assertThat(propiedades.fallosPorMinutoIp()).isEqualTo(20);
        assertThat(propiedades.maximoDeClaves()).isEqualTo(100_000);
    }

    @Test
    @DisplayName("un valor cero, negativo o absurdo impide el arranque")
    void valoresInvalidos_fallan() {
        assertThatThrownBy(() -> new PropiedadesTasa(0, null, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PropiedadesTasa(null, -1, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PropiedadesTasa(100_001, null, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PropiedadesTasa(null, null, 99)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PropiedadesTasa(null, null, 10_000_001))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
