package mx.negocio360.inventario.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** AZ-02 e IDN-4.4: Inventario declara sus cuatro permisos como datos; ninguno es sensible todavia. */
class PermisosDeInventarioTest {

    private final List<PermisoDeclarado> permisos = new PermisosDeInventario().permisos();

    @Test
    @DisplayName("declara cuatro permisos, todos del modulo inventario y sin repetir")
    void declara_cuatroPermisos() {
        assertThat(permisos).hasSize(4);
        assertThat(permisos).allMatch(p -> p.modulo().equals("inventario"));
        assertThat(permisos.stream().map(PermisoDeclarado::codigo)).doesNotHaveDuplicates();
        assertThat(permisos).noneMatch(PermisoDeclarado::sensible);
    }

    @Test
    @DisplayName("las constantes que usa el codigo existen en el catalogo")
    void constantes_existenEnElCatalogo() {
        assertThat(permisos.stream().map(PermisoDeclarado::codigo)).containsExactlyInAnyOrder(
                PermisosDeInventario.ARTICULO_VER, PermisosDeInventario.ARTICULO_GESTIONAR,
                PermisosDeInventario.MOVIMIENTO_REGISTRAR, PermisosDeInventario.CONTEO_AJUSTAR);
        assertThat(PermisosDeInventario.ARTICULO_VER).isEqualTo("inventario.articulo.ver");
        assertThat(PermisosDeInventario.ARTICULO_GESTIONAR).isEqualTo("inventario.articulo.gestionar");
        assertThat(PermisosDeInventario.MOVIMIENTO_REGISTRAR).isEqualTo("inventario.movimiento.registrar");
        assertThat(PermisosDeInventario.CONTEO_AJUSTAR).isEqualTo("inventario.conteo.ajustar");
    }
}
