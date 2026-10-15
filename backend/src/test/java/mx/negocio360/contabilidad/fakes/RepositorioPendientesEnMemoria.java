package mx.negocio360.contabilidad.fakes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPendientes;
import mx.negocio360.contabilidad.dominio.EstadoPendiente;
import mx.negocio360.contabilidad.dominio.Pendiente;

/** Doble en memoria de pendiente_contable. */
public final class RepositorioPendientesEnMemoria implements RepositorioPendientes {

    private final Map<UUID, Pendiente> filas = new LinkedHashMap<>();

    @Override
    public synchronized void guardar(Pendiente pendiente) {
        filas.put(pendiente.id(), pendiente);
    }

    @Override
    public synchronized Optional<Pendiente> porId(UUID negocioId, UUID pendienteId) {
        return Optional.ofNullable(filas.get(pendienteId)).filter(p -> p.negocioId().equals(negocioId));
    }

    @Override
    public synchronized List<Pendiente> delNegocio(UUID negocioId, EstadoPendiente estado) {
        return filas.values().stream().filter(p -> p.negocioId().equals(negocioId) && p.estado() == estado)
                .toList();
    }
}
