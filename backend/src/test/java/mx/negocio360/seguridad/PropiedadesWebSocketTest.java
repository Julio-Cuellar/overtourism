package mx.negocio360.seguridad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import mx.negocio360.compartido.seguridad.PropiedadesWebSocket;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SG-15: los topes del WebSocket y su validacion. */
class PropiedadesWebSocketTest {

    @Test
    @DisplayName("valores iniciales: 5 operaciones abiertas por cuenta y 30 conexiones nuevas por minuto por IP")
    void porOmision() {
        var propiedades = new PropiedadesWebSocket(null, null);

        assertThat(propiedades.suscripcionesPorCuenta()).isEqualTo(5);
        assertThat(propiedades.conexionesPorMinutoIp()).isEqualTo(30);
    }

    @Test
    @DisplayName("un valor cero, negativo o absurdo impide el arranque")
    void valoresInvalidos_fallan() {
        assertThatThrownBy(() -> new PropiedadesWebSocket(0, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PropiedadesWebSocket(101, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PropiedadesWebSocket(null, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PropiedadesWebSocket(null, 100_001)).isInstanceOf(IllegalArgumentException.class);
    }
}
