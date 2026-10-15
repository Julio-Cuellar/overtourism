package mx.negocio360.contabilidad.fakes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioCortesContables;
import mx.negocio360.contabilidad.dominio.CorteContable;

/** Doble en memoria de corte_contable, con llave (negocio, corte). */
public final class RepositorioCortesContablesEnMemoria implements RepositorioCortesContables {

    private final Map<String, CorteContable> filas = new LinkedHashMap<>();

    @Override
    public synchronized void guardar(CorteContable corte) {
        filas.put(corte.negocioId() + "/" + corte.corteId(), corte);
    }

    @Override
    public synchronized Optional<CorteContable> porCorte(UUID negocioId, UUID corteId) {
        return Optional.ofNullable(filas.get(negocioId + "/" + corteId));
    }

    @Override
    public synchronized Optional<CorteContable> deSesion(UUID negocioId, UUID cajaSesionId) {
        return filas.values().stream()
                .filter(c -> c.negocioId().equals(negocioId) && c.cajaSesionId().equals(cajaSesionId)).findFirst();
    }

    @Override
    public synchronized List<CorteContable> delNegocio(UUID negocioId) {
        return filas.values().stream().filter(c -> c.negocioId().equals(negocioId)).toList();
    }
}
