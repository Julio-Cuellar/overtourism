package mx.negocio360.contabilidad.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.EstadoPendiente;
import mx.negocio360.contabilidad.dominio.Pendiente;

/** Puerto de la tabla {@code contabilidad.pendiente_contable}. */
public interface RepositorioPendientes {

    /**
     * Inserta o reemplaza un pendiente.
     *
     * @param pendiente pendiente a guardar
     */
    void guardar(Pendiente pendiente);

    /**
     * Pendiente del negocio por id.
     *
     * @param negocioId   negocio
     * @param pendienteId pendiente
     * @return el pendiente, o vacio si no existe o es de otro negocio
     */
    Optional<Pendiente> porId(UUID negocioId, UUID pendienteId);

    /**
     * Pendientes del negocio en un estado, del mas antiguo al mas nuevo.
     *
     * @param negocioId negocio
     * @param estado    estado
     * @return los pendientes
     */
    List<Pendiente> delNegocio(UUID negocioId, EstadoPendiente estado);
}
