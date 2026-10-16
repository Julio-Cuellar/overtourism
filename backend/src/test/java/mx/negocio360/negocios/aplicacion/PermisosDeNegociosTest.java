package mx.negocio360.negocios.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** AZ-02 y SG-04: Negocios declara sus catorce permisos como datos; dos son sensibles. */
class PermisosDeNegociosTest {

    private final List<PermisoDeclarado> permisos = new PermisosDeNegocios().permisos();

    @Test
    @DisplayName("declara catorce permisos, todos del modulo negocios y sin repetir")
    void declara_catorcePermisos_delModuloNegocios() {
        assertThat(permisos).hasSize(14);
        assertThat(permisos).allMatch(p -> p.modulo().equals("negocios"));
        assertThat(permisos.stream().map(PermisoDeclarado::codigo)).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("solo cerrar el negocio y dar de baja a un empleado son sensibles")
    void sensibles_sonCerrarYBaja() {
        var sensibles = permisos.stream().filter(PermisoDeclarado::sensible).map(PermisoDeclarado::codigo).toList();

        assertThat(sensibles).containsExactlyInAnyOrder("negocios.negocio.cerrar", "negocios.empleado.baja");
    }

    @Test
    @DisplayName("las constantes que usa el codigo existen en el catalogo")
    void constantes_existenEnElCatalogo() {
        var codigos = permisos.stream().map(PermisoDeclarado::codigo).toList();

        assertThat(codigos).contains(PermisosDeNegocios.PERFIL_VER, PermisosDeNegocios.PERFIL_EDITAR,
                PermisosDeNegocios.SUCURSAL_GESTIONAR, PermisosDeNegocios.HORARIO_EDITAR,
                PermisosDeNegocios.NEGOCIO_PUBLICAR, PermisosDeNegocios.DISPONIBILIDAD_EDITAR,
                PermisosDeNegocios.NEGOCIO_PAUSAR, PermisosDeNegocios.NEGOCIO_CERRAR);
    }

    @Test
    @DisplayName("cada permiso trae una descripcion para la pantalla de roles")
    void permisos_traenDescripcion() {
        assertThat(permisos).allMatch(p -> !p.descripcion().isBlank());
    }
}
