package mx.negocio360.caja.aplicacion.puertos;

import java.math.BigDecimal;
import java.util.UUID;

/** Reglas de negocio versionadas de Caja (no se hardcodean). El umbral inicial propuesto es 50.00 [?] (CJ-08). */
public interface ReglasCaja {

    /**
     * Umbral de diferencia en efectivo del negocio.
     *
     * @param negocioId negocio
     * @return diferencia a partir de la cual un corte pide autorizacion, escala 2
     */
    BigDecimal umbralDiferencia(UUID negocioId);
}
