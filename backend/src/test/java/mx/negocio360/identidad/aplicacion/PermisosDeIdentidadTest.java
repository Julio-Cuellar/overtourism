package mx.negocio360.identidad.aplicacion;

import static org.assertj.core.api.Assertions.assertThat;

import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** AZ-02: Identidad declara sus propios permisos; administrar al equipo es sensible (D-73). */
class PermisosDeIdentidadTest {

    private final PermisosDeIdentidad catalogo = new PermisosDeIdentidad();

    @Test
    @DisplayName("declara los 5 permisos de Identidad, todos de su modulo")
    void permisos_losCinco() {
        assertThat(catalogo.permisos()).extracting(PermisoDeclarado::codigo).containsExactlyInAnyOrder(
                "identidad.equipo.ver", "identidad.miembro.invitar", "identidad.miembro.cambiar_rol",
                "identidad.miembro.revocar", "identidad.auditoria.ver");
        assertThat(catalogo.permisos()).allSatisfy(p -> assertThat(p.modulo()).isEqualTo("identidad"));
    }

    @Test
    @DisplayName("invitar, cambiar el rol y revocar son sensibles; ver al equipo y la auditoria no")
    void permisos_sensibles() {
        assertThat(catalogo.permisos()).filteredOn(PermisoDeclarado::sensible).extracting(PermisoDeclarado::codigo)
                .containsExactlyInAnyOrder("identidad.miembro.invitar", "identidad.miembro.cambiar_rol",
                        "identidad.miembro.revocar");
    }
}
