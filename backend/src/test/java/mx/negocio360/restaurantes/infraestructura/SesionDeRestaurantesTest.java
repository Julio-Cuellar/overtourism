package mx.negocio360.restaurantes.infraestructura;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.seguridad.PermisoDenegadoException;
import mx.negocio360.compartido.seguridad.SesionAuthentication;
import mx.negocio360.compartido.seguridad.SesionDePeticion;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;

/** AZ-04: el adaptador de permisos de los comandos lee la sesion de la peticion y no deja pasar a otro negocio. */
class SesionDeRestaurantesTest {

    private static final UUID NEGOCIO = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000a");
    private static final UUID OTRO = UUID.fromString("0192f3a0-7c10-7000-8000-00000000000b");

    private final SesionDeRestaurantes adaptador = new SesionDeRestaurantes();

    @AfterEach
    void limpiar() {
        SecurityContextHolder.clearContext();
    }

    private static void sesion(Set<String> permisos) {
        var sesion = new SesionDePeticion("ana", NEGOCIO, permisos);
        SecurityContextHolder.getContext().setAuthentication(new SesionAuthentication(sesion));
    }

    @Test
    @DisplayName("con sesion: tiene el permiso o no segun la sesion")
    void conSesion() {
        sesion(Set.of("restaurantes.menu.editar"));

        assertThat(adaptador.puede(new ContextoNegocio(NEGOCIO, "ana"), "restaurantes.menu.editar")).isTrue();
        assertThat(adaptador.puede(new ContextoNegocio(NEGOCIO, "ana"), "restaurantes.menu.ver")).isFalse();
    }

    @Test
    @DisplayName("el contexto de otro negocio nunca tiene permisos con la sesion de este")
    void otroNegocio() {
        sesion(Set.of("restaurantes.menu.editar"));

        assertThat(adaptador.puede(new ContextoNegocio(OTRO, "ana"), "restaurantes.menu.editar")).isFalse();
    }

    @Test
    @DisplayName("sin sesion todo esta negado, y exigir lanza PermisoDenegado")
    void sinSesion() {
        assertThat(adaptador.puede(new ContextoNegocio(NEGOCIO, "ana"), "restaurantes.menu.editar")).isFalse();
        assertThatThrownBy(() -> adaptador.exigir(new ContextoNegocio(NEGOCIO, "ana"), "restaurantes.menu.editar"))
                .isInstanceOf(PermisoDenegadoException.class);
    }
}
