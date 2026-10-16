package mx.negocio360.caja.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** AZ-02 y D-127: Caja declara sus ocho permisos como datos; ninguno es sensible todavia. */
class PermisosDeCajaTest {

    private final List<PermisoDeclarado> permisos = new PermisosDeCaja().permisos();

    @Test
    @DisplayName("declara ocho permisos, todos del modulo caja y sin repetir")
    void declara_ochoPermisos_delModuloCaja() {
        assertThat(permisos).hasSize(8);
        assertThat(permisos).allMatch(p -> p.modulo().equals("caja"));
        assertThat(permisos.stream().map(PermisoDeclarado::codigo)).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("D-127: ninguno es sensible (retirar y autorizar quedan protegidos por rol y auditoria)")
    void ninguno_esSensible() {
        assertThat(permisos).noneMatch(PermisoDeclarado::sensible);
    }

    @Test
    @DisplayName("las constantes que usa el codigo existen en el catalogo")
    void constantes_existenEnElCatalogo() {
        assertThat(permisos.stream().map(PermisoDeclarado::codigo)).containsExactlyInAnyOrder(
                "caja.caja.gestionar", "caja.turno.ver", "caja.turno.abrir", "caja.movimiento.registrar",
                "caja.retiro.registrar", "caja.corte.cerrar", "caja.corte.autorizar", "caja.corte.ver");
        assertThat(PermisosDeCaja.CAJA_GESTIONAR).isEqualTo("caja.caja.gestionar");
        assertThat(PermisosDeCaja.TURNO_VER).isEqualTo("caja.turno.ver");
        assertThat(PermisosDeCaja.TURNO_ABRIR).isEqualTo("caja.turno.abrir");
        assertThat(PermisosDeCaja.MOVIMIENTO_REGISTRAR).isEqualTo("caja.movimiento.registrar");
        assertThat(PermisosDeCaja.RETIRO_REGISTRAR).isEqualTo("caja.retiro.registrar");
        assertThat(PermisosDeCaja.CORTE_CERRAR).isEqualTo("caja.corte.cerrar");
        assertThat(PermisosDeCaja.CORTE_AUTORIZAR).isEqualTo("caja.corte.autorizar");
        assertThat(PermisosDeCaja.CORTE_VER).isEqualTo("caja.corte.ver");
    }

    @Test
    @DisplayName("cada permiso trae una descripcion para la pantalla de roles")
    void permisos_traenDescripcion() {
        assertThat(permisos).allMatch(p -> !p.descripcion().isBlank());
    }
}
