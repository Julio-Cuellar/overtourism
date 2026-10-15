package mx.negocio360.contabilidad.aplicacion;

import java.util.UUID;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioConfiguracion;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.Configuracion;
import mx.negocio360.contabilidad.dominio.ContabilidadException;

/** Verificaciones que comparten varios casos de uso. */
final class Verificaciones {

    private Verificaciones() {
    }

    /**
     * Devuelve la configuracion del negocio o falla si la contabilidad no esta activa.
     *
     * @throws ContabilidadException {@code CONTABILIDAD_NO_ACTIVA}
     */
    static Configuracion activa(RepositorioConfiguracion configuracion, UUID negocioId) {
        return configuracion.delNegocio(negocioId)
                .orElseThrow(() -> new ContabilidadException(CodigoError.CONTABILIDAD_NO_ACTIVA));
    }
}
