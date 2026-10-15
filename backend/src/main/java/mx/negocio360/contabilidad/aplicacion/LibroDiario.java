package mx.negocio360.contabilidad.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPeriodos;
import mx.negocio360.contabilidad.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPolizas;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioSaldos;
import mx.negocio360.contabilidad.dominio.Asiento;
import mx.negocio360.contabilidad.dominio.Balanza;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.ContabilidadException;
import mx.negocio360.contabilidad.dominio.Montos;
import mx.negocio360.contabilidad.dominio.OrigenPoliza;
import mx.negocio360.contabilidad.dominio.Periodo;
import mx.negocio360.contabilidad.dominio.Poliza;
import mx.negocio360.contabilidad.dominio.TipoCuenta;

/**
 * Unico punto por donde se registran polizas, sean manuales, reversas o automaticas. Revisa las cuentas (A-04) y
 * escribe la poliza en su periodo con su folio (A-05) y actualiza los saldos (A-08). En Postgres todo ocurre en una
 * sola transaccion, que incluye el evento {@code poliza.registrada} en el outbox (E-08); con los dobles en memoria, la
 * actualizacion de saldos se serializa.
 */
public final class LibroDiario {

    /**
     * Datos de quien y por que se registra la poliza.
     *
     * @param origen           automatica, manual o reversa
     * @param eventoId         evento que la origino, o nulo
     * @param referenciaTipo   VENTA, CANJE, CORTE o PENDIENTE, o nulo
     * @param referenciaId     id de la referencia, o nulo
     * @param reviertePolizaId poliza que revierte, o nulo
     * @param creadoPor        actor o {@code sistema}
     */
    public record Registro(OrigenPoliza origen, UUID eventoId, String referenciaTipo, UUID referenciaId,
                           UUID reviertePolizaId, String creadoPor) {

        /**
         * Registro de una poliza capturada por una persona.
         *
         * @param actor quien la captura
         * @return el registro
         */
        public static Registro manual(String actor) {
            return new Registro(OrigenPoliza.MANUAL, null, null, null, null, actor);
        }
    }

    private static final int MAX_CONCEPTO = 200;

    /**
     * Cifras de un mes para {@code periodo.cerrado}.
     *
     * @param ventasNetas ventas menos descuentos
     * @param resultado   ingresos menos gastos y costos
     */
    public record CifrasDelMes(BigDecimal ventasNetas, BigDecimal resultado) {
    }

    private final RepositorioCuentas cuentas;
    private final RepositorioPeriodos periodos;
    private final RepositorioPolizas polizas;
    private final PublicadorEventos eventos;
    private final Clock reloj;
    private final Mayor mayor;

    /**
     * Crea el libro.
     *
     * @param cuentas  tabla de cuentas
     * @param periodos tabla de periodos
     * @param polizas  tablas de polizas y movimientos
     * @param saldos   tabla de saldos por cuenta y periodo
     * @param eventos  publicacion de eventos (E-08)
     * @param reloj    reloj de la plataforma
     */
    public LibroDiario(RepositorioCuentas cuentas, RepositorioPeriodos periodos, RepositorioPolizas polizas,
                       RepositorioSaldos saldos, PublicadorEventos eventos, Clock reloj) {
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.periodos = Objects.requireNonNull(periodos, "periodos");
        this.polizas = Objects.requireNonNull(polizas, "polizas");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        this.mayor = new Mayor(cuentas, periodos, saldos);
    }

    /**
     * Revisa que todas las cuentas del asiento existan en el negocio, sean afectables y esten activas (A-04).
     *
     * @param negocioId negocio
     * @param asiento   asiento a revisar
     * @throws ContabilidadException {@code CUENTA_NO_ENCONTRADA}, {@code CUENTA_NO_AFECTABLE} o
     *                               {@code CUENTA_INACTIVA}
     */
    public void validarCuentas(UUID negocioId, Asiento asiento) {
        for (var renglon : asiento.renglones()) {
            validarCuenta(negocioId, renglon.cuentaId());
        }
    }

    /**
     * Revisa que una cuenta exista en el negocio, sea afectable y este activa (A-04).
     *
     * @param negocioId negocio
     * @param cuentaId  cuenta; nula cuenta como inexistente
     * @throws ContabilidadException {@code CUENTA_NO_ENCONTRADA}, {@code CUENTA_NO_AFECTABLE} o
     *                               {@code CUENTA_INACTIVA}
     */
    public void validarCuenta(UUID negocioId, UUID cuentaId) {
        var cuenta = cuentaId == null ? null : cuentas.porId(negocioId, cuentaId).orElse(null);
        if (cuenta == null) {
            throw new ContabilidadException(CodigoError.CUENTA_NO_ENCONTRADA);
        }
        if (!cuenta.afectable()) {
            throw new ContabilidadException(CodigoError.CUENTA_NO_AFECTABLE, cuenta.id());
        }
        if (!cuenta.recibeMovimientos()) {
            throw new ContabilidadException(CodigoError.CUENTA_INACTIVA, cuenta.id());
        }
    }

    /**
     * Revisa todo lo que se puede revisar antes de registrar: las cuentas (A-04) y que el mes del asiento no este
     * cerrado (T-01).
     *
     * @param negocioId negocio
     * @param asiento   asiento a revisar
     * @throws ContabilidadException los codigos de {@link #validarCuentas} o {@code PERIODO_CERRADO}
     */
    public void validar(UUID negocioId, Asiento asiento) {
        validarCuentas(negocioId, asiento);
        var periodo = periodos.delMes(negocioId, YearMonth.from(asiento.fecha()));
        if (periodo.isPresent() && !periodo.get().abierto()) {
            throw new ContabilidadException(CodigoError.PERIODO_CERRADO, periodo.get().id());
        }
    }

    /**
     * Registra el asiento como poliza. Una poliza automatica con fecha de un mes cerrado no falla: entra en el
     * periodo abierto mas antiguo, con fecha del dia 1, marcada como extemporanea y con la fecha original en el
     * concepto (T-05). Las manuales y las reversas si fallan con {@code PERIODO_CERRADO} (T-01).
     *
     * @param negocioId negocio
     * @param asiento   asiento cuadrado
     * @param registro  origen y referencias
     * @return la poliza registrada
     * @throws ContabilidadException los codigos de {@link #validar}
     */
    public Poliza registrar(UUID negocioId, Asiento asiento, Registro registro) {
        if (registro.origen() == OrigenPoliza.AUTOMATICA) {
            validarCuentas(negocioId, asiento);
        } else {
            validar(negocioId, asiento);
        }
        var ahora = reloj.instant();
        var periodo = periodoDe(negocioId, YearMonth.from(asiento.fecha()));
        var extemporanea = !periodo.abierto();
        if (extemporanea) {
            periodo = abiertoMasAntiguo(negocioId, periodo.mes());
            asiento = new Asiento(asiento.tipo(), periodo.mes().atDay(1),
                    recortar(asiento.concepto() + " (extemporanea, fecha original " + asiento.fecha() + ")"),
                    asiento.renglones());
        }
        var folio = polizas.siguienteFolio(negocioId, periodo.id(), asiento.tipo());
        var poliza = new Poliza(UuidV7.generar(ahora), negocioId, periodo.id(), asiento.tipo(), folio,
                asiento.fecha(), asiento.concepto(), registro.origen(), registro.eventoId(), registro.referenciaTipo(),
                registro.referenciaId(), extemporanea, registro.reviertePolizaId(), asiento.total(),
                asiento.renglones(), registro.creadoPor(), ahora);
        polizas.insertar(poliza);
        synchronized (mayor) {
            mayor.aplicar(poliza, periodo);
        }
        eventos.publicar(EventosContabilidad.polizaRegistrada(poliza, periodo.mes(), ahora));
        return poliza;
    }

    /**
     * Balanza de comprobacion de un mes.
     *
     * @param negocioId negocio
     * @param mes       anio y mes
     * @return la balanza; si el mes no tiene periodo, cada cuenta muestra el saldo que arrastra
     */
    public Balanza balanza(UUID negocioId, YearMonth mes) {
        synchronized (mayor) {
            return mayor.balanza(negocioId, mes);
        }
    }

    /**
     * Abre el periodo de un mes si no existe (T-04). Lo usan la tarea del dia 1 y el primer evento del mes.
     *
     * @param negocioId negocio
     * @param mes       anio y mes
     * @return verdadero si lo abrio; falso si ya existia
     */
    public synchronized boolean abrirPeriodo(UUID negocioId, YearMonth mes) {
        if (periodos.delMes(negocioId, mes).isPresent()) {
            return false;
        }
        periodoDe(negocioId, mes);
        return true;
    }

    /** El periodo abierto mas antiguo posterior a {@code cerrado}; si no hay, abre el mes siguiente al ultimo. */
    private synchronized Periodo abiertoMasAntiguo(UUID negocioId, YearMonth cerrado) {
        var todos = periodos.delNegocio(negocioId);
        return todos.stream().filter(p -> p.abierto() && p.mes().isAfter(cerrado)).findFirst()
                .orElseGet(() -> periodoDe(negocioId, todos.get(todos.size() - 1).mes().plusMonths(1)));
    }

    private static String recortar(String concepto) {
        return concepto.length() <= MAX_CONCEPTO ? concepto : concepto.substring(0, MAX_CONCEPTO);
    }

    /**
     * Ventas netas y resultado de un mes, para el evento {@code periodo.cerrado}. Ventas netas: el neto de las cuentas
     * 401 (ventas) menos el de las 402 (descuentos). Resultado: ingresos menos gastos y costos del mes. No incluye
     * costo de ventas (K-4).
     *
     * @param negocioId negocio
     * @param mes       anio y mes
     * @return las cifras del mes
     */
    public CifrasDelMes cifrasDelMes(UUID negocioId, YearMonth mes) {
        var tipos = new HashMap<UUID, TipoCuenta>();
        cuentas.delNegocio(negocioId).forEach(c -> tipos.put(c.id(), c.tipo()));
        var ventasNetas = BigDecimal.ZERO;
        var resultado = BigDecimal.ZERO;
        for (var fila : balanza(negocioId, mes).filas()) {
            var acreedor = fila.abonos().subtract(fila.cargos());
            if (fila.codigo().startsWith("401") || fila.codigo().startsWith("402")) {
                ventasNetas = ventasNetas.add(acreedor);
            }
            var tipo = tipos.get(fila.cuentaId());
            if (tipo == TipoCuenta.INGRESO || tipo == TipoCuenta.GASTO || tipo == TipoCuenta.COSTO) {
                resultado = resultado.add(acreedor);
            }
        }
        return new CifrasDelMes(Montos.redondear(ventasNetas), Montos.redondear(resultado));
    }

    private synchronized Periodo periodoDe(UUID negocioId, YearMonth mes) {
        return periodos.delMes(negocioId, mes).orElseGet(() -> {
            var nuevo = Periodo.abierto(UuidV7.generar(reloj.instant()), negocioId, mes, reloj.instant());
            periodos.guardar(nuevo);
            return nuevo;
        });
    }
}
