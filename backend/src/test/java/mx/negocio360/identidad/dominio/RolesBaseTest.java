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

        assertThat(permisosDe("DUENO")).containsAll(todos).hasSize(45);
    }

    @Test
    @DisplayName("el administrador tiene todo menos los derechos del responsable y pausar o cerrar el negocio")
    void administrador_todoMenosLosDelResponsable() {
        var esperado = permisosDe("DUENO").stream().filter(p -> !RESERVADOS_DEL_RESPONSABLE.contains(p))
                .filter(p -> !p.equals("negocios.negocio.pausar") && !p.equals("negocios.negocio.cerrar"))
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
    @DisplayName("el cajero solo busca y registra clientes y sus consentimientos (ademas de cobrar y operar su caja)")
    void cajero_soloBuscaYRegistra() {
        assertThat(permisosDe("CAJERO")).filteredOn(p -> p.startsWith("clientes.") || p.startsWith("negocios."))
                .containsExactlyInAnyOrder("clientes.sugerir", "clientes.cliente.registrar",
                        "clientes.consentimiento.registrar", "negocios.perfil.ver");
    }

    @Test
    @DisplayName("el mesero solo busca clientes para ligarlos a una cuenta")
    void mesero_soloBusca() {
        assertThat(permisosDe("MESERO")).containsExactlyInAnyOrder("clientes.sugerir", "negocios.perfil.ver");
    }

    @Test
    @DisplayName("el cocinero ve el perfil y maneja existencias; guia y taquillero solo ven el perfil (el taquillero ademas cobra)")
    void cocineroTaquilleroYGuia_soloVenElPerfil() {
        assertThat(permisosDe("COCINERO")).containsExactlyInAnyOrder("negocios.perfil.ver", "inventario.articulo.ver",
                "inventario.movimiento.registrar");
        assertThat(permisosDe("TAQUILLERO")).filteredOn(p -> !p.startsWith("ventas.") && !p.startsWith("caja."))
                .containsExactly("negocios.perfil.ver");
        assertThat(permisosDe("GUIA")).containsExactly("negocios.perfil.ver");
    }

    /** La matriz de la seccion 7.7 de docs/diseno/negocios.md: quien tiene cada permiso de Negocios. */
    private static final java.util.Map<String, Set<String>> MATRIZ_DE_NEGOCIOS = java.util.Map.ofEntries(
            java.util.Map.entry("negocios.perfil.ver", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO", "MESERO",
                    "COCINERO", "TAQUILLERO", "GUIA")),
            java.util.Map.entry("negocios.perfil.editar", Set.of("DUENO", "ADMINISTRADOR")),
            java.util.Map.entry("negocios.sucursal.gestionar", Set.of("DUENO", "ADMINISTRADOR")),
            java.util.Map.entry("negocios.horario.editar", Set.of("DUENO", "ADMINISTRADOR", "GERENTE")),
            java.util.Map.entry("negocios.negocio.publicar", Set.of("DUENO", "ADMINISTRADOR")),
            java.util.Map.entry("negocios.disponibilidad.editar", Set.of("DUENO", "ADMINISTRADOR", "GERENTE")),
            java.util.Map.entry("negocios.negocio.pausar", Set.of("DUENO")),
            java.util.Map.entry("negocios.negocio.cerrar", Set.of("DUENO")),
            java.util.Map.entry("negocios.empleado.ver", Set.of("DUENO", "ADMINISTRADOR", "GERENTE")),
            java.util.Map.entry("negocios.empleado.registrar", Set.of("DUENO", "ADMINISTRADOR")),
            java.util.Map.entry("negocios.empleado.editar", Set.of("DUENO", "ADMINISTRADOR")),
            java.util.Map.entry("negocios.empleado.baja", Set.of("DUENO", "ADMINISTRADOR")),
            java.util.Map.entry("negocios.resena.ver", Set.of("DUENO", "ADMINISTRADOR", "GERENTE")),
            java.util.Map.entry("negocios.resena.responder", Set.of("DUENO", "ADMINISTRADOR", "GERENTE")));

    @Test
    @DisplayName("D-96: los permisos de Negocios de cada rol son exactamente los de la matriz de negocios.md 7.7")
    void negocios_matrizDeLaSeccion77() {
        assertThat(MATRIZ_DE_NEGOCIOS).hasSize(14);
        for (var rol : RolesBase.todos()) {
            var esperados = MATRIZ_DE_NEGOCIOS.entrySet().stream().filter(e -> e.getValue().contains(rol.codigo()))
                    .map(java.util.Map.Entry::getKey).collect(Collectors.toSet());
            var reales = rol.permisos().stream().filter(p -> p.startsWith("negocios.")).collect(Collectors.toSet());

            assertThat(reales).as(rol.codigo()).isEqualTo(esperados);
        }
    }

    @Test
    @DisplayName("los dos permisos sensibles de Negocios (cerrar y dar de baja) solo los tiene el dueno o el administrador")
    void negocios_sensibles_soloDuenoYAdministrador() {
        for (var rol : RolesBase.todos()) {
            if (!Set.of("DUENO", "ADMINISTRADOR").contains(rol.codigo())) {
                assertThat(rol.permisos()).as(rol.codigo()).doesNotContain("negocios.negocio.cerrar",
                        "negocios.empleado.baja");
            }
        }
        assertThat(permisosDe("ADMINISTRADOR")).doesNotContain("negocios.negocio.cerrar").contains("negocios.empleado.baja");
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

    /** La matriz de la seccion 8.2 de docs/planes/integracion-ventas-caja.md: quien tiene cada permiso de Ventas y Caja. */
    private static final java.util.Map<String, Set<String>> MATRIZ_VENTAS_Y_CAJA = java.util.Map.ofEntries(
            java.util.Map.entry("ventas.venta.ver", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO", "TAQUILLERO")),
            java.util.Map.entry("ventas.venta.registrar", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO", "TAQUILLERO")),
            java.util.Map.entry("ventas.venta.anular", Set.of("DUENO", "ADMINISTRADOR", "GERENTE")),
            java.util.Map.entry("caja.caja.gestionar", Set.of("DUENO", "ADMINISTRADOR")),
            java.util.Map.entry("caja.turno.ver", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO", "TAQUILLERO")),
            java.util.Map.entry("caja.turno.abrir", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO", "TAQUILLERO")),
            java.util.Map.entry("caja.movimiento.registrar",
                    Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO", "TAQUILLERO")),
            java.util.Map.entry("caja.retiro.registrar", Set.of("DUENO", "ADMINISTRADOR", "GERENTE")),
            java.util.Map.entry("caja.corte.cerrar", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO", "TAQUILLERO")),
            java.util.Map.entry("caja.corte.autorizar", Set.of("DUENO", "ADMINISTRADOR", "GERENTE")),
            java.util.Map.entry("caja.corte.ver", Set.of("DUENO", "ADMINISTRADOR", "GERENTE")));

    @Test
    @DisplayName("D-127: los permisos de Ventas y Caja de cada rol son exactamente los de la matriz de la seccion 8.2")
    void ventasYCaja_matrizDeLaSeccion82() {
        for (var rol : RolesBase.todos()) {
            var esperados = MATRIZ_VENTAS_Y_CAJA.entrySet().stream().filter(e -> e.getValue().contains(rol.codigo()))
                    .map(java.util.Map.Entry::getKey).collect(Collectors.toSet());
            var reales = rol.permisos().stream().filter(p -> p.startsWith("ventas.") || p.startsWith("caja."))
                    .collect(Collectors.toSet());
            assertThat(reales).as(rol.codigo()).isEqualTo(esperados);
        }
    }

    @Test
    @DisplayName("D-127: mesero, cocinero y guia no cobran ni tocan la caja; anular, retirar y autorizar son del gerente para arriba")
    void ventasYCaja_quienNoCobraYQuienAnula() {
        for (var rol : List.of("MESERO", "COCINERO", "GUIA")) {
            assertThat(permisosDe(rol)).as(rol).noneMatch(p -> p.startsWith("ventas.") || p.startsWith("caja."));
        }
        for (var rol : List.of("CAJERO", "TAQUILLERO")) {
            assertThat(permisosDe(rol)).as(rol).doesNotContain("ventas.venta.anular", "caja.retiro.registrar",
                    "caja.corte.autorizar", "caja.corte.ver", "caja.caja.gestionar");
        }
    }

    /** Inventario (IDN-4.4): quien tiene cada permiso. */
    private static final java.util.Map<String, Set<String>> MATRIZ_INVENTARIO = java.util.Map.of(
            "inventario.articulo.ver", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "COCINERO"),
            "inventario.articulo.gestionar", Set.of("DUENO", "ADMINISTRADOR", "GERENTE"),
            "inventario.movimiento.registrar", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "COCINERO"),
            "inventario.conteo.ajustar", Set.of("DUENO", "ADMINISTRADOR", "GERENTE"));

    @Test
    @DisplayName("IDN-4.4: los permisos de Inventario de cada rol son exactamente los de la matriz")
    void inventario_matriz() {
        for (var rol : RolesBase.todos()) {
            var esperados = MATRIZ_INVENTARIO.entrySet().stream().filter(e -> e.getValue().contains(rol.codigo()))
                    .map(java.util.Map.Entry::getKey).collect(Collectors.toSet());
            var reales = rol.permisos().stream().filter(p -> p.startsWith("inventario.")).collect(Collectors.toSet());
            assertThat(reales).as(rol.codigo()).isEqualTo(esperados);
        }
    }
}
