package mx.negocio360.identidad.infraestructura;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioSesiones;
import mx.negocio360.identidad.dominio.Sesion;

/**
 * Sesiones en memoria, por hash de token. Solo para desarrollo, demo y pruebas. El orden de insercion es el orden de
 * antiguedad; actualizar una sesion no cambia su lugar.
 */
public final class RepositorioSesionesEnMemoria implements RepositorioSesiones {

    private final Map<String, Sesion> porHash = new LinkedHashMap<>();

    @Override
    public synchronized void guardar(Sesion sesion) {
        porHash.put(sesion.tokenHash(), sesion);
    }

    @Override
    public synchronized Optional<Sesion> porHash(String tokenHash) {
        return Optional.ofNullable(porHash.get(tokenHash));
    }

    @Override
    public synchronized List<Sesion> vigentesDe(UUID cuentaId, Instant ahora) {
        return porHash.values().stream().filter(s -> s.cuentaId().equals(cuentaId) && s.vigente(ahora)).toList();
    }

    /** Todas las sesiones, para las pruebas. */
    public synchronized List<Sesion> todas() {
        return new ArrayList<>(porHash.values());
    }
}
