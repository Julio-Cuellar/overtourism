package mx.negocio360.caja.aplicacion;

import java.util.Objects;
import java.util.UUID;
import mx.negocio360.caja.aplicacion.puertos.ConteoDeVentas;
import mx.negocio360.caja.aplicacion.puertos.ResumenVentas;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.ventas.VentasApi;

/**
 * Adaptador del puerto {@link ResumenVentas} sobre la API publica de Ventas (J1 de {@code caja.md}). Es el unico punto
 * donde Caja llama a Ventas, y en un solo sentido (D-135).
 */
public final class ResumenVentasDesdeVentasApi implements ResumenVentas {

    private final VentasApi ventas;

    /**
     * Arma el adaptador.
     *
     * @param ventas API publica de Ventas
     */
    public ResumenVentasDesdeVentasApi(VentasApi ventas) {
        this.ventas = Objects.requireNonNull(ventas, "ventas");
    }

    @Override
    public ConteoDeVentas deTurno(ContextoNegocio contexto, UUID turnoId) {
        var resumen = ventas.resumenTurno(contexto, turnoId);
        return new ConteoDeVentas(resumen.ventasRegistradas(), resumen.ventasAnuladas(),
                resumen.preparacionesVigentes());
    }
}
