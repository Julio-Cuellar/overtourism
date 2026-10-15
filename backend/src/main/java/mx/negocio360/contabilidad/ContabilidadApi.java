package mx.negocio360.contabilidad;

import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.contabilidad.aplicacion.Comando;
import mx.negocio360.contabilidad.aplicacion.PreparacionCreada;
import mx.negocio360.contabilidad.aplicacion.ResultadoComando;
import mx.negocio360.contabilidad.dominio.Balanza;
import mx.negocio360.contabilidad.dominio.Cuenta;
import mx.negocio360.contabilidad.dominio.EstadoPendiente;
import mx.negocio360.contabilidad.dominio.Pendiente;
import mx.negocio360.contabilidad.dominio.Periodo;
import mx.negocio360.contabilidad.dominio.Poliza;
import mx.negocio360.contabilidad.dominio.ReglaContable;
import mx.negocio360.contabilidad.dominio.ResultadoEvento;

/**
 * Funciones publicas del modulo (seccion 7 de {@code docs/diseno/contabilidad.md}). Los resolvedores GraphQL, los
 * controladores REST y los listeners llaman a esta interfaz y nunca a los repositorios. El negocio y el actor salen
 * siempre del contexto, nunca de un argumento (SG-02).
 */
public interface ContabilidadApi {

    /**
     * Valida un comando sin escribir y devuelve el token de un solo uso con el resumen para confirmar.
     *
     * @param contexto negocio y actor
     * @param comando  comando a preparar
     * @return token, resumen y vencimiento
     * @throws mx.negocio360.contabilidad.dominio.ContabilidadException con el codigo de la regla incumplida
     */
    PreparacionCreada preparar(ContextoNegocio contexto, Comando comando);

    /**
     * Ejecuta el comando preparado.
     *
     * @param contexto negocio y actor; los mismos que al preparar
     * @param token    token devuelto al preparar
     * @return el resultado
     * @throws mx.negocio360.contabilidad.dominio.ContabilidadException {@code TOKEN_*}, {@code COMANDO_CAMBIO} o el
     *                                                                  codigo de la regla incumplida
     */
    ResultadoComando confirmar(ContextoNegocio contexto, String token);

    /**
     * Catalogo de cuentas del negocio.
     *
     * @param contexto negocio
     * @return las cuentas ordenadas por codigo; vacio si la contabilidad no esta activa
     */
    List<Cuenta> catalogo(ContextoNegocio contexto);

    /**
     * Reglas de contabilizacion del negocio.
     *
     * @param contexto negocio
     * @return las reglas; vacio si la contabilidad no esta activa
     */
    List<ReglaContable> reglas(ContextoNegocio contexto);

    /**
     * Una poliza del negocio con sus renglones.
     *
     * @param contexto negocio
     * @param polizaId poliza
     * @return la poliza, o vacio si no existe o es de otro negocio (responden igual)
     */
    Optional<Poliza> poliza(ContextoNegocio contexto, UUID polizaId);

    /**
     * Balanza de comprobacion de un mes (G5). Lee los saldos, no los movimientos.
     *
     * @param contexto negocio
     * @param mes      anio y mes
     * @return la balanza con todas las cuentas afectables y los totales del mes
     */
    Balanza balanza(ContextoNegocio contexto, YearMonth mes);

    /**
     * Escritura del sistema para {@code venta.registrada} (J2). La llama el listener; el negocio sale del sobre.
     *
     * @param evento sobre del evento
     * @return lo que se hizo con el evento
     */
    ResultadoEvento contabilizarVenta(SobreEvento evento);

    /**
     * Bandeja de pendientes del negocio (G9).
     *
     * @param contexto negocio
     * @param estado   estado a listar
     * @return los pendientes, del mas antiguo al mas nuevo
     */
    List<Pendiente> pendientes(ContextoNegocio contexto, EstadoPendiente estado);

    /**
     * Escritura del sistema que abre el periodo de un mes (J3, T-04). La llama la tarea del dia 1, una vez por
     * negocio, fijando el negocio de cada uno (K-12).
     *
     * @param negocioId negocio
     * @param mes       anio y mes
     * @return {@code ABIERTO}, {@code YA_ABIERTO} o {@code CONTABILIDAD_NO_ACTIVA}
     */
    String abrirPeriodo(UUID negocioId, YearMonth mes);

    /**
     * Periodos del negocio en un anio (G7).
     *
     * @param contexto negocio
     * @param anio     anio
     * @return los periodos, ordenados por mes
     */
    List<Periodo> periodos(ContextoNegocio contexto, int anio);
}
