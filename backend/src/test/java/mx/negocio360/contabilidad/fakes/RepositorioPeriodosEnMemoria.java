package mx.negocio360.contabilidad.fakes;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPeriodos;
import mx.negocio360.contabilidad.dominio.Periodo;

/** Doble en memoria de la tabla periodo. */
public final class RepositorioPeriodosEnMemoria implements RepositorioPeriodos {

    private final List<Periodo> filas = new ArrayList<>();

    @Override
    public synchronized void guardar(Periodo periodo) {
        filas.removeIf(p -> p.id().equals(periodo.id()));
        filas.add(periodo);
    }

    @Override
    public synchronized Optional<Periodo> delMes(UUID negocioId, YearMonth mes) {
        return filas.stream().filter(p -> p.negocioId().equals(negocioId) && p.mes().equals(mes)).findFirst();
    }

    @Override
    public synchronized List<Periodo> delNegocio(UUID negocioId) {
        return filas.stream().filter(p -> p.negocioId().equals(negocioId))
                .sorted(Comparator.comparing(Periodo::mes)).toList();
    }
}
