package mx.negocio360.restaurantes.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.Autorizacion;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * AZ-04: el adaptador de permisos de los comandos usa la sesion que el despachador le vincula (el hilo de un comando de
 * GraphQL no trae el contexto de seguridad de la peticion), no deja pasar a otro negocio y falla cerrado fuera del vinculo.
 */
class SesionDeRestaurantesTest {

    private static final UUID NEGOCIO = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000a");
    private static final UUID OTRO = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000b");
    private static final ContextoNegocio CONTEXTO = new ContextoNegocio(NEGOCIO, "ana");
    private static final Autorizacion SOLO_EDITAR = Autorizacion.de(Set.of("restaurantes.menu.editar"));

    private final SesionDeRestaurantes adaptador = new SesionDeRestaurantes();

    @Test
    @DisplayName("con la sesion vinculada: tiene el permiso o no segun la autorizacion de la peticion")
    void conSesion() {
        adaptador.vincular(CONTEXTO, SOLO_EDITAR, () -> {
            assertThat(adaptador.puede(CONTEXTO, "restaurantes.menu.editar")).isTrue();
            assertThat(adaptador.puede(CONTEXTO, "restaurantes.menu.ver")).isFalse();
            return null;
        });
    }

    @Test
    @DisplayName("el contexto de otro negocio nunca tiene permisos con la sesion de este")
    void otroNegocio() {
        adaptador.vincular(CONTEXTO, SOLO_EDITAR, () -> {
            assertThat(adaptador.puede(new ContextoNegocio(OTRO, "ana"), "restaurantes.menu.editar")).isFalse();
            return null;
        });
    }

    @Test
    @DisplayName("fuera del vinculo todo esta negado, y exigir lanza PermisoDenegado")
    void sinSesion() {
        assertThat(adaptador.puede(CONTEXTO, "restaurantes.menu.editar")).isFalse();
        assertThatThrownBy(() -> adaptador.exigir(CONTEXTO, "restaurantes.menu.editar"))
                .isInstanceOf(PermisoDenegadoException.class);
    }

    @Test
    @DisplayName("el vinculo se libera al terminar, tambien si la accion lanza, y devuelve lo que devuelve la accion")
    void seLibera() {
        assertThat(adaptador.vincular(CONTEXTO, SOLO_EDITAR, () -> "listo")).isEqualTo("listo");
        assertThat(adaptador.puede(CONTEXTO, "restaurantes.menu.editar")).isFalse();

        assertThatThrownBy(() -> adaptador.vincular(CONTEXTO, SOLO_EDITAR, () -> {
            throw new IllegalStateException("falla");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(adaptador.puede(CONTEXTO, "restaurantes.menu.editar")).isFalse();
    }

    @Test
    @DisplayName("un vinculo anidado restaura el exterior al terminar")
    void anidado() {
        var todo = Autorizacion.de(Set.of("restaurantes.menu.editar", "restaurantes.menu.ver"));
        adaptador.vincular(CONTEXTO, SOLO_EDITAR, () -> {
            adaptador.vincular(CONTEXTO, todo, () -> {
                assertThat(adaptador.puede(CONTEXTO, "restaurantes.menu.ver")).isTrue();
                return null;
            });
            assertThat(adaptador.puede(CONTEXTO, "restaurantes.menu.ver")).isFalse();
            return null;
        });
    }
}
