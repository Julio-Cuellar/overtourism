package mx.negocio360.contabilidad.fakes;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioReglas;
import mx.negocio360.contabilidad.dominio.ReglaContable;

/** Doble en memoria de la tabla regla_contable. */
public final class RepositorioReglasEnMemoria implements RepositorioReglas {

    private final List<ReglaContable> filas = new ArrayList<>();

    @Override
    public void guardar(ReglaContable regla) {
        filas.removeIf(r -> r.id().equals(regla.id()));
        filas.add(regla);
    }

    @Override
    public List<ReglaContable> delNegocio(UUID negocioId) {
        return filas.stream().filter(r -> r.negocioId().equals(negocioId)).toList();
    }
}
