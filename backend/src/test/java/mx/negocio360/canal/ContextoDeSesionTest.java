package mx.negocio360.canal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.canal.web.ContextoDeSesion;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.SesionAutenticada;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mockito;
import org.junit.jupiter.api.Test;

/**
 * El negocio y el actor salen de la sesion, nunca de la peticion (AZ-05). Mientras no exista el modulo de negocios,
 * en la demo se usa un negocio de demostracion configurado en el servidor (D-90).
 */
class ContextoDeSesionTest {

    private static final String TOKEN = "token-valido";
    private static final UUID CUENTA = UUID.randomUUID();
    private static final UUID NEGOCIO_DEMO = UUID.randomUUID();

    /** Identidad que solo reconoce {@link #TOKEN}, con el negocio que se le diga. */
    private static IdentidadApi identidadCon(UUID negocioDeLaSesion) {
        var identidad = Mockito.mock(IdentidadApi.class);
        Mockito.when(identidad.autenticar(Mockito.anyString())).thenReturn(Optional.empty());
        Mockito.when(identidad.autenticar(TOKEN)).thenReturn(Optional.of(new SesionAutenticada(CUENTA, "Ana",
                negocioDeLaSesion, negocioDeLaSesion == null ? null : "DUENO", Set.of(), Instant.now())));
        return identidad;
    }

    @Test
    @DisplayName("una sesion con negocio activo da ese negocio y la cuenta como actor")
    void sesionConNegocio() {
        var negocio = UUID.randomUUID();
        var contexto = new ContextoDeSesion(identidadCon(negocio), null).resolver("Bearer " + TOKEN);

        assertThat(contexto).contains(new ContextoNegocio(negocio, CUENTA.toString()));
    }

    @Test
    @DisplayName("D-90: una sesion sin negocio usa el negocio de demostracion solo si el servidor lo configuro")
    void sesionSinNegocio_negocioDeDemo() {
        assertThat(new ContextoDeSesion(identidadCon(null), NEGOCIO_DEMO).resolver("bearer " + TOKEN))
                .contains(new ContextoNegocio(NEGOCIO_DEMO, CUENTA.toString()));
        assertThat(new ContextoDeSesion(identidadCon(null), null).resolver("Bearer " + TOKEN)).isEmpty();
    }

    @Test
    @DisplayName("sin encabezado, con otro esquema o con un token que Identidad no reconoce, no hay contexto")
    void sinSesionValida_vacio() {
        var contexto = new ContextoDeSesion(identidadCon(null), NEGOCIO_DEMO);

        assertThat(contexto.resolver(null)).isEmpty();
        assertThat(contexto.resolver("Basic " + TOKEN)).isEmpty();
        assertThat(contexto.resolver("Bearer otro")).isEmpty();
    }
}
