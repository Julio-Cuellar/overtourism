package mx.negocio360.contabilidad.aplicacion.puertos;

import java.util.List;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.ReglaContable;

/** Puerto de la tabla {@code contabilidad.regla_contable}. */
public interface RepositorioReglas {

    /**
     * Inserta o reemplaza una regla.
     *
     * @param regla regla a guardar
     */
    void guardar(ReglaContable regla);

    /**
     * Todas las reglas del negocio, vigentes o no.
     *
     * @param negocioId negocio
     * @return las reglas
     */
    List<ReglaContable> delNegocio(UUID negocioId);
}
