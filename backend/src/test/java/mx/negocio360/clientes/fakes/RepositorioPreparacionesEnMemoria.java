package mx.negocio360.clientes.fakes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.clientes.aplicacion.Preparacion;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioPreparaciones;

/** Doble en memoria de la tabla preparacion. Solo guarda el hash del token, como la tabla real. */
public final class RepositorioPreparacionesEnMemoria implements RepositorioPreparaciones {

    private final Map<String, Preparacion> filas = new LinkedHashMap<>();

    /** Cuando es verdadero, otra peticion gana la carrera por usar el token: marcarUsada responde falso. */
    public boolean simularCarreraDeUso;

    @Override
    public void guardar(Preparacion preparacion) {
        filas.put(preparacion.tokenHash(), preparacion);
    }

    @Override
    public Optional<Preparacion> porHash(UUID negocioId, String tokenHash) {
        return Optional.ofNullable(filas.get(tokenHash)).filter(f -> f.negocioId().equals(negocioId));
    }

    @Override
    public boolean marcarUsada(UUID negocioId, String tokenHash, Instant ahora) {
        var fila = filas.get(tokenHash);
        if (simularCarreraDeUso || fila == null || !fila.negocioId().equals(negocioId) || fila.usadoEn() != null) {
            return false;
        }
        filas.put(tokenHash, fila.usada(ahora));
        return true;
    }

    /** Cuando es verdadero, vaciar falla como lo haria una base caida. */
    public boolean fallarAlVaciar;

    @Override
    public void vaciar(UUID negocioId, String tokenHash) {
        if (fallarAlVaciar) {
            throw new IllegalStateException("fallo simulado al vaciar");
        }
        var fila = filas.get(tokenHash);
        if (fila != null && fila.negocioId().equals(negocioId)) {
            filas.put(tokenHash, fila.vaciada());
        }
    }

    public List<Preparacion> todas() {
        return List.copyOf(filas.values());
    }

    /** La unica fila guardada; falla si hay cero o varias. */
    public Preparacion unica() {
        if (filas.size() != 1) {
            throw new IllegalStateException("Se esperaba una sola preparacion y hay " + filas.size());
        }
        return filas.values().iterator().next();
    }

    /** Simula a quien altera el comando y tambien recalcula su hash, de modo que el hash no lo delata. */
    public void reemplazarComandoYHash(String tokenHash, String comandoNuevo, String comandoHashNuevo) {
        var fila = filas.get(tokenHash);
        filas.put(tokenHash, new Preparacion(fila.tokenHash(), fila.negocioId(), fila.actor(), fila.comandoTipo(),
                comandoHashNuevo, comandoNuevo, fila.resumen(), fila.expiraEn(), fila.usadoEn()));
    }

    /** Simula que alguien altero el comando guardado sin tocar su hash. */
    public void alterarComando(String tokenHash, String comandoNuevo) {
        var fila = filas.get(tokenHash);
        filas.put(tokenHash, new Preparacion(fila.tokenHash(), fila.negocioId(), fila.actor(), fila.comandoTipo(),
                fila.comandoHash(), comandoNuevo, fila.resumen(), fila.expiraEn(), fila.usadoEn()));
    }
}
