package mx.negocio360.contabilidad.contratos;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Lo que Contabilidad lee de {@code cupon.canjeado} v1 (seccion 8.1 del diseno). La forma viene del mockup de la app
 * del turista (E-07): {@code cupon_id}, {@code venta_id}, {@code descuento.monto}. {@code financiador} es el campo
 * nuevo y opcional de D-103; si falta se asume {@code NEGOCIO} (K-3). Ignora {@code visitante_id} y
 * {@code descuento.moneda}.
 *
 * @param cuponId        cupon; llave natural, un cupon se canjea una vez (E-02)
 * @param ventaId        venta en que se uso, o nula (U-03)
 * @param montoDescuento descuento con IVA, como lo ve el turista; nulo si no vino
 * @param financiador    NEGOCIO o PLATAFORMA
 */
public record CuponCanjeadoV1(UUID cuponId, UUID ventaId, BigDecimal montoDescuento, String financiador) {

    /** Tipo del evento en el sobre. */
    public static final String TIPO = "cupon.canjeado";
    /** Unica version del contrato que Contabilidad sabe leer. */
    public static final int VERSION = 1;
    /** Quien paga el descuento cuando el evento no lo dice (K-3). */
    public static final String FINANCIADOR_POR_DEFECTO = "NEGOCIO";
    /** Campos del contrato que usa Contabilidad; son los unicos que se copian a un pendiente (E-04). */
    public static final List<String> CAMPOS = List.of("cupon_id", "venta_id", "descuento", "financiador");

    /**
     * Lee los datos del evento.
     *
     * @param datos carga del sobre
     * @return el contrato leido
     * @throws DatosInvalidosException si falta {@code cupon_id} o un campo no tiene forma valida
     */
    public static CuponCanjeadoV1 leer(Map<String, Object> datos) {
        var lector = new LectorDatos(datos);
        var financiador = lector.texto("financiador");
        return new CuponCanjeadoV1(lector.uuid("cupon_id"), lector.uuidOpcional("venta_id"),
                lector.objeto("descuento").dineroOpcional("monto"),
                financiador == null ? FINANCIADOR_POR_DEFECTO : financiador);
    }
}
