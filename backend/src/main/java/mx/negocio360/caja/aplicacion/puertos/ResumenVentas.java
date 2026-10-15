package mx.negocio360.caja.aplicacion.puertos;

import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;

/**
 * Puerto hacia Ventas para el corte (J1 de {@code caja.md}). La implementacion real es
 * {@code ResumenVentasDesdeVentasApi}, que llama a {@code VentasApi.resumenTurno} en el mismo proceso (D-76).
 */
public interface ResumenVentas {

    /**
     * Conteos de Ventas para un turno (CJ-07).
     *
     * @param contexto negocio
     * @param turnoId  turno
     * @return registradas, anuladas y preparaciones vigentes
     */
    ConteoDeVentas deTurno(ContextoNegocio contexto, UUID turnoId);
}
