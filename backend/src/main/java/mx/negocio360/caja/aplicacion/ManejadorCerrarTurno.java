package mx.negocio360.caja.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import mx.negocio360.caja.aplicacion.puertos.AutorizacionCaja;
import mx.negocio360.caja.aplicacion.puertos.LibroDeCaja;
import mx.negocio360.caja.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.caja.aplicacion.puertos.ReglasCaja;
import mx.negocio360.caja.aplicacion.puertos.RepositorioCaja;
import mx.negocio360.caja.aplicacion.puertos.ResumenVentas;
import mx.negocio360.caja.dominio.CajaException;
import mx.negocio360.caja.dominio.CalculoCaja;
import mx.negocio360.caja.dominio.CodigoError;
import mx.negocio360.caja.dominio.Corte;
import mx.negocio360.caja.dominio.EstadoTurno;
import mx.negocio360.caja.dominio.EstadoVentaAplicada;
import mx.negocio360.caja.dominio.MetodoPago;
import mx.negocio360.caja.dominio.Turno;
import mx.negocio360.caja.eventos.CorteConfirmado;
import mx.negocio360.caja.eventos.DiferenciaDetectada;
import mx.negocio360.compartido.comandos.ManejadorComando;
import mx.negocio360.compartido.comandos.ResultadoComando;
import mx.negocio360.compartido.comandos.ServicioPreparacion;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.dinero.DecimalPlano;
import mx.negocio360.compartido.dinero.Redondeo;
import mx.negocio360.compartido.identidad.UuidV7;

/**
 * CF-12 CerrarTurno, el corte de caja: CJ-06 a CJ-09 y CJ-12.
 *
 * <p>Preparar calcula esperado, contado y diferencia, revisa el umbral (CJ-08) y pone el turno EN_CORTE por la misma
 * vigencia que el token (D-81): desde ahi el canal ya no deja preparar ventas en ese turno. Confirmar exige que no
 * haya ventas preparadas y que Caja haya aplicado todas las ventas que reporta Ventas (CJ-07), recalcula y compara el
 * resumen, cierra el turno con bloqueo optimista y publica {@code caja.corte_confirmado} v1 (y
 * {@code caja.diferencia_detectada} v1 si se autorizo una diferencia sobre el umbral).
 */
public final class ManejadorCerrarTurno implements ManejadorComando<CerrarTurnoComando> {

    /** Nombre estable del comando en {@code preparacion.comando_tipo}. */
    public static final String TIPO = "cerrar_turno";

    private final RepositorioCaja cajas;
    private final LibroDeCaja libro;
    private final ResumenVentas ventas;
    private final ReglasCaja reglas;
    private final AutorizacionCaja autorizacion;
    private final PublicadorEventos eventos;
    private final Clock reloj;

    /** Cifras del corte tal como estan ahora. */
    private record Calculo(Turno turno, Map<MetodoPago, BigDecimal> esperado, Map<MetodoPago, BigDecimal> contado,
                           BigDecimal diferencia, BigDecimal umbral, boolean sobreUmbral, String resumen) {
    }

    /**
     * Arma el manejador.
     *
     * @param cajas        tablas caja y turno
     * @param libro        movimientos, ventas aplicadas y cortes
     * @param ventas       conteos de Ventas (CJ-07)
     * @param reglas       umbral de diferencia (CJ-08)
     * @param autorizacion permisos de encargado (CJ-08)
     * @param eventos      outbox
     * @param reloj        reloj de la plataforma
     */
    public ManejadorCerrarTurno(RepositorioCaja cajas, LibroDeCaja libro, ResumenVentas ventas, ReglasCaja reglas,
                                AutorizacionCaja autorizacion, PublicadorEventos eventos, Clock reloj) {
        this.cajas = Objects.requireNonNull(cajas, "cajas");
        this.libro = Objects.requireNonNull(libro, "libro");
        this.ventas = Objects.requireNonNull(ventas, "ventas");
        this.reglas = Objects.requireNonNull(reglas, "reglas");
        this.autorizacion = Objects.requireNonNull(autorizacion, "autorizacion");
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public String tipo() {
        return TIPO;
    }

    @Override
    public Class<CerrarTurnoComando> claseComando() {
        return CerrarTurnoComando.class;
    }

    /**
     * Calcula el corte y pone el turno EN_CORTE (D-81).
     *
     * @throws CajaException TURNO_NO_ABIERTO, CONTEO_INVALIDO, CORTE_REQUIERE_AUTORIZACION o CONCURRENCIA
     */
    @Override
    public String preparar(ContextoNegocio contexto, CerrarTurnoComando comando) {
        var calculo = calcular(contexto, comando);
        exigirAutorizacion(contexto, comando, calculo);
        var vence = reloj.instant().plus(ServicioPreparacion.VIGENCIA_TOKEN);
        var turno = calculo.turno();
        if (!cajas.actualizarTurno(turno.enCorte(vence), turno.version())) {
            throw new CajaException(CodigoError.CONCURRENCIA, turno.id());
        }
        return calculo.resumen();
    }

    /**
     * Cierra el turno.
     *
     * @throws CajaException TURNO_NO_ABIERTO, CORTE_VENTAS_PENDIENTES, CORTE_EVENTOS_PENDIENTES, COMANDO_CAMBIO,
     *                       CORTE_REQUIERE_AUTORIZACION o CONCURRENCIA
     */
    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, CerrarTurnoComando comando, String resumenPreparado) {
        var ahora = reloj.instant();
        var enCorte = Validaciones.turnoNoCerrado(cajas, contexto.negocioId(), comando.turnoId(), ahora);
        if (enCorte.estadoEn(ahora) != EstadoTurno.EN_CORTE) {
            throw new CajaException(CodigoError.TURNO_NO_ABIERTO, enCorte.id());
        }
        exigirVentasAlDia(contexto, enCorte);
        var calculo = calcular(contexto, comando);
        if (!calculo.resumen().equals(resumenPreparado)) {
            throw new CajaException(CodigoError.COMANDO_CAMBIO, enCorte.id());
        }
        exigirAutorizacion(contexto, comando, calculo);
        var turno = calculo.turno();
        if (!cajas.actualizarTurno(turno.cerrado(ahora), turno.version())) {
            throw new CajaException(CodigoError.CONCURRENCIA, turno.id());
        }
        var corte = new Corte(UuidV7.generar(ahora), contexto.negocioId(), turno.id(), turno.cajaId(),
                calculo.esperado(), calculo.contado(), calculo.diferencia(),
                calculo.sobreUmbral() ? contexto.actor() : null, turno.abiertoEn(), ahora);
        libro.guardarCorte(corte);
        eventos.publicar(FabricaSobres.de(contexto.negocioId(), ahora, turno.id(), CorteConfirmado.TIPO,
                CorteConfirmado.VERSION, new CorteConfirmado(corte).datos()));
        if (calculo.sobreUmbral()) {
            eventos.publicar(FabricaSobres.de(contexto.negocioId(), ahora, turno.id(), DiferenciaDetectada.TIPO,
                    DiferenciaDetectada.VERSION, new DiferenciaDetectada(turno.id(), turno.cajaId(),
                            calculo.diferencia(), calculo.umbral()).datos()));
        }
        return ResultadoComando.ok(PlantillasCaja.CORTE_CONFIRMADO, corte.id(), Map.of(
                "corte_id", corte.id().toString(),
                "diferencia_efectivo", DecimalPlano.escribir(calculo.diferencia())));
    }

    /** CJ-07: sin ventas preparadas y con todos los eventos de venta del turno ya aplicados. */
    private void exigirVentasAlDia(ContextoNegocio contexto, Turno turno) {
        var conteo = ventas.deTurno(contexto, turno.id());
        if (conteo.preparacionesVigentes() > 0) {
            throw new CajaException(CodigoError.CORTE_VENTAS_PENDIENTES, turno.id());
        }
        var aplicadas = libro.contarVentas(contexto.negocioId(), turno.id(), EstadoVentaAplicada.APLICADA);
        var revertidas = libro.contarVentas(contexto.negocioId(), turno.id(), EstadoVentaAplicada.REVERTIDA);
        if (aplicadas != conteo.registradas() || revertidas != conteo.anuladas()) {
            throw new CajaException(CodigoError.CORTE_EVENTOS_PENDIENTES, turno.id());
        }
    }

    private Calculo calcular(ContextoNegocio contexto, CerrarTurnoComando comando) {
        var turno = Validaciones.turnoNoCerrado(cajas, contexto.negocioId(), comando.turnoId(), reloj.instant());
        var contado = contadoValido(comando.contadoPorMetodo());
        var esperado = CalculoCaja.esperado(libro.movimientos(contexto.negocioId(), turno.id()));
        var diferencia = Redondeo.dinero(contado.get(MetodoPago.EFECTIVO).subtract(esperado.get(MetodoPago.EFECTIVO)));
        var umbral = Redondeo.dinero(reglas.umbralDiferencia(contexto.negocioId()));
        var sobreUmbral = diferencia.abs().compareTo(umbral) > 0;
        var resumen = PlantillasCaja.resumenCorte(esperado, contado, diferencia);
        return new Calculo(turno, esperado, contado, diferencia, umbral, sobreUmbral, resumen);
    }

    /**
     * CJ-08: una diferencia en efectivo mayor al umbral solo se acepta si quien confirma es encargado y la autoriza.
     * Al confirmar se revisa despues de comparar el resumen, para que un esperado que cambio responda COMANDO_CAMBIO.
     */
    private void exigirAutorizacion(ContextoNegocio contexto, CerrarTurnoComando comando, Calculo calculo) {
        if (calculo.sobreUmbral() && !(comando.autorizar() && autorizacion.esEncargado(contexto))) {
            throw new CajaException(CodigoError.CORTE_REQUIERE_AUTORIZACION, calculo.turno().id());
        }
    }

    private static Map<MetodoPago, BigDecimal> contadoValido(Map<MetodoPago, BigDecimal> capturado) {
        if (capturado == null) {
            throw new CajaException(CodigoError.CONTEO_INVALIDO);
        }
        var contado = new EnumMap<MetodoPago, BigDecimal>(MetodoPago.class);
        for (var metodo : MetodoPago.values()) {
            var monto = capturado.getOrDefault(metodo, BigDecimal.ZERO);
            if (monto == null || monto.signum() < 0 || !Validaciones.escalaDeDinero(monto)) {
                throw new CajaException(CodigoError.CONTEO_INVALIDO);
            }
            contado.put(metodo, Redondeo.dinero(monto));
        }
        return Collections.unmodifiableMap(contado);
    }
}
