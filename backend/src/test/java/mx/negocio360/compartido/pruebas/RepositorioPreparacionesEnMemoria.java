package mx.negocio360.compartido.pruebas;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.comandos.Preparacion;
import mx.negocio360.compartido.comandos.RepositorioPreparaciones;

/** Doble en memoria de la tabla {@code preparacion}. Solo guarda el hash del token, como la tabla real. */
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

    @Override
    public void vaciar(String tokenHash) {
        var fila = filas.get(tokenHash);
        if (fila != null) {
            filas.put(tokenHash, fila.vaciada());
        }
    }

    @Override
    public long vigentes(UUID negocioId, String comandoTipo, String ambito, Instant ahora) {
        return filas.values().stream()
                .filter(f -> f.negocioId().equals(negocioId) && f.comandoTipo().equals(comandoTipo))
                .filter(f -> Objects.equals(f.ambito(), ambito))
                .filter(f -> f.usadoEn() == null && ahora.isBefore(f.expiraEn()))
                .count();
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

    /** Simula que alguien altero el comando guardado sin tocar su hash. */
    public void alterarComando(String tokenHash, String comandoNuevo) {
        var f = filas.get(tokenHash);
        filas.put(tokenHash, new Preparacion(f.tokenHash(), f.negocioId(), f.actor(), f.comandoTipo(),
                f.comandoHash(), comandoNuevo, f.resumen(), f.ambito(), f.expiraEn(), f.usadoEn()));
    }
}
