package mx.negocio360.compartido.graphql;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.compartido.seguridad.NoAutenticadoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** SG-01 y SG-02: lo que el filtro de entrada le entrega a los resolvedores, y que falle cerrado si no hay sesion. */
class EntradaGraphQlTest {

    private static final UUID CUENTA = UUID.fromString("0192f39f-3333-7000-8000-00000000f001");
    private static final UUID NEGOCIO = UUID.fromString("0192f39f-3333-7000-8000-00000000f002");

    @Test
    @DisplayName("una entrada anonima no tiene cuenta, contexto ni permisos, y los exige con NO_AUTENTICADO")
    void anonima_fallaCerrado() {
        var entrada = EntradaGraphQl.anonima(null);

        assertThat(entrada.autenticada()).isFalse();
        assertThat(entrada.autorizacion().puede("negocios.perfil.ver")).isFalse();
        assertThatThrownBy(entrada::exigirCuenta).isInstanceOf(NoAutenticadoException.class)
                .extracting(e -> ((NoAutenticadoException) e).codigo()).isEqualTo("NO_AUTENTICADO");
        assertThatThrownBy(entrada::exigirContexto).isInstanceOf(NoAutenticadoException.class)
                .extracting(e -> ((NoAutenticadoException) e).codigo()).isEqualTo("NO_AUTENTICADO");
    }

    @Test
    @DisplayName("una sesion sin negocio activo tiene cuenta pero no contexto: SIN_NEGOCIO_ACTIVO")
    void sinNegocioActivo() {
        var entrada = EntradaGraphQl.de(CUENTA, null, Autorizacion.de(Set.of()), "disp");

        assertThat(entrada.exigirCuenta()).isEqualTo(CUENTA);
        assertThatThrownBy(entrada::exigirContexto).isInstanceOf(NoAutenticadoException.class)
                .extracting(e -> ((NoAutenticadoException) e).codigo()).isEqualTo("SIN_NEGOCIO_ACTIVO");
    }

    @Test
    @DisplayName("con negocio activo entrega el contexto y los permisos de la sesion")
    void conNegocioActivo() {
        var contexto = new ContextoNegocio(NEGOCIO, CUENTA.toString());
        var entrada = EntradaGraphQl.de(CUENTA, contexto, Autorizacion.de(Set.of("negocios.perfil.ver")), "disp");

        assertThat(entrada.exigirContexto()).isEqualTo(contexto);
        assertThat(entrada.autorizacion().puede("negocios.perfil.ver")).isTrue();
        assertThat(entrada.autorizacion().puede("negocios.negocio.cerrar")).isFalse();
        assertThat(entrada.dispositivo()).isEqualTo("disp");
    }

    @Test
    @DisplayName("el dispositivo viaja aunque no haya sesion: es el autor de las reseñas del turista")
    void dispositivoSinSesion() {
        assertThat(EntradaGraphQl.anonima("0192f39f-3333-7000-8000-00000000f003").dispositivo())
                .isEqualTo("0192f39f-3333-7000-8000-00000000f003");
    }

    @Test
    @DisplayName("toString no imprime el dispositivo ni la cuenta")
    void toString_sinDatos() {
        var entrada = EntradaGraphQl.de(CUENTA, null, Autorizacion.de(Set.of()), "dispositivo-secreto");

        assertThat(entrada.toString()).doesNotContain("dispositivo-secreto").doesNotContain(CUENTA.toString());
    }
}
