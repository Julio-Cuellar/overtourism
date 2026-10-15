package mx.negocio360.contabilidad.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.Saldo;

/** Puerto de la tabla {@code contabilidad.saldo_cuenta}. Solo la escribe el libro diario (A-08). */
public interface RepositorioSaldos {

    /**
     * Inserta o reemplaza el saldo de una cuenta en un periodo.
     *
     * @param saldo saldo a guardar
     */
    void guardar(Saldo saldo);

    /**
     * Saldo de una cuenta en un periodo.
     *
     * @param negocioId negocio
     * @param cuentaId  cuenta
     * @param periodoId periodo
     * @return el saldo, o vacio si la cuenta no se movio en ese periodo
     */
    Optional<Saldo> de(UUID negocioId, UUID cuentaId, UUID periodoId);

    /**
     * Todos los saldos de una cuenta, en todos los periodos.
     *
     * @param negocioId negocio
     * @param cuentaId  cuenta
     * @return los saldos, en cualquier orden
     */
    List<Saldo> deCuenta(UUID negocioId, UUID cuentaId);
}
