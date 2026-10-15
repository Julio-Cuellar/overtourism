package mx.negocio360.contabilidad.fakes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioCanjesContables;
import mx.negocio360.contabilidad.dominio.CanjeContable;

/** Doble en memoria de canje_contable, con llave (negocio, cupon). */
public final class RepositorioCanjesContablesEnMemoria implements RepositorioCanjesContables {

    private final Map<String, CanjeContable> filas = new LinkedHashMap<>();

    @Override
    public synchronized void guardar(CanjeContable canje) {
        filas.put(canje.negocioId() + "/" + canje.cuponId(), canje);
    }

    @Override
    public synchronized Optional<CanjeContable> porCupon(UUID negocioId, UUID cuponId) {
        return Optional.ofNullable(filas.get(negocioId + "/" + cuponId));
    }
}
