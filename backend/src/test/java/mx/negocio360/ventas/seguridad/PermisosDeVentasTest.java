package mx.negocio360.ventas.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** AZ-02 y D-127: Ventas declara sus tres permisos como datos; ninguno es sensible todavia. */
class PermisosDeVentasTest {

    private final List<PermisoDeclarado> permisos = new PermisosDeVentas().permisos();

    @Test
    @DisplayName("declara tres permisos, todos del modulo ventas y sin repetir")
    void declara_tresPermisos_delModuloVentas() {
        assertThat(permisos).hasSize(3);
        assertThat(permisos).allMatch(p -> p.modulo().equals("ventas"));
        assertThat(permisos.stream().map(PermisoDeclarado::codigo)).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("D-127: ninguno es sensible (anular queda protegido por motivo, permiso y auditoria)")
    void ninguno_esSensible() {
        assertThat(permisos).noneMatch(PermisoDeclarado::sensible);
    }

    @Test
    @DisplayName("las constantes que usa el codigo existen en el catalogo")
    void constantes_existenEnElCatalogo() {
        assertThat(permisos.stream().map(PermisoDeclarado::codigo)).containsExactlyInAnyOrder(
                PermisosDeVentas.VENTA_VER, PermisosDeVentas.VENTA_REGISTRAR, PermisosDeVentas.VENTA_ANULAR);
        assertThat(PermisosDeVentas.VENTA_VER).isEqualTo("ventas.venta.ver");
        assertThat(PermisosDeVentas.VENTA_REGISTRAR).isEqualTo("ventas.venta.registrar");
        assertThat(PermisosDeVentas.VENTA_ANULAR).isEqualTo("ventas.venta.anular");
    }

    @Test
    @DisplayName("cada permiso trae una descripcion para la pantalla de roles")
    void permisos_traenDescripcion() {
        assertThat(permisos).allMatch(p -> !p.descripcion().isBlank());
    }
}
