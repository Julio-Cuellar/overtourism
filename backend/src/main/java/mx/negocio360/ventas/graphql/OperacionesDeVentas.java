package mx.negocio360.ventas.graphql;

import static mx.negocio360.compartido.graphql.TipoDeOperacion.QUERY;

import java.util.List;
import mx.negocio360.compartido.graphql.ComandoDeclarado;
import mx.negocio360.compartido.graphql.OperacionDeclarada;
import mx.negocio360.compartido.graphql.OperacionesGraphql;
import mx.negocio360.ventas.seguridad.PermisosDeVentas;
import org.springframework.stereotype.Component;

/**
 * El inventario de operaciones de GraphQL de Ventas (SG-21) y el permiso de cada una (SG-03 y SG-04, seccion 7.3 de
 * {@code docs/planes/integracion-ventas-caja.md}). Ninguna es publica: todas piden sesion con negocio activo y su permiso.
 * La puerta de la plataforma lo comprueba antes y el servicio vuelve a hacerlo: dos barreras (D-126).
 */
@Component
public final class OperacionesDeVentas implements OperacionesGraphql {

    @Override
    public List<OperacionDeclarada> operaciones() {
        return List.of(
                OperacionDeclarada.de(QUERY, "venta", PermisosDeVentas.VENTA_VER),
                OperacionDeclarada.de(QUERY, "ventas", PermisosDeVentas.VENTA_VER),
                OperacionDeclarada.de(QUERY, "resumenTurno", PermisosDeVentas.VENTA_VER),
                OperacionDeclarada.de(QUERY, "productosParaVenta", PermisosDeVentas.VENTA_REGISTRAR));
    }

    @Override
    public List<ComandoDeclarado> comandos() {
        return List.of(
                new ComandoDeclarado("registrarVenta", PermisosDeVentas.VENTA_REGISTRAR),
                new ComandoDeclarado("anularVenta", PermisosDeVentas.VENTA_ANULAR));
    }
}
