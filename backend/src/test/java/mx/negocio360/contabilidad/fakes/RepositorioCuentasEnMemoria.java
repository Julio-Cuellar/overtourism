package mx.negocio360.contabilidad.fakes;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.contabilidad.dominio.Cuenta;

/** Doble en memoria de la tabla cuenta. Filtra por negocio como lo haria la seguridad por fila. */
public final class RepositorioCuentasEnMemoria implements RepositorioCuentas {

    private final Map<UUID, Cuenta> filas = new LinkedHashMap<>();

    @Override
    public void guardar(Cuenta cuenta) {
        filas.put(cuenta.id(), cuenta);
    }

    @Override
    public Optional<Cuenta> porId(UUID negocioId, UUID cuentaId) {
        return Optional.ofNullable(filas.get(cuentaId)).filter(c -> c.negocioId().equals(negocioId));
    }

    @Override
    public Optional<Cuenta> porCodigo(UUID negocioId, String codigo) {
        return filas.values().stream().filter(c -> c.negocioId().equals(negocioId) && c.codigo().equals(codigo))
                .findFirst();
    }

    @Override
    public List<Cuenta> delNegocio(UUID negocioId) {
        return new ArrayList<>(filas.values().stream().filter(c -> c.negocioId().equals(negocioId)).toList());
    }
}
