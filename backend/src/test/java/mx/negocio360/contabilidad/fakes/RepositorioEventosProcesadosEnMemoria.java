package mx.negocio360.contabilidad.fakes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioEventosProcesados;
import mx.negocio360.contabilidad.dominio.EventoProcesado;

/** Doble en memoria de evento_procesado, con llave (negocio, evento). */
public final class RepositorioEventosProcesadosEnMemoria implements RepositorioEventosProcesados {

    private final Map<String, EventoProcesado> filas = new LinkedHashMap<>();

    @Override
    public synchronized Optional<EventoProcesado> de(UUID negocioId, UUID eventoId) {
        return Optional.ofNullable(filas.get(negocioId + "/" + eventoId));
    }

    @Override
    public synchronized boolean insertar(EventoProcesado evento) {
        return filas.putIfAbsent(evento.negocioId() + "/" + evento.eventoId(), evento) == null;
    }

    /** Todas las filas, en orden de procesamiento. */
    public synchronized List<EventoProcesado> todas() {
        return List.copyOf(filas.values());
    }
}
