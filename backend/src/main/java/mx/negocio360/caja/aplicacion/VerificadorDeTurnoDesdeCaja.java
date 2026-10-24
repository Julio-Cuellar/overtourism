package mx.negocio360.caja.aplicacion;

import java.time.Clock;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.caja.aplicacion.puertos.RepositorioCaja;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.ventas.VerificadorDeTurno;

/**
 * Respuesta de Caja a la pregunta de Ventas «¿el turno sigue abierto?» (VT-01). Ventas define la interfaz y Caja la
 * implementa aqui, asi la dependencia sigue yendo solo de Caja hacia Ventas (D-135).
 *
 * <p>Usa el estado efectivo (D-140): un turno EN_CORTE no vende, pero si el corte preparado ya vencio vuelve a vender.
 */
public final class VerificadorDeTurnoDesdeCaja implements VerificadorDeTurno {

    private final RepositorioCaja cajas;
    private final Clock reloj;

    /**
     * Arma el verificador.
     *
     * @param cajas tablas caja y turno
     * @param reloj reloj de la plataforma
     */
    public VerificadorDeTurnoDesdeCaja(RepositorioCaja cajas, Clock reloj) {
        this.cajas = Objects.requireNonNull(cajas, "cajas");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public boolean abierto(ContextoNegocio contexto, UUID turnoId) {
        if (turnoId == null) {
            return false;
        }
        var ahora = reloj.instant();
        return cajas.turno(contexto.negocioId(), turnoId)
                .map(turno -> turno.estadoEn(ahora) == EstadoTurno.ABIERTO).orElse(false);
    }
}
