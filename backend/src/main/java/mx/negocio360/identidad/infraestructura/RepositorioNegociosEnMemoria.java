package mx.negocio360.identidad.infraestructura;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioNegocios;
import mx.negocio360.identidad.dominio.NegocioReferencia;

/** Negocios de referencia en memoria. Solo para desarrollo, demo y pruebas: se pierden al reiniciar. */
public final class RepositorioNegociosEnMemoria implements RepositorioNegocios {

    private final Map<UUID, NegocioReferencia> porId = new LinkedHashMap<>();

    @Override
    public synchronized NegocioReferencia guardar(NegocioReferencia negocio) {
        porId.put(negocio.negocioId(), negocio);
        return negocio;
    }

    @Override
    public synchronized Optional<NegocioReferencia> porId(UUID negocioId) {
        return Optional.ofNullable(porId.get(negocioId));
    }
}
