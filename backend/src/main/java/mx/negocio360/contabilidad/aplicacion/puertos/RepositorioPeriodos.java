package mx.negocio360.contabilidad.aplicacion.puertos;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.Periodo;

/** Puerto de la tabla {@code contabilidad.periodo}. */
public interface RepositorioPeriodos {

    /**
     * Inserta o reemplaza un periodo.
     *
     * @param periodo periodo a guardar
     */
    void guardar(Periodo periodo);

    /**
     * Periodo del negocio para un mes.
     *
     * @param negocioId negocio
     * @param mes       anio y mes
     * @return el periodo, o vacio si no se ha abierto
     */
    Optional<Periodo> delMes(UUID negocioId, YearMonth mes);

    /**
     * Todos los periodos del negocio.
     *
     * @param negocioId negocio
     * @return los periodos, ordenados por mes
     */
    List<Periodo> delNegocio(UUID negocioId);
}
