package mx.negocio360.contabilidad.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.CorteContable;

/** Puerto de la tabla {@code contabilidad.corte_contable}. La llave natural es (negocio, corte). */
public interface RepositorioCortesContables {

    /**
     * Inserta o reemplaza un corte.
     *
     * @param corte corte a guardar
     */
    void guardar(CorteContable corte);

    /**
     * Corte por su id en Caja.
     *
     * @param negocioId negocio
     * @param corteId   corte
     * @return el corte, o vacio si no se ha recibido
     */
    Optional<CorteContable> porCorte(UUID negocioId, UUID corteId);

    /**
     * Corte de una sesion de caja, si ya cerro.
     *
     * @param negocioId    negocio
     * @param cajaSesionId sesion
     * @return el corte, o vacio si la sesion no ha cerrado
     */
    Optional<CorteContable> deSesion(UUID negocioId, UUID cajaSesionId);

    /**
     * Cortes del negocio, del mas antiguo al mas nuevo.
     *
     * @param negocioId negocio
     * @return los cortes
     */
    List<CorteContable> delNegocio(UUID negocioId);
}
