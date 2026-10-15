package mx.negocio360.contabilidad.aplicacion.puertos;

import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.Configuracion;

/** Puerto de la tabla {@code contabilidad.configuracion}: una fila por negocio. */
public interface RepositorioConfiguracion {

    /**
     * Configuracion del negocio. Si no existe, la contabilidad no esta activa.
     *
     * @param negocioId negocio
     * @return la configuracion, o vacio
     */
    Optional<Configuracion> delNegocio(UUID negocioId);

    /**
     * Inserta o reemplaza la configuracion del negocio.
     *
     * @param configuracion fila a guardar
     */
    void guardar(Configuracion configuracion);
}
