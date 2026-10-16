package mx.negocio360.inventario.dominio;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * A-02: las alertas salen al cruzar el umbral, no en cada movimiento. Comparar el saldo de antes con el de despues
 * hace que una serie de salidas bajo el minimo avise una sola vez.
 */
public final class CruceDeUmbral {

    private CruceDeUmbral() {
    }

    /** Alertas que provoca pasar de {@code antes} a {@code despues}, en orden fijo. */
    public static List<TipoAlerta> entre(BigDecimal antes, BigDecimal despues, BigDecimal minimo) {
        var alertas = new ArrayList<TipoAlerta>();
        var estabaArriba = antes.compareTo(minimo) > 0;
        var quedaArriba = despues.compareTo(minimo) > 0;
        if (estabaArriba && !quedaArriba) {
            alertas.add(TipoAlerta.BAJO_MINIMO);
        }
        if (!estabaArriba && quedaArriba) {
            alertas.add(TipoAlerta.REPUESTO);
        }
        if (antes.signum() >= 0 && despues.signum() < 0) {
            alertas.add(TipoAlerta.SALDO_NEGATIVO);
        }
        return List.copyOf(alertas);
    }
}
