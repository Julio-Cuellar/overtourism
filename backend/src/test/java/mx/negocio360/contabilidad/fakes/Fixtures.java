package mx.negocio360.contabilidad.fakes;

import java.math.BigDecimal;
import java.time.Instant;
import mx.negocio360.contabilidad.aplicacion.ActivarContabilidadComando;

/**
 * Datos de ejemplo de las pruebas. Son simulados: la tasa de 16 % no es una tasa verificada contra el SAT (F-01).
 */
public final class Fixtures {

    /** 2026-10-15 12:00 en Puebla (UTC-6). */
    public static final Instant AHORA = Instant.parse("2026-10-15T18:00:00Z");
    public static final BigDecimal TASA_EJEMPLO = new BigDecimal("0.16");
    public static final String FUENTE_EJEMPLO = "Dato de ejemplo de pruebas, no verificado";

    private Fixtures() {
    }

    /** Activacion con precios con IVA, tasa de ejemplo y ejercicio desde enero. */
    public static ActivarContabilidadComando activar() {
        return activar(TASA_EJEMPLO, FUENTE_EJEMPLO);
    }

    /** Activacion con la tasa y la fuente indicadas. */
    public static ActivarContabilidadComando activar(BigDecimal tasa, String fuente) {
        return new ActivarContabilidadComando(true, tasa, fuente, false, 1);
    }
}
