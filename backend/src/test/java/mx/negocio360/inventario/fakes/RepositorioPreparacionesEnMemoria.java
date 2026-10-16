package mx.negocio360.inventario.fakes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import mx.negocio360.inventario.aplicacion.Preparacion;
import mx.negocio360.inventario.aplicacion.puertos.RepositorioPreparaciones;

/** Doble en memoria de la tabla preparacion de Inventario. Solo guarda el hash del token, como la tabla real. */
public final class RepositorioPreparacionesEnMemoria implements RepositorioPreparaciones {

    private final Map<String, Preparacion> filas = new LinkedHashMap<>();

    /** Cuando es verdadero, otra peticion gana la carrera por usar el token: marcarUsada responde falso. */
    public boolean simularCarreraDeUso;

    @Override
    public void guardar(Preparacion preparacion) {
        filas.put(preparacion.tokenHash(), preparacion);
    }

    @Override
    public Optional<Preparacion> porHash(String tokenHash) {
        return Optional.ofNullable(filas.get(tokenHash));
    }

    @Override
    public boolean marcarUsada(String tokenHash, Instant ahora) {
        var fila = filas.get(tokenHash);
        if (simularCarreraDeUso || fila == null || fila.usadoEn() != null) {
            return false;
        }
        filas.put(tokenHash, fila.usada(ahora));
        return true;
    }

    /** Cuantas preparaciones se han guardado. */
    public int cuantas() {
        return filas.size();
    }

    /** La unica fila guardada; falla si hay cero o varias. */
    public Preparacion unica() {
        if (filas.size() != 1) {
            throw new IllegalStateException("Se esperaba una sola preparacion y hay " + filas.size());
        }
        return filas.values().iterator().next();
    }

    /** La preparacion guardada mas recientemente; falla si no hay ninguna. */
    public Preparacion ultima() {
        if (filas.isEmpty()) {
            throw new IllegalStateException("No hay preparaciones");
        }
        return filas.values().stream().reduce((anterior, siguiente) -> siguiente).orElseThrow();
    }

    /** Simula que alguien altero el comando guardado sin tocar su hash. */
    public void alterarComando(String tokenHash, String comandoNuevo) {
        var fila = filas.get(tokenHash);
        filas.put(tokenHash, new Preparacion(fila.tokenHash(), fila.negocioId(), fila.actor(), fila.comandoTipo(),
                fila.comandoHash(), comandoNuevo, fila.resumen(), fila.expiraEn(), fila.usadoEn()));
    }
}
