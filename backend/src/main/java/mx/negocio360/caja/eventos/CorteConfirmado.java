package mx.negocio360.caja.eventos;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;
import mx.negocio360.caja.dominio.Corte;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.compartido.dinero.DecimalPlano;

/**
 * {@code caja.corte_confirmado} v1: se publica al confirmar CerrarTurno. Datos: {@code corte_id}, {@code turno_id},
 * {@code caja_id}, {@code esperado_por_metodo}, {@code contado_por_metodo}, {@code diferencia_efectivo},
 * {@code abierto_en} y {@code cerrado_en}. Dinero como cadena decimal; sin datos del cajero.
 *
 * @param corte corte confirmado
 */
public record CorteConfirmado(Corte corte) {

    /** Tipo del evento en el sobre. */
    public static final String TIPO = "caja.corte_confirmado";
    /** Version del contrato (R5). */
    public static final int VERSION = 1;

    /**
     * Carga del sobre con las claves del contrato.
     *
     * @return mapa listo para {@code SobreEvento}
     */
    public Map<String, Object> datos() {
        var datos = new LinkedHashMap<String, Object>();
        datos.put("corte_id", corte.id().toString());
        datos.put("turno_id", corte.turnoId().toString());
        datos.put("caja_id", corte.cajaId().toString());
        datos.put("esperado_por_metodo", comoTexto(corte.esperadoPorMetodo()));
        datos.put("contado_por_metodo", comoTexto(corte.contadoPorMetodo()));
        datos.put("diferencia_efectivo", DecimalPlano.escribir(corte.diferenciaEfectivo()));
        datos.put("abierto_en", corte.abiertoEn().toString());
        datos.put("cerrado_en", corte.confirmadoEn().toString());
        return datos;
    }

    private static Map<String, Object> comoTexto(Map<MetodoPago, BigDecimal> montos) {
        var texto = new TreeMap<String, Object>();
        montos.forEach((metodo, monto) -> texto.put(metodo.name(), DecimalPlano.escribir(monto)));
        return texto;
    }
}
