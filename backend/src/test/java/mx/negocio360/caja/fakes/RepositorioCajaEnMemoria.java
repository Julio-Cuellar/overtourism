package mx.negocio360.caja.fakes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.caja.aplicacion.puertos.RepositorioCaja;
import mx.negocio360.caja.dominio.Caja;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.caja.dominio.Turno;

/** Doble de las tablas {@code caja} y {@code turno}. */
public final class RepositorioCajaEnMemoria implements RepositorioCaja {

    private final Map<UUID, Caja> cajas = new LinkedHashMap<>();
    private final Map<UUID, Turno> turnos = new LinkedHashMap<>();

    /** Cuando es verdadero, otra peticion cambio el turno antes: actualizarTurno responde falso. */
    public boolean simularConcurrencia;

    @Override
    public void guardarCaja(Caja caja) {
        cajas.put(caja.id(), caja);
    }

    @Override
    public Optional<Caja> caja(UUID negocioId, UUID cajaId) {
        return Optional.ofNullable(cajas.get(cajaId)).filter(c -> c.negocioId().equals(negocioId));
    }

    @Override
    public List<Caja> cajas(UUID negocioId) {
        return cajas.values().stream().filter(c -> c.negocioId().equals(negocioId)).toList();
    }

    @Override
    public Optional<Turno> turno(UUID negocioId, UUID turnoId) {
        return Optional.ofNullable(turnos.get(turnoId)).filter(t -> t.negocioId().equals(negocioId));
    }

    @Override
    public Optional<Turno> turnoNoCerradoDeCaja(UUID negocioId, UUID cajaId) {
        return turnos.values().stream()
                .filter(t -> t.negocioId().equals(negocioId) && t.cajaId().equals(cajaId))
                .filter(t -> t.estado() != EstadoTurno.CERRADO)
                .findFirst();
    }

    @Override
    public void guardarTurno(Turno turno) {
        turnos.put(turno.id(), turno);
    }

    @Override
    public boolean actualizarTurno(Turno turno, int versionEsperada) {
        var actual = turnos.get(turno.id());
        if (simularConcurrencia || actual == null || actual.version() != versionEsperada) {
            return false;
        }
        turnos.put(turno.id(), turno);
        return true;
    }
}
