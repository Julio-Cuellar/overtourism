package mx.negocio360.caja.eventos;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import mx.negocio360.compartido.dinero.DecimalPlano;

/**
 * {@code caja.diferencia_detectada} v1: se publica cuando un corte cierra con una diferencia en efectivo mayor al
 * umbral (CJ-08). Datos: {@code turno_id}, {@code caja_id}, {@code diferencia}, {@code umbral}, {@code motivo}.
 *
 * @param turnoId    turno
 * @param cajaId     caja
 * @param diferencia contado − esperado en efectivo, escala 2
 * @param umbral     umbral vigente del negocio, escala 2
 */
public record DiferenciaDetectada(UUID turnoId, UUID cajaId, BigDecimal diferencia, BigDecimal umbral) {

    /** Tipo del evento en el sobre. */
    public static final String TIPO = "caja.diferencia_detectada";
    /** Version del contrato (R5). */
    public static final int VERSION = 1;
    /** Motivo del MVP; CJ-11 agregara {@code VENTA_TARDIA}. */
    public static final String MOTIVO_CORTE = "CORTE";

    /**
     * Carga del sobre con las claves del contrato.
     *
     * @return mapa listo para {@code SobreEvento}
     */
    public Map<String, Object> datos() {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("turno_id", turnoId.toString());
        datos.put("caja_id", cajaId.toString());
        datos.put("diferencia", DecimalPlano.escribir(diferencia));
        datos.put("umbral", DecimalPlano.escribir(umbral));
        datos.put("motivo", MOTIVO_CORTE);
        return datos;
    }
}
