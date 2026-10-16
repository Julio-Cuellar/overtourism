package mx.negocio360.ventas.fakes;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.ventas.VerificadorDeTurno;

/**
 * Doble del verificador de turno que Caja implementa (VT-01). Guarda los turnos abiertos por negocio; la prueba de
 * punta a punta lo conecta con el verificador real de Caja con {@link #delegarEn}.
 */
public final class TurnosEnMemoria implements VerificadorDeTurno {

    private final Set<String> abiertos = new HashSet<>();
    private VerificadorDeTurno delegado;

    public TurnosEnMemoria abrir(UUID negocioId, UUID turnoId) {
        abiertos.add(negocioId + "/" + turnoId);
        return this;
    }

    public void cerrar(UUID negocioId, UUID turnoId) {
        abiertos.remove(negocioId + "/" + turnoId);
    }

    /** A partir de aqui responde el verificador real. */
    public void delegarEn(VerificadorDeTurno real) {
        delegado = real;
    }

    @Override
    public boolean abierto(ContextoNegocio contexto, UUID turnoId) {
        if (delegado != null) {
            return delegado.abierto(contexto, turnoId);
        }
        return abiertos.contains(contexto.negocioId() + "/" + turnoId);
    }
}
