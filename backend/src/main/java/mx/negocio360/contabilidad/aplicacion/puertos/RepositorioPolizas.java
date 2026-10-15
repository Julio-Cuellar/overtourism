package mx.negocio360.contabilidad.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.contabilidad.dominio.Poliza;
import mx.negocio360.contabilidad.dominio.TipoPoliza;

/**
 * Puerto de las tablas {@code poliza} y {@code movimiento}. Solo inserta: no hay operacion para cambiar ni borrar
 * una poliza (A-03, F-04).
 */
public interface RepositorioPolizas {

    /**
     * Inserta una poliza con sus renglones.
     *
     * @param poliza poliza nueva
     */
    void insertar(Poliza poliza);

    /**
     * Reserva el siguiente folio de la serie (A-05). Debe ser atomico: dos registros simultaneos nunca reciben el
     * mismo folio. En Postgres se resuelve con un contador por serie bloqueado en la misma transaccion.
     *
     * @param negocioId negocio
     * @param periodoId periodo
     * @param tipo      tipo de poliza
     * @return el folio reservado, desde 1
     */
    int siguienteFolio(UUID negocioId, UUID periodoId, TipoPoliza tipo);

    /**
     * Busca una poliza del negocio.
     *
     * @param negocioId negocio
     * @param polizaId  poliza
     * @return la poliza, o vacio si no existe o es de otro negocio
     */
    Optional<Poliza> porId(UUID negocioId, UUID polizaId);

    /**
     * La reversa de una poliza, si existe. En Postgres, {@code revierte_poliza_id} es unico (A-06).
     *
     * @param negocioId negocio
     * @param polizaId  poliza original
     * @return la reversa, o vacio
     */
    Optional<Poliza> reversaDe(UUID negocioId, UUID polizaId);

    /**
     * Dice si alguna poliza del negocio mueve la cuenta (C-03).
     *
     * @param negocioId negocio
     * @param cuentaId  cuenta
     * @return verdadero si tiene al menos un movimiento
     */
    boolean cuentaTieneMovimientos(UUID negocioId, UUID cuentaId);

    /**
     * Polizas del negocio en un periodo, en orden de registro.
     *
     * @param negocioId negocio
     * @param periodoId periodo
     * @return las polizas
     */
    List<Poliza> delPeriodo(UUID negocioId, UUID periodoId);
}
