package mx.negocio360.caja.graphql;

import static mx.negocio360.compartido.graphql.TipoDeOperacion.QUERY;

import java.util.List;
import mx.negocio360.caja.seguridad.PermisosDeCaja;
import mx.negocio360.compartido.graphql.ComandoDeclarado;
import mx.negocio360.compartido.graphql.OperacionDeclarada;
import mx.negocio360.compartido.graphql.OperacionesGraphql;
import org.springframework.stereotype.Component;

/**
 * El inventario de operaciones de GraphQL de Caja (SG-21) y el permiso de cada una (SG-03 y SG-04, seccion 7.3 de
 * {@code docs/planes/integracion-ventas-caja.md}). Ninguna es publica. Registrar un retiro y confirmar el corte con
 * autorizacion piden ademas un segundo permiso que comprueba el servicio, tambien al confirmar (D-126).
 */
@Component
public final class OperacionesDeCaja implements OperacionesGraphql {

    @Override
    public List<OperacionDeclarada> operaciones() {
        return List.of(
                OperacionDeclarada.de(QUERY, "cajas", PermisosDeCaja.TURNO_VER),
                OperacionDeclarada.de(QUERY, "turnoActivo", PermisosDeCaja.TURNO_VER),
                OperacionDeclarada.de(QUERY, "saldoTurno", PermisosDeCaja.TURNO_VER),
                OperacionDeclarada.de(QUERY, "cortes", PermisosDeCaja.CORTE_VER));
    }

    @Override
    public List<ComandoDeclarado> comandos() {
        return List.of(
                new ComandoDeclarado("crearCaja", PermisosDeCaja.CAJA_GESTIONAR),
                new ComandoDeclarado("abrirTurno", PermisosDeCaja.TURNO_ABRIR),
                new ComandoDeclarado("registrarMovimientoCaja", PermisosDeCaja.MOVIMIENTO_REGISTRAR),
                new ComandoDeclarado("cerrarTurno", PermisosDeCaja.CORTE_CERRAR),
                new ComandoDeclarado("descartarCorte", PermisosDeCaja.CORTE_CERRAR));
    }
}
