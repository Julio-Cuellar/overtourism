package mx.negocio360.inventario.graphql;

import static mx.negocio360.compartido.graphql.TipoDeOperacion.QUERY;

import java.util.List;
import mx.negocio360.compartido.graphql.ComandoDeclarado;
import mx.negocio360.compartido.graphql.OperacionDeclarada;
import mx.negocio360.compartido.graphql.OperacionesGraphql;
import mx.negocio360.inventario.seguridad.PermisosDeInventario;
import org.springframework.stereotype.Component;

/**
 * El inventario de operaciones de GraphQL de Inventario (SG-21) y el permiso de cada una (SG-03 y SG-04). Ninguna es
 * publica: todas piden sesion con negocio activo y su permiso. La puerta de la plataforma lo comprueba antes y el canal
 * vuelve a exigirlo, tambien al confirmar y antes de gastar el token (D-126).
 */
@Component
public final class OperacionesDeInventario implements OperacionesGraphql {

    @Override
    public List<OperacionDeclarada> operaciones() {
        return List.of(
                OperacionDeclarada.de(QUERY, "inventario", PermisosDeInventario.ARTICULO_VER),
                OperacionDeclarada.de(QUERY, "articulo", PermisosDeInventario.ARTICULO_VER),
                OperacionDeclarada.de(QUERY, "kardex", PermisosDeInventario.ARTICULO_VER));
    }

    @Override
    public List<ComandoDeclarado> comandos() {
        return List.of(
                new ComandoDeclarado("crearArticulo", PermisosDeInventario.ARTICULO_GESTIONAR),
                new ComandoDeclarado("editarArticulo", PermisosDeInventario.ARTICULO_GESTIONAR),
                new ComandoDeclarado("definirUnidadPropia", PermisosDeInventario.ARTICULO_GESTIONAR),
                new ComandoDeclarado("bajaArticulo", PermisosDeInventario.ARTICULO_GESTIONAR),
                new ComandoDeclarado("registrarEntradaInventario", PermisosDeInventario.MOVIMIENTO_REGISTRAR),
                new ComandoDeclarado("registrarSalidaInventario", PermisosDeInventario.MOVIMIENTO_REGISTRAR),
                new ComandoDeclarado("registrarMermaInventario", PermisosDeInventario.MOVIMIENTO_REGISTRAR),
                new ComandoDeclarado("ajustarPorConteo", PermisosDeInventario.CONTEO_AJUSTAR));
    }
}
