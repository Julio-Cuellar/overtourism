package mx.negocio360.contabilidad.fakes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioSaldos;
import mx.negocio360.contabilidad.dominio.Saldo;

/** Doble en memoria de saldo_cuenta, con llave (negocio, cuenta, periodo). */
public final class RepositorioSaldosEnMemoria implements RepositorioSaldos {

    private final Map<String, Saldo> filas = new LinkedHashMap<>();

    private static String llave(UUID negocioId, UUID cuentaId, UUID periodoId) {
        return negocioId + "/" + cuentaId + "/" + periodoId;
    }

    @Override
    public synchronized void guardar(Saldo saldo) {
        filas.put(llave(saldo.negocioId(), saldo.cuentaId(), saldo.periodoId()), saldo);
    }

    @Override
    public synchronized Optional<Saldo> de(UUID negocioId, UUID cuentaId, UUID periodoId) {
        return Optional.ofNullable(filas.get(llave(negocioId, cuentaId, periodoId)));
    }

    @Override
    public synchronized List<Saldo> deCuenta(UUID negocioId, UUID cuentaId) {
        return filas.values().stream().filter(s -> s.negocioId().equals(negocioId) && s.cuentaId().equals(cuentaId))
                .toList();
    }
}
