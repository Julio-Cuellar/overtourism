package mx.negocio360.caja;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.caja.dominio.Caja;
import mx.negocio360.caja.dominio.Corte;
import mx.negocio360.caja.dominio.Turno;
import mx.negocio360.compartido.comandos.Comando;
import mx.negocio360.compartido.comandos.PreparacionCreada;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;

/**
 * Funciones publicas de Caja ({@code docs/diseno/caja.md}, seccion 5). El negocio y el actor salen del contexto. Los
 * fallos salen como {@link mx.negocio360.caja.dominio.CajaException} con su codigo.
 */
public interface CajaApi {

    /**
     * Prepara un comando: CF-14 CrearCaja, CF-10 AbrirTurno, CF-11 RegistrarMovimientoCaja, CF-12 CerrarTurno o
     * CF-13 DescartarCorte. Solo CerrarTurno cambia algo al preparar: pone el turno EN_CORTE (D-81).
     *
     * @param contexto negocio y actor
     * @param comando  comando de Caja
     * @return token, resumen y vencimiento
     */
    PreparacionCreada preparar(ContextoNegocio contexto, Comando comando);

    /**
     * Confirma el comando preparado.
     *
     * @param contexto el mismo negocio y actor que prepararon
     * @param token    token en claro
     * @return resultado con el id de la caja, del turno o del movimiento
     */
    ResultadoComando confirmar(ContextoNegocio contexto, String token);

    /**
     * CF-01: turno abierto o en corte de una caja; lo usa el canal antes de cada venta (VT-01). Devuelve el estado
     * efectivo: si el corte preparado vencio, el turno sale ABIERTO (D-81).
     *
     * @param contexto negocio
     * @param cajaId   caja
     * @return el turno no cerrado con su estado efectivo, o vacio
     */
    Optional<Turno> turnoActivo(ContextoNegocio contexto, UUID cajaId);

    /**
     * CF-02: esperado por metodo y movimientos de un turno.
     *
     * @param contexto negocio
     * @param turnoId  turno
     * @return saldo del turno
     * @throws mx.negocio360.caja.dominio.CajaException TURNO_NO_ENCONTRADO
     */
    SaldoTurno saldoTurno(ContextoNegocio contexto, UUID turnoId);

    /**
     * CF-03: cortes del negocio con sus diferencias.
     *
     * @param contexto negocio
     * @return cortes en orden de confirmacion
     */
    List<Corte> cortes(ContextoNegocio contexto);

    /**
     * CF-04: cajas del negocio.
     *
     * @param contexto negocio
     * @return cajas en orden de alta
     */
    List<Caja> cajas(ContextoNegocio contexto);
}
