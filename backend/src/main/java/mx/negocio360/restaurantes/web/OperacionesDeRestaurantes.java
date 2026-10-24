package mx.negocio360.restaurantes.web;

import static mx.negocio360.compartido.graphql.TipoDeOperacion.MUTATION;
import static mx.negocio360.compartido.graphql.TipoDeOperacion.QUERY;

import java.util.List;
import mx.negocio360.compartido.graphql.ComandoDeclarado;
import mx.negocio360.compartido.graphql.OperacionDeclarada;
import mx.negocio360.compartido.graphql.OperacionesGraphql;
import mx.negocio360.restaurantes.seguridad.PermisosDeRestaurantes;
import org.springframework.stereotype.Component;

/**
 * El inventario de operaciones de GraphQL de Restaurantes (SG-21) y lo que exige cada una (SG-03 y SG-04). El panel pide el
 * mismo permiso que ya exige su servicio: la puerta de la plataforma lo comprueba antes y el servicio vuelve a hacerlo. La
 * unica operacion sin sesion es el menu publico del turista (D-170), que sale de una vista de solo lectura. Las variantes
 * de comando ({@code guardarReceta}, {@code darDeBajaPlatillo}) viajan por el despachador de comandos de la plataforma
 * (D-125, D-209) y se declaran en {@link #comandos()}.
 */
@Component
public final class OperacionesDeRestaurantes implements OperacionesGraphql {

    @Override
    public List<OperacionDeclarada> operaciones() {
        return List.of(
                OperacionDeclarada.de(QUERY, "categoriasDeMenu", PermisosDeRestaurantes.MENU_VER),
                OperacionDeclarada.de(QUERY, "platillos", PermisosDeRestaurantes.MENU_VER),
                OperacionDeclarada.de(QUERY, "platillo", PermisosDeRestaurantes.MENU_VER),
                OperacionDeclarada.de(QUERY, "resumenDeMenu", PermisosDeRestaurantes.MENU_VER),
                OperacionDeclarada.de(QUERY, "consumosFallidos", PermisosDeRestaurantes.CONSUMO_VER),
                OperacionDeclarada.de(QUERY, "receta", PermisosDeRestaurantes.RECETA_VER),
                OperacionDeclarada.de(QUERY, "costoDeReceta", PermisosDeRestaurantes.COSTO_VER),
                // Superficie publica del turista: sin sesion, sobre lo publicado
                OperacionDeclarada.publica(QUERY, "menuPublico"),
                OperacionDeclarada.de(MUTATION, "crearCategoria", PermisosDeRestaurantes.MENU_EDITAR),
                OperacionDeclarada.de(MUTATION, "editarCategoria", PermisosDeRestaurantes.MENU_EDITAR),
                OperacionDeclarada.de(MUTATION, "crearPlatillo", PermisosDeRestaurantes.MENU_EDITAR),
                OperacionDeclarada.de(MUTATION, "editarPlatillo", PermisosDeRestaurantes.MENU_EDITAR),
                OperacionDeclarada.de(MUTATION, "publicarPlatillo", PermisosDeRestaurantes.MENU_EDITAR),
                OperacionDeclarada.de(MUTATION, "cambiarModoDeInventario", PermisosDeRestaurantes.MENU_EDITAR),
                OperacionDeclarada.de(MUTATION, "marcarAgotado", PermisosDeRestaurantes.MENU_AGOTAR),
                OperacionDeclarada.de(MUTATION, "desactivarReceta", PermisosDeRestaurantes.RECETA_EDITAR));
    }

    /** Las variantes de {@code ComandoInput} de Restaurantes y el permiso que pide la puerta de la plataforma (SG-04). */
    @Override
    public List<ComandoDeclarado> comandos() {
        return List.of(
                new ComandoDeclarado("guardarReceta", PermisosDeRestaurantes.RECETA_EDITAR),
                new ComandoDeclarado("darDeBajaPlatillo", PermisosDeRestaurantes.MENU_EDITAR));
    }
}
