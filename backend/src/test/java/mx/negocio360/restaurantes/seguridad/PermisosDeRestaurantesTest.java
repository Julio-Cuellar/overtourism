package mx.negocio360.restaurantes.seguridad;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import mx.negocio360.compartido.seguridad.CatalogoDePermisos;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * AZ-02 y D-70 · R0 de Restaurantes (docs/diseno/restaurantes.md, seccion 11): el modulo declara sus 18 permisos de P1
 * como datos. Ninguno es sensible (D-73 negaria por completo los sensibles hasta que exista la confirmacion reforzada).
 */
class PermisosDeRestaurantesTest {

    private static final List<String> LOS_DE_P1 = List.of("restaurantes.menu.ver", "restaurantes.menu.editar",
            "restaurantes.menu.agotar", "restaurantes.receta.ver", "restaurantes.receta.editar",
            "restaurantes.costo.ver", "restaurantes.salon.ver", "restaurantes.salon.configurar",
            "restaurantes.servicio.operar", "restaurantes.cuenta.ver", "restaurantes.cuenta.abrir",
            "restaurantes.cuenta.cancelar", "restaurantes.linea.anular", "restaurantes.linea.anular_preparada",
            "restaurantes.comanda.ver", "restaurantes.comanda.preparar", "restaurantes.comanda.entregar",
            "restaurantes.consumo.ver");

    private final CatalogoDePermisos catalogo = new PermisosDeRestaurantes();

    @Test
    @DisplayName("declara exactamente los 18 permisos de P1 del diseno, sin repetir ninguno")
    void permisos_sonLos18DeP1() {
        var codigos = catalogo.permisos().stream().map(PermisoDeclarado::codigo).toList();

        assertThat(codigos).containsExactlyInAnyOrderElementsOf(LOS_DE_P1).doesNotHaveDuplicates().hasSize(18);
    }

    @Test
    @DisplayName("todos son del modulo restaurantes")
    void permisos_todosSonDelModulo() {
        assertThat(catalogo.permisos()).allSatisfy(p -> assertThat(p.modulo()).isEqualTo("restaurantes"));
    }

    @Test
    @DisplayName("ninguno es sensible")
    void permisos_ningunoEsSensible() {
        assertThat(catalogo.permisos()).noneMatch(PermisoDeclarado::sensible);
    }

    @Test
    @DisplayName("cada permiso tiene una descripcion para la pantalla de roles")
    void permisos_tienenDescripcion() {
        assertThat(catalogo.permisos()).allSatisfy(p -> assertThat(p.descripcion()).isNotBlank());
    }

    @Test
    @DisplayName("las constantes publicas nombran los permisos que los casos de uso van a exigir")
    void constantes_coincidenConElCatalogo() {
        var codigos = catalogo.permisos().stream().map(PermisoDeclarado::codigo).toList();

        assertThat(codigos).contains(PermisosDeRestaurantes.MENU_VER, PermisosDeRestaurantes.MENU_EDITAR,
                PermisosDeRestaurantes.MENU_AGOTAR, PermisosDeRestaurantes.RECETA_VER,
                PermisosDeRestaurantes.RECETA_EDITAR, PermisosDeRestaurantes.COSTO_VER,
                PermisosDeRestaurantes.CONSUMO_VER);
    }
}
