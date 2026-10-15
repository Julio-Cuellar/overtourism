package mx.negocio360.contabilidad.fakes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPolizas;
import mx.negocio360.contabilidad.dominio.Poliza;
import mx.negocio360.contabilidad.dominio.TipoPoliza;

/** Doble en memoria de poliza y movimiento. Solo inserta, como la tabla real (A-03). */
public final class RepositorioPolizasEnMemoria implements RepositorioPolizas {

    private final List<Poliza> filas = new ArrayList<>();
    private final Map<String, Integer> folios = new HashMap<>();

    @Override
    public synchronized int siguienteFolio(UUID negocioId, UUID periodoId, TipoPoliza tipo) {
        return folios.merge(negocioId + "/" + periodoId + "/" + tipo, 1, Integer::sum);
    }

    /** Cuando es verdadero, insertar falla como lo haria una base caida. */
    public volatile boolean fallarAlInsertar;

    @Override
    public synchronized void insertar(Poliza poliza) {
        if (fallarAlInsertar) {
            throw new IllegalStateException("fallo simulado al insertar");
        }
        if (filas.stream().anyMatch(p -> p.id().equals(poliza.id()))) {
            throw new IllegalStateException("La poliza ya existe: las polizas solo se insertan");
        }
        filas.add(poliza);
    }

    @Override
    public synchronized Optional<Poliza> porId(UUID negocioId, UUID polizaId) {
        return filas.stream().filter(p -> p.id().equals(polizaId) && p.negocioId().equals(negocioId)).findFirst();
    }

    @Override
    public synchronized Optional<Poliza> reversaDe(UUID negocioId, UUID polizaId) {
        return filas.stream().filter(p -> p.negocioId().equals(negocioId) && polizaId.equals(p.reviertePolizaId()))
                .findFirst();
    }

    @Override
    public synchronized boolean cuentaTieneMovimientos(UUID negocioId, UUID cuentaId) {
        return filas.stream().filter(p -> p.negocioId().equals(negocioId))
                .anyMatch(p -> p.renglones().stream().anyMatch(r -> r.cuentaId().equals(cuentaId)));
    }

    @Override
    public synchronized List<Poliza> delPeriodo(UUID negocioId, UUID periodoId) {
        return filas.stream().filter(p -> p.negocioId().equals(negocioId) && p.periodoId().equals(periodoId))
                .toList();
    }

    /** Todas las polizas de un negocio, en orden de registro. */
    public synchronized List<Poliza> delNegocio(UUID negocioId) {
        return filas.stream().filter(p -> p.negocioId().equals(negocioId)).toList();
    }
}
