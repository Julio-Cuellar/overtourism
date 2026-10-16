package mx.negocio360.identidad.infraestructura;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioPreregistros;
import mx.negocio360.identidad.dominio.Preregistro;

/** Pre-registros en memoria, uno por correo. Solo para desarrollo, demo y pruebas; los de Postgres cumplen el mismo contrato. */
public final class RepositorioPreregistrosEnMemoria implements RepositorioPreregistros {

    private final Map<String, Preregistro> porCorreo = new LinkedHashMap<>();

    @Override
    public synchronized void guardar(Preregistro preregistro) {
        porCorreo.put(preregistro.correo(), preregistro);
    }

    @Override
    public synchronized Optional<Preregistro> porCorreo(String correo) {
        return Optional.ofNullable(porCorreo.get(correo));
    }

    @Override
    public synchronized boolean eliminar(UUID id) {
        return porCorreo.values().removeIf(p -> p.id().equals(id));
    }

    @Override
    public synchronized int borrarVencidos(Instant ahora) {
        var antes = porCorreo.size();
        porCorreo.values().removeIf(p -> p.vencido(ahora));
        return antes - porCorreo.size();
    }

    /** Todos los pre-registros, para las pruebas. */
    public synchronized List<Preregistro> todos() {
        return new ArrayList<>(porCorreo.values());
    }
}
