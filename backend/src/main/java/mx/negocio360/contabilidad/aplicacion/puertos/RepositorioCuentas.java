package mx.negocio360.contabilidad.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.Cuenta;

/** Puerto de la tabla {@code contabilidad.cuenta}. Toda lectura va filtrada por negocio (seguridad por fila). */
public interface RepositorioCuentas {

    /**
     * Inserta o reemplaza una cuenta.
     *
     * @param cuenta cuenta a guardar
     */
    void guardar(Cuenta cuenta);

    /**
     * Busca una cuenta del negocio por id.
     *
     * @param negocioId negocio
     * @param cuentaId  cuenta
     * @return la cuenta, o vacio si no existe o es de otro negocio
     */
    Optional<Cuenta> porId(UUID negocioId, UUID cuentaId);

    /**
     * Busca una cuenta del negocio por codigo.
     *
     * @param negocioId negocio
     * @param codigo    codigo, por ejemplo {@code 101.01}
     * @return la cuenta, o vacio
     */
    Optional<Cuenta> porCodigo(UUID negocioId, String codigo);

    /**
     * Todas las cuentas del negocio.
     *
     * @param negocioId negocio
     * @return las cuentas, en cualquier orden
     */
    List<Cuenta> delNegocio(UUID negocioId);
}
