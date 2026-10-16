package mx.negocio360.identidad.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** AM-09 y AZ-03 · D-69: los roles base y sus plantillas por giro son datos; un permiso que no existe impide arrancar. */
class RolesBaseTest {

    private static final Set<String> RESERVADOS_DEL_RESPONSABLE = Set.of("clientes.aviso.publicar",
            "clientes.cliente.anonimizar", "clientes.arco.gestionar", "clientes.datos.exportar");

    private static Set<String> permisosDe(String rol) {
        return RolesBase.porCodigo(rol).orElseThrow().permisos();
    }

    @Test
    @DisplayName("hay ocho roles base, en el orden del diseño")
    void todos_sonOchoRoles() {
        assertThat(RolesBase.todos().stream().map(Rol::codigo)).containsExactly("DUENO", "ADMINISTRADOR", "GERENTE",
                "CAJERO", "MESERO", "COCINERO", "TAQUILLERO", "GUIA");
    }

    @Test
    @DisplayName("cada giro ofrece sus roles: restaurante con mesero y cocinero, museo con taquillero y guia")
    void plantilla_porGiro() {
        assertThat(RolesBase.plantilla(Giro.RESTAURANTE)).containsExactly("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO",
                "MESERO", "COCINERO");
        assertThat(RolesBase.plantilla(Giro.MISCELANEA)).containsExactly("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO");
        assertThat(RolesBase.plantilla(Giro.MUSEO)).containsExactly("DUENO", "ADMINISTRADOR", "GERENTE", "TAQUILLERO",
                "GUIA");
    }

    @Test
    @DisplayName("el dueño tiene todos los permisos que cita cualquier rol")
    void dueno_tieneTodos() {
        var todos = RolesBase.todos().stream().flatMap(r -> r.permisos().stream()).collect(Collectors.toSet());

        assertThat(permisosDe("DUENO")).containsAll(todos).hasSize(16);
    }

    @Test
    @DisplayName("el administrador tiene todo menos los derechos del responsable")
    void administrador_todoMenosLosDelResponsable() {
        var esperado = permisosDe("DUENO").stream().filter(p -> !RESERVADOS_DEL_RESPONSABLE.contains(p))
                .collect(Collectors.toSet());

        assertThat(permisosDe("ADMINISTRADOR")).containsExactlyInAnyOrderElementsOf(esperado);
    }

    @Test
    @DisplayName("el gerente opera clientes y ve al equipo, pero no lo administra")
    void gerente_operaClientesYVeAlEquipo() {
        assertThat(permisosDe("GERENTE")).contains("identidad.equipo.ver", "clientes.lista.ver", "clientes.cliente.editar")
                .doesNotContain("identidad.miembro.invitar", "identidad.miembro.cambiar_rol", "identidad.miembro.revocar",
                        "identidad.auditoria.ver");
    }

    @Test
    @DisplayName("el cajero solo busca y registra clientes y sus consentimientos")
    void cajero_soloBuscaYRegistra() {
        assertThat(permisosDe("CAJERO")).containsExactlyInAnyOrder("clientes.sugerir", "clientes.cliente.registrar",
                "clientes.consentimiento.registrar");
    }

    @Test
    @DisplayName("el mesero solo busca clientes para ligarlos a una cuenta")
    void mesero_soloBusca() {
        assertThat(permisosDe("MESERO")).containsExactly("clientes.sugerir");
    }

    @Test
    @DisplayName("cocinero, taquillero y guia no tienen permisos hasta que existan Inventario y Museos")
    void cocineroTaquilleroYGuia_sinPermisosPorAhora() {
        assertThat(permisosDe("COCINERO")).isEmpty();
        assertThat(permisosDe("TAQUILLERO")).isEmpty();
        assertThat(permisosDe("GUIA")).isEmpty();
    }

    @Test
    @DisplayName("AZ-03: con el catalogo completo la verificacion pasa")
    void verificar_catalogoCompleto_pasa() {
        var catalogo = RolesBase.todos().stream().flatMap(r -> r.permisos().stream()).distinct()
                .map(codigo -> new PermisoDeclarado(codigo, "Permiso de prueba", false)).toList();

        RolesBase.verificar(catalogo);
    }

    @Test
    @DisplayName("AZ-03: un rol que cita un permiso que no esta declarado impide el arranque, y el error lo nombra")
    void verificar_permisoNoDeclarado_lanzaConElNombre() {
        var sinSugerir = RolesBase.todos().stream().flatMap(r -> r.permisos().stream()).distinct()
                .filter(p -> !p.equals("clientes.sugerir"))
                .map(codigo -> new PermisoDeclarado(codigo, "Permiso de prueba", false)).toList();

        assertThatThrownBy(() -> RolesBase.verificar(sinSugerir)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("clientes.sugerir");
    }

    @Test
    @DisplayName("un rol desconocido no se encuentra")
    void porCodigo_desconocido_vacio() {
        assertThat(RolesBase.porCodigo("SUPERUSUARIO")).isEmpty();
        assertThat(RolesBase.porCodigo(null)).isEmpty();
    }

    @Test
    @DisplayName("los roles y sus permisos son inmutables")
    void roles_sonInmutables() {
        var rol = RolesBase.porCodigo("CAJERO").orElseThrow();

        assertThatThrownBy(() -> rol.permisos().add("clientes.cliente.anonimizar"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> RolesBase.todos().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThat(List.copyOf(RolesBase.plantilla(Giro.MUSEO))).isNotEmpty();
    }
}
