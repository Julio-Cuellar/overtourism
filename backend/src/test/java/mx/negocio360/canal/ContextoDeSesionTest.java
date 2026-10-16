package mx.negocio360.canal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.canal.web.ContextoDeSesion;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.identidad.CuentaVista;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.PerfilVista;
import mx.negocio360.identidad.SesionAutenticada;
import mx.negocio360.identidad.SesionIniciada;
import org.junit.jupiter.api.DisplayName;
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
        return new IdentidadApi() {
            @Override
            public Optional<SesionAutenticada> autenticar(String token) {
                return TOKEN.equals(token)
                        ? Optional.of(new SesionAutenticada(CUENTA, "Ana", negocioDeLaSesion, Instant.now()))
                        : Optional.empty();
            }

            @Override
            public void solicitarCodigo(String telefono, String ip) {
                throw new UnsupportedOperationException();
            }

            @Override
            public SesionIniciada iniciarSesion(String t, String c, String n, String d, String ip) {
                throw new UnsupportedOperationException();
            }

            @Override
            public PerfilVista yo(String token) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void cerrarSesion(String token) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void cerrarTodasLasSesiones(String token) {
                throw new UnsupportedOperationException();
            }

            @Override
            public CuentaVista actualizarNombre(String token, String nombre) {
                throw new UnsupportedOperationException();
            }
        };
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
