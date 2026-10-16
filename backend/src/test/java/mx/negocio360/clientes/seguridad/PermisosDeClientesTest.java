package mx.negocio360.clientes.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** AZ-02: Clientes declara sus permisos como datos; los derechos del responsable son sensibles (D-73). */
class PermisosDeClientesTest {

    private final PermisosDeClientes catalogo = new PermisosDeClientes();

    @Test
    @DisplayName("declara los 11 permisos de Clientes, todos de su modulo")
    void permisos_losOnce() {
        assertThat(catalogo.permisos()).extracting(PermisoDeclarado::codigo).containsExactlyInAnyOrder(
                "clientes.sugerir", "clientes.lista.ver", "clientes.panorama.ver", "clientes.ficha.ver",
                "clientes.cliente.registrar", "clientes.cliente.editar", "clientes.consentimiento.registrar",
                "clientes.aviso.publicar", "clientes.cliente.anonimizar", "clientes.arco.gestionar",
                "clientes.datos.exportar");
        assertThat(catalogo.permisos()).allSatisfy(p -> {
            assertThat(p.modulo()).isEqualTo("clientes");
            assertThat(p.descripcion()).isNotBlank();
        });
    }

    @Test
    @DisplayName("son sensibles publicar el aviso, anonimizar, atender ARCO y exportar los datos")
    void permisos_sensibles() {
        assertThat(catalogo.permisos()).filteredOn(PermisoDeclarado::sensible).extracting(PermisoDeclarado::codigo)
                .containsExactlyInAnyOrder("clientes.aviso.publicar", "clientes.cliente.anonimizar",
                        "clientes.arco.gestionar", "clientes.datos.exportar");
    }
}
