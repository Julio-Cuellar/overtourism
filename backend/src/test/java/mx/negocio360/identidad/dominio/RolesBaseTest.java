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
    @DisplayName("hay nueve roles base, en el orden del diseño (MESERO_CAJA va después de MESERO, D-171)")
    void todos_sonNueveRoles() {
        assertThat(RolesBase.todos().stream().map(Rol::codigo)).containsExactly("DUENO", "ADMINISTRADOR", "GERENTE",
                "CAJERO", "MESERO", "MESERO_CAJA", "COCINERO", "TAQUILLERO", "GUIA");
    }

    @Test
    @DisplayName("cada giro ofrece sus roles: restaurante con mesero, mesero con caja y cocinero; museo con taquillero y guia")
    void plantilla_porGiro() {
        assertThat(RolesBase.plantilla(Giro.RESTAURANTE)).containsExactly("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO",
                "MESERO", "MESERO_CAJA", "COCINERO");
        assertThat(RolesBase.plantilla(Giro.MISCELANEA)).containsExactly("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO");
        assertThat(RolesBase.plantilla(Giro.MUSEO)).containsExactly("DUENO", "ADMINISTRADOR", "GERENTE", "TAQUILLERO",
                "GUIA");
    }

    @Test
    @DisplayName("el dueño tiene todos los permisos que cita cualquier rol")
    void dueno_tieneTodos() {
        var todos = RolesBase.todos().stream().flatMap(r -> r.permisos().stream()).collect(Collectors.toSet());

        assertThat(permisosDe("DUENO")).containsAll(todos).hasSize(48);
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
    @DisplayName("el cajero busca y registra clientes y sus consentimientos, y solo mira el salon, el menu, las cuentas y las comandas")
    void cajero_soloBuscaYRegistra() {
        assertThat(permisosDe("CAJERO")).containsExactlyInAnyOrder("clientes.sugerir", "clientes.cliente.registrar",
                "clientes.consentimiento.registrar", "negocios.perfil.ver", "restaurantes.menu.ver",
                "restaurantes.salon.ver", "restaurantes.cuenta.ver", "restaurantes.comanda.ver");
    }

    @Test
    @DisplayName("el mesero busca clientes para ligarlos a una cuenta y atiende el salon: abre y cancela cuentas, anula lineas y entrega")
    void mesero_atiendeElSalon() {
        assertThat(permisosDe("MESERO")).containsExactlyInAnyOrder("clientes.sugerir", "negocios.perfil.ver",
                "restaurantes.menu.ver", "restaurantes.salon.ver", "restaurantes.cuenta.ver", "restaurantes.cuenta.abrir",
                "restaurantes.cuenta.cancelar", "restaurantes.linea.anular", "restaurantes.comanda.ver",
                "restaurantes.comanda.entregar");
    }

    @Test
    @DisplayName("el cocinero ve el menu y las recetas sin costos, marca agotados y prepara comandas; taquillero y guia solo ven el perfil")
    void cocineroTaquilleroYGuia() {
        assertThat(permisosDe("COCINERO")).containsExactlyInAnyOrder("negocios.perfil.ver", "restaurantes.menu.ver",
                "restaurantes.menu.agotar", "restaurantes.receta.ver", "restaurantes.comanda.ver",
                "restaurantes.comanda.preparar");
        assertThat(permisosDe("TAQUILLERO")).containsExactly("negocios.perfil.ver");
        assertThat(permisosDe("GUIA")).containsExactly("negocios.perfil.ver");
    }

    /**
     * D-171: quien atiende y cobra. Hasta que la Pista A declare ventas.venta.* y caja.turno.ver (H-1) tiene exactamente los
     * permisos del mesero; esos tres se agregan entonces, con su propia migracion (la V037 corre antes de la V035 en una
     * base nueva y no puede citar permisos que todavia no existen).
     */
    @Test
    @DisplayName("D-171: MESERO_CAJA tiene hoy los permisos del mesero y su nombre lo dice")
    void meseroCaja_hoyTieneLosDelMesero() {
        assertThat(RolesBase.porCodigo("MESERO_CAJA").orElseThrow().nombre()).isEqualTo("Mesero con caja");
        assertThat(permisosDe("MESERO_CAJA")).containsExactlyInAnyOrderElementsOf(permisosDe("MESERO"));
    }

    /** La matriz de la seccion 11 de docs/diseno/restaurantes.md: quien tiene cada permiso de Restaurantes. */
    private static final java.util.Map<String, Set<String>> MATRIZ_DE_RESTAURANTES = java.util.Map.ofEntries(
            java.util.Map.entry("restaurantes.menu.ver", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO", "MESERO",
                    "MESERO_CAJA", "COCINERO")),
            java.util.Map.entry("restaurantes.menu.editar", Set.of("DUENO", "ADMINISTRADOR", "GERENTE")),
            java.util.Map.entry("restaurantes.menu.agotar", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "COCINERO")),
            java.util.Map.entry("restaurantes.receta.ver", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "COCINERO")),
            java.util.Map.entry("restaurantes.receta.editar", Set.of("DUENO", "ADMINISTRADOR", "GERENTE")),
            java.util.Map.entry("restaurantes.costo.ver", Set.of("DUENO", "ADMINISTRADOR", "GERENTE")),
            java.util.Map.entry("restaurantes.salon.ver", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO", "MESERO",
                    "MESERO_CAJA")),
            java.util.Map.entry("restaurantes.salon.configurar", Set.of("DUENO", "ADMINISTRADOR")),
            java.util.Map.entry("restaurantes.servicio.operar", Set.of("DUENO", "ADMINISTRADOR", "GERENTE")),
            java.util.Map.entry("restaurantes.cuenta.ver", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO", "MESERO",
                    "MESERO_CAJA")),
            java.util.Map.entry("restaurantes.cuenta.abrir", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "MESERO",
                    "MESERO_CAJA")),
            java.util.Map.entry("restaurantes.cuenta.cancelar", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "MESERO",
                    "MESERO_CAJA")),
            java.util.Map.entry("restaurantes.linea.anular", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "MESERO",
                    "MESERO_CAJA")),
            java.util.Map.entry("restaurantes.linea.anular_preparada", Set.of("DUENO", "ADMINISTRADOR", "GERENTE")),
            java.util.Map.entry("restaurantes.comanda.ver", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO", "MESERO",
                    "MESERO_CAJA", "COCINERO")),
            java.util.Map.entry("restaurantes.comanda.preparar", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "COCINERO")),
            java.util.Map.entry("restaurantes.comanda.entregar", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "MESERO",
                    "MESERO_CAJA")),
            java.util.Map.entry("restaurantes.consumo.ver", Set.of("DUENO", "ADMINISTRADOR", "GERENTE")));

    @Test
    @DisplayName("los permisos de Restaurantes de cada rol son exactamente los de la matriz de restaurantes.md seccion 11")
    void restaurantes_matrizDeLaSeccion11() {
        assertThat(MATRIZ_DE_RESTAURANTES).hasSize(18);
        for (var rol : RolesBase.todos()) {
            var esperados = MATRIZ_DE_RESTAURANTES.entrySet().stream().filter(e -> e.getValue().contains(rol.codigo()))
                    .map(java.util.Map.Entry::getKey).collect(Collectors.toSet());
            var reales = rol.permisos().stream().filter(p -> p.startsWith("restaurantes.")).collect(Collectors.toSet());

            assertThat(reales).as(rol.codigo()).isEqualTo(esperados);
        }
    }

    @Test
    @DisplayName("el gerente tiene todo de Restaurantes menos configurar el salon, y el taquillero y el guia nada")
    void restaurantes_gerenteTaquilleroYGuia() {
        assertThat(permisosDe("GERENTE")).filteredOn(p -> p.startsWith("restaurantes.")).hasSize(17)
                .doesNotContain("restaurantes.salon.configurar");
        assertThat(permisosDe("TAQUILLERO")).noneMatch(p -> p.startsWith("restaurantes."));
        assertThat(permisosDe("GUIA")).noneMatch(p -> p.startsWith("restaurantes."));
    }

    /** La matriz de la seccion 7.7 de docs/diseno/negocios.md: quien tiene cada permiso de Negocios. */
    private static final java.util.Map<String, Set<String>> MATRIZ_DE_NEGOCIOS = java.util.Map.ofEntries(
            java.util.Map.entry("negocios.perfil.ver", Set.of("DUENO", "ADMINISTRADOR", "GERENTE", "CAJERO", "MESERO",
                    "MESERO_CAJA", "COCINERO", "TAQUILLERO", "GUIA")),
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
}
