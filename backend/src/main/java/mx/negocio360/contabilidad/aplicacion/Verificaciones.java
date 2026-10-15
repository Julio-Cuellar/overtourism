package mx.negocio360.contabilidad.aplicacion;

import java.util.UUID;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioConfiguracion;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.Configuracion;
import mx.negocio360.contabilidad.dominio.ContabilidadException;
import mx.negocio360.contabilidad.dominio.Cuenta;

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

    /**
     * Devuelve la cuenta del negocio si la persona tenia a la vista su version actual.
     *
     * @throws ContabilidadException {@code CUENTA_NO_ENCONTRADA} o {@code CONCURRENCIA}
     */
    static Cuenta cuentaVigente(RepositorioCuentas cuentas, UUID negocioId, UUID cuentaId, long version) {
        var cuenta = cuentaId == null ? null : cuentas.porId(negocioId, cuentaId).orElse(null);
        if (cuenta == null) {
            throw new ContabilidadException(CodigoError.CUENTA_NO_ENCONTRADA);
        }
        if (cuenta.version() != version) {
            throw new ContabilidadException(CodigoError.CONCURRENCIA, cuenta.id());
        }
        return cuenta;
    }
}
