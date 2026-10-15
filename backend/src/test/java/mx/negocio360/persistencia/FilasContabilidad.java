package mx.negocio360.persistencia;

import java.sql.SQLException;
import java.util.UUID;

/**
 * Filas minimas de Contabilidad para las pruebas contra PostgreSQL: un periodo, dos cuentas y una poliza cuadrada con
 * dos movimientos. La sesion debe tener fijado el negocio.
 *
 * @param periodo periodo de octubre de 2026
 * @param caja    cuenta 101.01
 * @param ventas  cuenta 401.01
 * @param poliza  poliza de ingreso por 10.00
 */
record FilasContabilidad(UUID periodo, UUID caja, UUID ventas, UUID poliza) {

    /** Siembra las filas del negocio en la sesion. */
    static FilasContabilidad sembrar(SesionSql sesion, UUID negocio) throws SQLException {
        var periodo = UUID.randomUUID();
        var caja = UUID.randomUUID();
        var ventas = UUID.randomUUID();
        var poliza = UUID.randomUUID();
        sesion.ejecutar("insert into contabilidad.periodo (id, negocio_id, anio, mes, estado, abierto_en) "
                + "values (?, ?, 2026, 10, 'ABIERTO', now())", periodo, negocio);
        sesion.ejecutar("insert into contabilidad.cuenta (id, negocio_id, codigo, nombre, tipo, naturaleza, afectable, "
                + "de_sistema, estado) values (?, ?, '101.01', 'Caja', 'ACTIVO', 'DEUDORA', true, true, 'ACTIVA')",
                caja, negocio);
        sesion.ejecutar("insert into contabilidad.cuenta (id, negocio_id, codigo, nombre, tipo, naturaleza, afectable, "
                + "de_sistema, estado) values (?, ?, '401.01', 'Ventas', 'INGRESO', 'ACREEDORA', true, true, 'ACTIVA')",
                ventas, negocio);
        sesion.ejecutar("insert into contabilidad.poliza (id, negocio_id, periodo_id, tipo, folio, fecha, concepto, origen, "
                + "extemporanea, total, creado_por, creado_en) values (?, ?, ?, 'INGRESO', 1, date '2026-10-15', "
                + "'Venta', 'MANUAL', false, 10.00, 'prueba', now())", poliza, negocio, periodo);
        sesion.ejecutar("insert into contabilidad.movimiento (id, negocio_id, poliza_id, renglon, cuenta_id, cargo, abono) "
                + "values (gen_random_uuid(), ?, ?, 1, ?, 10.00, 0)", negocio, poliza, caja);
        sesion.ejecutar("insert into contabilidad.movimiento (id, negocio_id, poliza_id, renglon, cuenta_id, cargo, abono) "
                + "values (gen_random_uuid(), ?, ?, 2, ?, 0, 10.00)", negocio, poliza, ventas);
        return new FilasContabilidad(periodo, caja, ventas, poliza);
    }
}
