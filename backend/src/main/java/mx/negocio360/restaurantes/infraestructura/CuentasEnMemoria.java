package mx.negocio360.restaurantes.infraestructura;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.restaurantes.aplicacion.OcupacionDeMesas;
import mx.negocio360.restaurantes.aplicacion.RepositorioCuentas;
import mx.negocio360.restaurantes.aplicacion.ResumenDeCuenta;
import mx.negocio360.restaurantes.dominio.Comanda;
import mx.negocio360.restaurantes.dominio.Cuenta;
import mx.negocio360.restaurantes.dominio.OrigenDeCuenta;

/**
 * Cuentas y comandas en memoria, para pruebas y demostracion (R7 trae Postgres). Cada operacion es atomica: el indice unico
 * parcial de una cuenta sin pagar por mesa (MS-03) lo da el cerrojo. Tambien dice que mesas estan ocupadas
 * ({@link OcupacionDeMesas}), asi el salon deriva su estado sin guardarlo (MS-02).
 */
public final class CuentasEnMemoria implements RepositorioCuentas, OcupacionDeMesas {

    private final Map<UUID, Cuenta> cuentas = new LinkedHashMap<>();
    private final Map<UUID, Comanda> comandas = new LinkedHashMap<>();
    private final Map<UUID, Integer> numeros = new HashMap<>();

    @Override
    public synchronized Optional<Cuenta> cuenta(UUID negocioId, UUID id) {
        return Optional.ofNullable(cuentas.get(id)).filter(c -> c.negocioId().equals(negocioId));
    }

    @Override
    public synchronized Optional<Cuenta> cuentaDeLinea(UUID negocioId, UUID lineaId) {
        return cuentas.values().stream().filter(c -> c.negocioId().equals(negocioId))
                .filter(c -> c.lineas().stream().anyMatch(l -> l.id().equals(lineaId))).findFirst();
    }

    @Override
    public synchronized Optional<Cuenta> cuentaDeVenta(UUID negocioId, UUID ventaId) {
        return cuentas.values().stream().filter(c -> c.negocioId().equals(negocioId) && ventaId.equals(c.ventaId()))
                .findFirst();
    }

    @Override
    public synchronized List<Cuenta> cuentas(UUID negocioId) {
        return new ArrayList<>(cuentas.values().stream().filter(c -> c.negocioId().equals(negocioId)).toList());
    }

    @Override
    public synchronized Optional<Cuenta> cuentaNoPagadaDeMesa(UUID negocioId, UUID mesaId) {
        return cuentas.values().stream().filter(c -> c.negocioId().equals(negocioId) && mesaId.equals(c.mesaId())
                && c.estado().noPagada()).findFirst();
    }

    @Override
    public synchronized boolean insertar(Cuenta cuenta) {
        if (cuenta.mesaId() != null && cuentaNoPagadaDeMesa(cuenta.negocioId(), cuenta.mesaId()).isPresent()) {
            return false;
        }
        cuentas.put(cuenta.id(), cuenta);
        return true;
    }

    @Override
    public synchronized boolean reemplazar(Cuenta nueva, long versionAnterior) {
        var actual = cuentas.get(nueva.id());
        if (actual == null || actual.version() != versionAnterior || !actual.negocioId().equals(nueva.negocioId())) {
            return false;
        }
        cuentas.put(nueva.id(), nueva);
        return true;
    }

    @Override
    public synchronized List<Cuenta> cuentasConDatosAnterioresA(Instant limite) {
        return new ArrayList<>(cuentas.values().stream().filter(c -> c.lineas().stream()
                .anyMatch(l -> l.creadaEn().isBefore(limite) && (l.nota() != null || l.dispositivoHash() != null))).toList());
    }

    @Override
    public synchronized int siguienteNumero(UUID negocioId) {
        return numeros.merge(negocioId, 1, Integer::sum);
    }

    @Override
    public synchronized Optional<Comanda> comanda(UUID negocioId, UUID id) {
        return Optional.ofNullable(comandas.get(id)).filter(c -> c.negocioId().equals(negocioId));
    }

    @Override
    public synchronized List<Comanda> comandas(UUID negocioId) {
        return new ArrayList<>(comandas.values().stream().filter(c -> c.negocioId().equals(negocioId)).toList());
    }

    @Override
    public synchronized void insertar(Comanda comanda) {
        comandas.put(comanda.id(), comanda);
    }

    @Override
    public synchronized boolean reemplazar(Comanda nueva, long versionAnterior) {
        var actual = comandas.get(nueva.id());
        if (actual == null || actual.version() != versionAnterior || !actual.negocioId().equals(nueva.negocioId())) {
            return false;
        }
        comandas.put(nueva.id(), nueva);
        return true;
    }

    @Override
    public synchronized Map<UUID, ResumenDeCuenta> cuentasNoPagadas(UUID negocioId) {
        var resultado = new HashMap<UUID, ResumenDeCuenta>();
        cuentas.values().stream().filter(c -> c.negocioId().equals(negocioId) && c.mesaId() != null && c.estado().noPagada())
                .forEach(c -> resultado.put(c.mesaId(), new ResumenDeCuenta(c.id(), c.numero(), c.estado().name(), c.total(),
                        c.abiertaEn(), c.origen() == OrigenDeCuenta.QR, c.presenciaConfirmada())));
        return resultado;
    }

    @Override
    public synchronized boolean hayCuentasSinPagar(UUID negocioId) {
        return cuentas.values().stream().anyMatch(c -> c.negocioId().equals(negocioId) && c.estado().noPagada());
    }
}
