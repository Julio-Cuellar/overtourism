package mx.negocio360.caja.aplicacion.puertos;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.caja.dominio.Caja;
import mx.negocio360.caja.dominio.Turno;

/** Puerto de las tablas {@code caja} y {@code turno}. */
public interface RepositorioCaja {

    /**
     * Inserta una caja nueva.
     *
     * @param caja caja nueva
     */
    void guardarCaja(Caja caja);

    /**
     * Busca una caja del negocio.
     *
     * @param negocioId negocio del contexto
     * @param cajaId    caja
     * @return la caja, si existe en ese negocio
     */
    Optional<Caja> caja(UUID negocioId, UUID cajaId);

    /**
     * Lista las cajas del negocio.
     *
     * @param negocioId negocio del contexto
     * @return cajas del negocio en orden de alta
     */
    List<Caja> cajas(UUID negocioId);

    /**
     * Busca un turno del negocio.
     *
     * @param negocioId negocio del contexto
     * @param turnoId   turno
     * @return el turno, si existe en ese negocio
     */
    Optional<Turno> turno(UUID negocioId, UUID turnoId);

    /**
     * Turno ABIERTO o EN_CORTE de una caja (CJ-01). En la base lo respalda un indice unico parcial.
     *
     * @param negocioId negocio del contexto
     * @param cajaId    caja
     * @return el turno no cerrado, o vacio
     */
    Optional<Turno> turnoNoCerradoDeCaja(UUID negocioId, UUID cajaId);

    /**
     * Inserta un turno nuevo.
     *
     * @param turno turno nuevo
     */
    void guardarTurno(Turno turno);

    /**
     * Reemplaza el turno solo si su version sigue siendo la esperada.
     *
     * @param turno           turno con la version nueva
     * @param versionEsperada version que se leyo
     * @return falso si otra peticion lo cambio antes
     */
    boolean actualizarTurno(Turno turno, int versionEsperada);
}
