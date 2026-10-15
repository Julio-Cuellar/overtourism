package mx.negocio360.contabilidad.contratos;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que Contabilidad lee de {@code venta.registrada} v1 (seccion 8.1 del diseno). {@code venta_id} y {@code total}
 * estan en el contrato de {@code inventario.md} §7.2; {@code medio_pago} y {@code caja_sesion_id} son campos nuevos y
 * opcionales que propone D-88.
 *
 * @param ventaId      venta
 * @param total        total cobrado, neto de cupon cuando aplica (D-65)
 * @param medioPago    EFECTIVO, TARJETA, TRANSFERENCIA u otro; nulo si Ventas no lo manda
 * @param cajaSesionId sesion de caja, o nula
 */
public record VentaRegistradaV1(UUID ventaId, BigDecimal total, String medioPago, UUID cajaSesionId) {

    /** Tipo del evento en el sobre. */
    public static final String TIPO = "venta.registrada";

    /**
     * Lee los datos del evento.
     *
     * @param datos carga del sobre
     * @return el contrato leido
     * @throws DatosInvalidosException si falta {@code venta_id} o {@code total}, o si un campo no tiene forma valida
     */
    public static VentaRegistradaV1 leer(Map<String, Object> datos) {
        var lector = new LectorDatos(datos);
        return new VentaRegistradaV1(lector.uuid("venta_id"), lector.dinero("total"), lector.texto("medio_pago"),
                lector.uuidOpcional("caja_sesion_id"));
    }
}
