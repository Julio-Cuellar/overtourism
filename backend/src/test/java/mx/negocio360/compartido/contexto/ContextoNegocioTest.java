package mx.negocio360.compartido.contexto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/** SG-02: el negocio y el actor salen del contexto de la peticion, nunca de un argumento. */
class ContextoNegocioTest {

    @Test
    @DisplayName("guarda el negocio y el actor")
    void nuevo_datosValidos_losConserva() {
        var negocio = UUID.randomUUID();

        var contexto = new ContextoNegocio(negocio, "locatario-1");

        assertThat(contexto.negocioId()).isEqualTo(negocio);
        assertThat(contexto.actor()).isEqualTo("locatario-1");
    }

    @Test
    @DisplayName("el negocio es obligatorio")
    void nuevo_sinNegocio_lanzaNullPointer() {
        assertThatThrownBy(() -> new ContextoNegocio(null, "locatario-1")).isInstanceOf(NullPointerException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("el actor no puede estar vacio")
    void nuevo_actorVacio_lanzaIllegalArgument(String actor) {
        var negocio = UUID.randomUUID();

        assertThatThrownBy(() -> new ContextoNegocio(negocio, actor)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("el actor es obligatorio")
    void nuevo_actorNulo_lanzaNullPointer() {
        var negocio = UUID.randomUUID();

        assertThatThrownBy(() -> new ContextoNegocio(negocio, null)).isInstanceOf(NullPointerException.class);
    }
}
