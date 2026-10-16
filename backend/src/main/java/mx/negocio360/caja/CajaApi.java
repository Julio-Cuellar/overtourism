package mx.negocio360.caja;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.caja.dominio.Caja;
import mx.negocio360.caja.dominio.Corte;
import mx.negocio360.caja.dominio.MovimientoCaja;
import mx.negocio360.caja.dominio.Turno;
import mx.negocio360.compartido.comandos.Comando;
import mx.negocio360.compartido.comandos.PreparacionCreada;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.paginacion.Pagina;
import mx.negocio360.compartido.seguridad.Autorizacion;

/**
 * Funciones publicas de Caja ({@code docs/diseno/caja.md}, seccion 5). El negocio y el actor salen del contexto. Cada
 * funcion de usuario recibe tambien la {@link Autorizacion} de la sesion y exige su permiso: es la segunda barrera
 * despues de la puerta de GraphQL (D-126). Los fallos salen como {@link mx.negocio360.caja.dominio.CajaException} con
 * su codigo, o como {@link mx.negocio360.compartido.seguridad.PermisoDenegadoException} si falta un permiso.
 */
public interface CajaApi {

    /**
     * Prepara un comando: CF-14 CrearCaja, CF-10 AbrirTurno, CF-11 RegistrarMovimientoCaja, CF-12 CerrarTurno o
     * CF-13 DescartarCorte. Solo CerrarTurno cambia algo al preparar: pone el turno EN_CORTE (D-140). Cada comando
     * exige su permiso; un retiro exige ademas {@code caja.retiro.registrar} y un corte con {@code autorizar}
     * exige ademas {@code caja.corte.autorizar}.
     *
     * @param contexto     negocio y actor
     * @param autorizacion permisos de la persona
     * @param comando      comando de Caja
     * @return token, resumen y vencimiento
     */
    PreparacionCreada preparar(ContextoNegocio contexto, Autorizacion autorizacion, Comando comando);

    /**
     * Confirma el comando preparado. Vuelve a exigir los permisos <b>antes de gastar el token</b> (D-126).
     *
     * @param contexto     el mismo negocio y actor que prepararon
     * @param autorizacion permisos de la persona ahora
     * @param token        token en claro
     * @return resultado con el id de la caja, del turno o del movimiento
     */
    ResultadoComando confirmar(ContextoNegocio contexto, Autorizacion autorizacion, String token);

    /**
     * CF-01: turno abierto o en corte de una caja; lo usa el panel antes de cada venta (VT-01). Devuelve el estado
     * efectivo: si el corte preparado vencio, el turno sale ABIERTO (D-140). Exige {@code caja.turno.ver}.
     *
     * @param contexto     negocio
     * @param autorizacion permisos de la persona
     * @param cajaId       caja
     * @return el turno no cerrado con su estado efectivo, o vacio
     */
    Optional<Turno> turnoActivo(ContextoNegocio contexto, Autorizacion autorizacion, UUID cajaId);

    /**
     * CF-02: esperado por metodo y movimientos de un turno. Exige {@code caja.turno.ver}.
     *
     * @param contexto     negocio
     * @param autorizacion permisos de la persona
     * @param turnoId      turno
     * @return saldo del turno
     * @throws mx.negocio360.caja.dominio.CajaException TURNO_NO_ENCONTRADO
     */
    SaldoTurno saldoTurno(ContextoNegocio contexto, Autorizacion autorizacion, UUID turnoId);

    /**
     * CF-02: movimientos de un turno con cursor, en orden de registro (D-131). Exige {@code caja.turno.ver}.
     *
     * @param contexto     negocio
     * @param autorizacion permisos de la persona
     * @param turnoId      turno
     * @param primeros     tamano de pagina, de 1 a 100
     * @param despues      cursor de la pagina anterior, o nulo
     * @return la pagina pedida; vacia si el turno es de otro negocio
     * @throws mx.negocio360.caja.dominio.CajaException PAGINACION_INVALIDA
     */
    Pagina<MovimientoCaja> movimientos(ContextoNegocio contexto, Autorizacion autorizacion, UUID turnoId,
                                       int primeros, String despues);

    /**
     * CF-03: cortes del negocio con sus diferencias, del mas reciente al mas antiguo, con cursor (D-131). Exige
     * {@code caja.corte.ver}.
     *
     * @param contexto     negocio
     * @param autorizacion permisos de la persona
     * @param filtro       filtros opcionales; nulo es sin filtros
     * @param primeros     tamano de pagina, de 1 a 100
     * @param despues      cursor de la pagina anterior, o nulo
     * @return la pagina pedida
     * @throws mx.negocio360.caja.dominio.CajaException PAGINACION_INVALIDA o FILTRO_INVALIDO
     */
    Pagina<Corte> cortes(ContextoNegocio contexto, Autorizacion autorizacion, FiltroCortes filtro, int primeros,
                         String despues);

    /**
     * CF-04: cajas del negocio, hasta 50. Exige {@code caja.turno.ver}.
     *
     * @param contexto     negocio
     * @param autorizacion permisos de la persona
     * @return cajas en orden de alta
     */
    List<Caja> cajas(ContextoNegocio contexto, Autorizacion autorizacion);
}
