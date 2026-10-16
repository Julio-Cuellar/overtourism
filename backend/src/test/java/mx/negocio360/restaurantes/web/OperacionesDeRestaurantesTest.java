package mx.negocio360.restaurantes.web;

import static mx.negocio360.compartido.graphql.TipoDeOperacion.MUTATION;
import static mx.negocio360.compartido.graphql.TipoDeOperacion.QUERY;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import mx.negocio360.compartido.graphql.Acceso;
import mx.negocio360.compartido.graphql.OperacionDeclarada;
import mx.negocio360.compartido.seguridad.PermisoDeclarado;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * SG-21, SG-03 y SG-04 para Restaurantes: el inventario de sus operaciones de GraphQL y lo que exige cada una. El panel pide
 * el mismo permiso que ya exige su servicio; la unica operacion sin sesion es el menu publico del turista.
 */
class OperacionesDeRestaurantesTest {

    private final OperacionesDeRestaurantes declaraciones = new OperacionesDeRestaurantes();

    private Map<String, OperacionDeclarada> porNombre() {
        var mapa = new LinkedHashMap<String, OperacionDeclarada>();
        declaraciones.operaciones().forEach(o -> mapa.put(o.nombre(), o));
        return mapa;
    }

    private static String regla(OperacionDeclarada o) {
        return o.acceso() + (o.permiso() == null ? "" : ":" + o.permiso());
    }

    @Test
    @DisplayName("cada operacion del panel pide el mismo permiso que su servicio")
    void panel_permisos() {
        var esperado = new LinkedHashMap<String, String>();
        esperado.put("Query.categoriasDeMenu", "NEGOCIO:" + PermisosDeRestaurantes.MENU_VER);
        esperado.put("Query.platillos", "NEGOCIO:" + PermisosDeRestaurantes.MENU_VER);
        esperado.put("Query.platillo", "NEGOCIO:" + PermisosDeRestaurantes.MENU_VER);
        esperado.put("Query.resumenDeMenu", "NEGOCIO:" + PermisosDeRestaurantes.MENU_VER);
        esperado.put("Mutation.crearCategoria", "NEGOCIO:" + PermisosDeRestaurantes.MENU_EDITAR);
        esperado.put("Mutation.editarCategoria", "NEGOCIO:" + PermisosDeRestaurantes.MENU_EDITAR);
        esperado.put("Mutation.crearPlatillo", "NEGOCIO:" + PermisosDeRestaurantes.MENU_EDITAR);
        esperado.put("Mutation.editarPlatillo", "NEGOCIO:" + PermisosDeRestaurantes.MENU_EDITAR);
        esperado.put("Mutation.publicarPlatillo", "NEGOCIO:" + PermisosDeRestaurantes.MENU_EDITAR);
        esperado.put("Mutation.cambiarModoDeInventario", "NEGOCIO:" + PermisosDeRestaurantes.MENU_EDITAR);
        esperado.put("Mutation.marcarAgotado", "NEGOCIO:" + PermisosDeRestaurantes.MENU_AGOTAR);
        esperado.put("Query.consumosFallidos", "NEGOCIO:" + PermisosDeRestaurantes.CONSUMO_VER);
        esperado.put("Query.receta", "NEGOCIO:" + PermisosDeRestaurantes.RECETA_VER);
        esperado.put("Query.costoDeReceta", "NEGOCIO:" + PermisosDeRestaurantes.COSTO_VER);
        esperado.put("Mutation.desactivarReceta", "NEGOCIO:" + PermisosDeRestaurantes.RECETA_EDITAR);

        var real = porNombre();
        esperado.forEach((nombre, regla) -> assertThat(real).as(nombre).containsKey(nombre));
        esperado.forEach((nombre, regla) -> assertThat(regla(real.get(nombre))).as(nombre).isEqualTo(regla));
    }

    @Test
    @DisplayName("SG-01: la unica operacion publica de Restaurantes en esta fase es el menu publico")
    void publicas() {
        var publicas = porNombre().values().stream().filter(o -> o.acceso() == Acceso.PUBLICA)
                .map(OperacionDeclarada::nombre).sorted().toList();

        assertThat(publicas).containsExactly("Query.menuPublico");
    }

    @Test
    @DisplayName("Restaurantes declara todas las operaciones de su esquema y ninguna otra")
    void inventarioCompleto() {
        assertThat(porNombre().keySet()).containsExactlyInAnyOrder("Query.categoriasDeMenu", "Query.platillos",
                "Query.platillo", "Query.resumenDeMenu", "Query.menuPublico", "Mutation.crearCategoria",
                "Mutation.editarCategoria", "Mutation.crearPlatillo", "Mutation.editarPlatillo",
                "Mutation.publicarPlatillo", "Mutation.cambiarModoDeInventario", "Mutation.marcarAgotado", "Query.receta",
                "Query.costoDeReceta", "Mutation.desactivarReceta", "Query.consumosFallidos");
    }

    @Test
    @DisplayName("todo permiso que pide una operacion esta en el catalogo de permisos de Restaurantes")
    void permisosDelCatalogo() {
        var catalogo = new PermisosDeRestaurantes().permisos().stream().map(PermisoDeclarado::codigo).toList();

        porNombre().values().stream().map(OperacionDeclarada::permiso).filter(p -> p != null)
                .forEach(permiso -> assertThat(catalogo).contains(permiso));
    }

    @Test
    @DisplayName("solo hay consultas y mutaciones, y ninguna variante de comando todavia: llega con el despachador de la plataforma")
    void tipos() {
        assertThat(porNombre().values().stream().map(OperacionDeclarada::tipo).distinct().toList())
                .containsExactlyInAnyOrder(QUERY, MUTATION);
        assertThat(declaraciones.comandos()).isEmpty();
    }
}
