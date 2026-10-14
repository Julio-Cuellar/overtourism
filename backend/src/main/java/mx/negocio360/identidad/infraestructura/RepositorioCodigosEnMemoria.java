package mx.negocio360.identidad.infraestructura;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioCodigos;
import mx.negocio360.identidad.dominio.CodigoAcceso;

/**
 * Codigos en memoria. Solo para desarrollo, demo y pruebas. El orden de insercion decide cual es el ultimo de un
 * telefono; actualizar un codigo no cambia su lugar.
 */
public final class RepositorioCodigosEnMemoria implements RepositorioCodigos {

    private final Map<UUID, CodigoAcceso> porId = new LinkedHashMap<>();

    @Override
    public synchronized void guardar(CodigoAcceso codigo) {
        porId.put(codigo.id(), codigo);
    }

    @Override
    public synchronized Optional<CodigoAcceso> ultimoDe(String telefono) {
        CodigoAcceso ultimo = null;
        for (var codigo : porId.values()) {
            if (codigo.telefono().equals(telefono)) {
                ultimo = codigo;
            }
        }
        return Optional.ofNullable(ultimo);
    }
}
