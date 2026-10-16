package mx.negocio360.caja.dominio;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import mx.negocio360.compartido.dinero.Redondeo;

/** Cifras de Caja calculadas solo por el codigo (CJ-12). Funciones puras. */
public final class CalculoCaja {

    private CalculoCaja() {
    }

    /**
     * Esperado por metodo: la suma de los movimientos del turno en cada metodo (CJ-12).
     *
     * @param movimientos movimientos del turno
     * @return los tres metodos, en orden, escala 2 (0.00 si no hubo movimientos en ese metodo)
     */
    public static Map<MetodoPago, BigDecimal> esperado(List<MovimientoCaja> movimientos) {
        var esperado = new EnumMap<MetodoPago, BigDecimal>(MetodoPago.class);
        for (var metodo : MetodoPago.values()) {
            esperado.put(metodo, BigDecimal.ZERO);
        }
        movimientos.forEach(m -> esperado.merge(m.metodo(), m.monto(), BigDecimal::add));
        esperado.replaceAll((metodo, monto) -> Redondeo.dinero(monto));
        return Collections.unmodifiableMap(esperado);
    }
}
