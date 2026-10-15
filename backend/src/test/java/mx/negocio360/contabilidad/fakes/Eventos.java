package mx.negocio360.contabilidad.fakes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;

/**
 * Eventos de ejemplo de los otros modulos, con la forma de los contratos de {@code contabilidad-diagramas.md} §4.
 * Los montos son simulados.
 */
public final class Eventos {

    private Eventos() {
    }

    /** {@code venta.registrada} v1 con medio de pago y, si no es nula, sesion de caja. */
    public static SobreEvento venta(UUID negocioId, UUID ventaId, String total, String medioPago, Instant ocurridoEn,
                                    UUID cajaSesionId) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("venta_id", ventaId.toString());
        datos.put("total", total);
        if (medioPago != null) {
            datos.put("medio_pago", medioPago);
        }
        if (cajaSesionId != null) {
            datos.put("caja_sesion_id", cajaSesionId.toString());
        }
        return sobre("venta.registrada", 1, negocioId, "ventas", ocurridoEn, ventaId, datos);
    }

    /** {@code venta.registrada} v1 en efectivo, sin sesion de caja. */
    public static SobreEvento venta(UUID negocioId, String total, String medioPago, Instant ocurridoEn) {
        return venta(negocioId, UUID.randomUUID(), total, medioPago, ocurridoEn, null);
    }

    /**
     * {@code cupon.canjeado} v1 con la forma del mockup de la app (E-07). {@code ventaId} y {@code financiador} pueden ser
     * nulos; {@code monto} nulo omite el descuento.
     */
    public static SobreEvento canje(UUID negocioId, UUID cuponId, UUID ventaId, String monto, String financiador,
                                    Instant canjeadoEn) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("cupon_id", cuponId.toString());
        datos.put("visitante_id", UUID.randomUUID().toString());
        if (ventaId != null) {
            datos.put("venta_id", ventaId.toString());
        }
        if (monto != null) {
            datos.put("descuento", Map.of("monto", monto, "moneda", "MXN"));
        }
        datos.put("canjeado_en", canjeadoEn.toString());
        if (financiador != null) {
            datos.put("financiador", financiador);
        }
        return sobre("cupon.canjeado", 1, negocioId, "pasaporte", canjeadoEn, ventaId == null ? cuponId : ventaId,
                datos);
    }

    /**
     * {@code caja.cerrada} v1 con el contrato propuesto (Caja no tiene diseno). {@code ventasEfectivo} es opcional.
     */
    public static SobreEvento corte(UUID negocioId, UUID corteId, UUID cajaSesionId, String esperado, String contado,
                                    String ventasEfectivo, Instant cerradaEn) {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("corte_id", corteId.toString());
        datos.put("caja_sesion_id", cajaSesionId.toString());
        datos.put("efectivo_esperado", esperado);
        datos.put("efectivo_contado", contado);
        if (ventasEfectivo != null) {
            datos.put("ventas_efectivo", ventasEfectivo);
        }
        datos.put("cerrada_en", cerradaEn.toString());
        return sobre("caja.cerrada", 1, negocioId, "caja", cerradaEn, cajaSesionId, datos);
    }

    /** Sobre con id UUIDv7 nuevo. */
    public static SobreEvento sobre(String tipo, int version, UUID negocioId, String origen, Instant ocurridoEn,
                                    UUID correlacionId, Map<String, Object> datos) {
        return new SobreEvento(UuidV7.generar(ocurridoEn), tipo, version, negocioId, origen, ocurridoEn,
                correlacionId, null, datos);
    }
}
