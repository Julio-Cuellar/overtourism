package mx.negocio360.contabilidad.aplicacion;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import mx.negocio360.compartido.eventos.SobreEvento;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.compartido.tiempo.ZonaMexico;
import mx.negocio360.contabilidad.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioCortesContables;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioEventosProcesados;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPendientes;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioReglas;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioVentasContables;
import mx.negocio360.contabilidad.contratos.CajaCerradaV1;
import mx.negocio360.contabilidad.contratos.DatosInvalidosException;
import mx.negocio360.contabilidad.contratos.VentaRegistradaV1;
import mx.negocio360.contabilidad.dominio.Asiento;
import mx.negocio360.contabilidad.dominio.Conciliacion;
import mx.negocio360.contabilidad.dominio.ContabilidadException;
import mx.negocio360.contabilidad.dominio.CorteContable;
import mx.negocio360.contabilidad.dominio.Montos;
import mx.negocio360.contabilidad.dominio.DesgloseIva;
import mx.negocio360.contabilidad.dominio.Disparador;
import mx.negocio360.contabilidad.dominio.EstadoPendiente;
import mx.negocio360.contabilidad.dominio.EventoProcesado;
import mx.negocio360.contabilidad.dominio.OrigenPoliza;
import mx.negocio360.contabilidad.dominio.Pendiente;
import mx.negocio360.contabilidad.dominio.ReglaContable;
import mx.negocio360.contabilidad.dominio.Renglon;
import mx.negocio360.contabilidad.dominio.ResultadoEvento;
import mx.negocio360.contabilidad.dominio.TipoPoliza;
import mx.negocio360.contabilidad.dominio.VentaContable;

/**
 * Escrituras del sistema (seccion 7.3): convierte los eventos de otros modulos en polizas. No pide confirmacion
 * humana porque son consecuencia de hechos ya confirmados (D-22). Nunca llama a otro modulo para completar un dato:
 * si algo falta, el evento queda pendiente y la cola sigue (E-04, E-07).
 */
public final class Contabilizador {

    /** Actor de las polizas automaticas. */
    public static final String SISTEMA = "sistema";
    /** Motivo de un evento que ya se habia procesado (E-01). */
    public static final String YA_PROCESADO = "YA_PROCESADO";
    /** Motivo de un evento distinto sobre una venta, cupon o corte ya contabilizado (E-02). */
    public static final String DUPLICADO_NATURAL = "DUPLICADO_NATURAL";
    /** Pendiente: no hay regla vigente para el disparador y la condicion. */
    public static final String REGLA_FALTANTE = "REGLA_FALTANTE";
    /** Pendiente: una cuenta de la regla no recibe movimientos. */
    public static final String CUENTA_INACTIVA = "CUENTA_INACTIVA";
    /** Pendiente: el evento no trae lo que su contrato promete. */
    public static final String DATOS_INVALIDOS = "DATOS_INVALIDOS";
    /** Pendiente: el evento trae una version de contrato que Contabilidad no conoce (E-06). */
    public static final String VERSION_NO_SOPORTADA = "VERSION_NO_SOPORTADA";

    private final RepositorioEventosProcesados procesados;
    private final RepositorioPendientes pendientes;
    private final RepositorioReglas reglas;
    private final RepositorioVentasContables ventas;
    private final RepositorioCortesContables cortes;
    private final PublicadorEventos eventos;
    private final LibroDiario libro;
    private final Clock reloj;

    /**
     * Crea el contabilizador.
     *
     * @param procesados tabla de eventos procesados
     * @param pendientes tabla de pendientes
     * @param reglas     tabla de reglas
     * @param ventas     tabla de huellas de venta
     * @param cortes     tabla de cortes de caja
     * @param eventos    publicacion de eventos
     * @param libro      libro diario
     * @param reloj      reloj de la plataforma
     */
    public Contabilizador(RepositorioEventosProcesados procesados, RepositorioPendientes pendientes,
                          RepositorioReglas reglas, RepositorioVentasContables ventas,
                          RepositorioCortesContables cortes, PublicadorEventos eventos, LibroDiario libro,
                          Clock reloj) {
        this.eventos = Objects.requireNonNull(eventos, "eventos");
        this.cortes = Objects.requireNonNull(cortes, "cortes");
        this.procesados = Objects.requireNonNull(procesados, "procesados");
        this.pendientes = Objects.requireNonNull(pendientes, "pendientes");
        this.reglas = Objects.requireNonNull(reglas, "reglas");
        this.ventas = Objects.requireNonNull(ventas, "ventas");
        this.libro = Objects.requireNonNull(libro, "libro");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    /**
     * {@code contabilizarVenta}: poliza de ingreso con cargo a la cuenta del medio de pago y abono a ventas e IVA
     * (F-02), con el desglose de A-07.
     *
     * @param evento sobre de {@code venta.registrada} v1
     * @return APLICADO con la poliza; OMITIDO con {@value #YA_PROCESADO} (E-01) o {@value #DUPLICADO_NATURAL}
     *         (E-02); o PENDIENTE con {@value #REGLA_FALTANTE}, {@value #CUENTA_INACTIVA}, {@value #DATOS_INVALIDOS}
     *         (E-04) o {@value #VERSION_NO_SOPORTADA} (E-06)
     */
    public ResultadoEvento contabilizarVenta(SobreEvento evento) {
        return procesar(evento, VentaRegistradaV1.VERSION, VentaRegistradaV1.CAMPOS, this::aplicarVenta);
    }

    /**
     * {@code contabilizarCorte}: registra el corte de caja con su diferencia de conteo (J-01) y, si hay faltante o
     * sobrante, una poliza de diario con la regla CORTE_FALTANTE o CORTE_SOBRANTE (J-02).
     *
     * @param evento sobre de {@code caja.cerrada} v1
     * @return APLICADO; OMITIDO con {@value #YA_PROCESADO} o {@value #DUPLICADO_NATURAL}; o PENDIENTE como en
     *         {@link #contabilizarVenta}
     */
    public ResultadoEvento contabilizarCorte(SobreEvento evento) {
        return procesar(evento, CajaCerradaV1.VERSION, CajaCerradaV1.CAMPOS, this::aplicarCorte);
    }

    private ResultadoEvento aplicarCorte(SobreEvento evento) {
        var negocioId = evento.negocioId();
        var corte = CajaCerradaV1.leer(evento.datos());
        if (cortes.porCorte(negocioId, corte.corteId()).isPresent()) {
            return ResultadoEvento.omitido(DUPLICADO_NATURAL);
        }
        var esperado = Montos.exacto(corte.efectivoEsperado());
        var contado = Montos.exacto(corte.efectivoContado());
        var diferencia = contado.subtract(esperado);
        UUID polizaId = null;
        if (diferencia.signum() != 0) {
            var disparador = diferencia.signum() < 0 ? Disparador.CORTE_FALTANTE : Disparador.CORTE_SOBRANTE;
            var regla = reglaVigente(negocioId, disparador, null);
            if (regla.isEmpty()) {
                return ResultadoEvento.pendiente(REGLA_FALTANTE);
            }
            var monto = diferencia.abs();
            var nombre = diferencia.signum() < 0 ? "Faltante" : "Sobrante";
            var asiento = new Asiento(TipoPoliza.DIARIO, fechaContable(corte.cerradaEn()),
                    nombre + " de caja, corte " + corto(corte.corteId()), List.of(
                            new Renglon(regla.get().cuentaCargoId(), monto, null, null),
                            new Renglon(regla.get().cuentaAbonoId(), null, monto, null)));
            polizaId = libro.registrar(negocioId, asiento, new LibroDiario.Registro(OrigenPoliza.AUTOMATICA,
                    evento.id(), "CORTE", corte.corteId(), null, SISTEMA)).id();
        }
        var nuevo = new CorteContable(UuidV7.generar(reloj.instant()), negocioId, corte.corteId(),
                corte.cajaSesionId(), esperado, contado, diferencia, corte.ventasEfectivo(),
                Montos.exacto(BigDecimal.ZERO), Conciliacion.CUADRA, null, polizaId, corte.cerradaEn());
        conciliar(nuevo, evento.id());
        return ResultadoEvento.aplicado(polizaId);
    }

    /**
     * Concilia un corte contra las ventas en efectivo contabilizadas de su sesion, lo guarda y, si queda en
     * DESCUADRE_VENTAS y aun no se habia avisado, publica el aviso (J-03). No hace ningun ajuste de ventas (D-84).
     */
    private void conciliar(CorteContable corte, UUID causaId) {
        var contables = ventas.deSesion(corte.negocioId(), corte.cajaSesionId()).stream()
                .filter(v -> "EFECTIVO".equals(v.medioPago()) && !v.revertida())
                .map(VentaContable::total).reduce(Montos.exacto(BigDecimal.ZERO), BigDecimal::add);
        var descuadre = corte.ventasEfectivoCaja() != null
                && corte.ventasEfectivoCaja().compareTo(contables) != 0;
        var conciliacion = descuadre ? Conciliacion.DESCUADRE_VENTAS
                : corte.diferencia().signum() == 0 ? Conciliacion.CUADRA : Conciliacion.DIFERENCIA_CAJA;
        var avisar = conciliacion == Conciliacion.DESCUADRE_VENTAS && corte.descuadrePublicadoEn() == null;
        var ahora = reloj.instant();
        var conciliado = new CorteContable(corte.id(), corte.negocioId(), corte.corteId(), corte.cajaSesionId(),
                corte.efectivoEsperado(), corte.efectivoContado(), corte.diferencia(), corte.ventasEfectivoCaja(),
                contables, conciliacion, avisar ? ahora : corte.descuadrePublicadoEn(), corte.polizaId(),
                corte.cerradaEn());
        cortes.guardar(conciliado);
        if (avisar) {
            eventos.publicar(EventosContabilidad.descuadreDetectado(conciliado,
                    conciliado.ventasEfectivoCaja().subtract(contables), causaId, ahora));
        }
    }

    /**
     * Aplica un evento una sola vez. La fila de {@code evento_procesado} se escribe junto con el efecto: si el
     * efecto falla por una causa tecnica, el evento no queda marcado y la reentrega lo intenta de nuevo (E-01). Si
     * falla por una causa de negocio, queda pendiente (E-04). Una version que no se conoce tambien queda pendiente,
     * sin intentar leerla (E-06).
     */
    private ResultadoEvento procesar(SobreEvento evento, int versionSoportada, List<String> camposDelContrato,
                                     Function<SobreEvento, ResultadoEvento> efecto) {
        if (procesados.de(evento.negocioId(), evento.id()).isPresent()) {
            return ResultadoEvento.omitido(YA_PROCESADO);
        }
        ResultadoEvento resultado;
        try {
            resultado = evento.version() == versionSoportada ? efecto.apply(evento)
                    : ResultadoEvento.pendiente(VERSION_NO_SOPORTADA);
        } catch (DatosInvalidosException e) {
            resultado = ResultadoEvento.pendiente(DATOS_INVALIDOS);
        } catch (ContabilidadException e) {
            resultado = ResultadoEvento.pendiente(motivoDe(e));
        }
        if (resultado.resultado() == ResultadoEvento.Resultado.PENDIENTE) {
            crearPendiente(evento, camposDelContrato, resultado.motivo());
        }
        var fila = new EventoProcesado(evento.negocioId(), evento.id(), evento.tipo(), evento.version(),
                reloj.instant(), resultado.resultado(), resultado.motivo());
        if (!procesados.insertar(fila)) {
            throw new IllegalStateException("Otra entrega del mismo evento gano la carrera; se deshace esta");
        }
        return resultado;
    }

    private static String motivoDe(ContabilidadException e) {
        return switch (e.codigo()) {
            case CUENTA_INACTIVA, CUENTA_NO_AFECTABLE, CUENTA_NO_ENCONTRADA -> CUENTA_INACTIVA;
            case MONTO_INVALIDO, MOVIMIENTO_INVALIDO, POLIZA_DESCUADRADA, POLIZA_SIN_RENGLONES, TASA_INVALIDA ->
                    DATOS_INVALIDOS;
            default -> throw e;
        };
    }

    private void crearPendiente(SobreEvento evento, List<String> camposDelContrato, String motivo) {
        var datos = new LinkedHashMap<String, Object>();
        for (var campo : camposDelContrato) {
            var valor = evento.datos().get(campo);
            if (valor != null) {
                datos.put(campo, valor);
            }
        }
        var ahora = reloj.instant();
        pendientes.guardar(new Pendiente(UuidV7.generar(ahora), evento.negocioId(), evento.id(), evento.tipo(),
                Map.copyOf(datos), motivo, fechaContable(evento.ocurridoEn()), EstadoPendiente.ABIERTO, null, null,
                ahora, null));
    }

    private ResultadoEvento aplicarVenta(SobreEvento evento) {
        var negocioId = evento.negocioId();
        var venta = VentaRegistradaV1.leer(evento.datos());
        if (ventas.porVenta(negocioId, venta.ventaId()).isPresent()) {
            return ResultadoEvento.omitido(DUPLICADO_NATURAL);
        }
        var regla = reglaVigente(negocioId, Disparador.VENTA, venta.medioPago());
        if (regla.isEmpty()) {
            return ResultadoEvento.pendiente(REGLA_FALTANTE);
        }
        var desglose = DesgloseIva.de(venta.total(), regla.get().tasaIva());
        var renglones = new ArrayList<Renglon>();
        renglones.add(new Renglon(regla.get().cuentaCargoId(), venta.total(), null, null));
        renglones.add(new Renglon(regla.get().cuentaAbonoId(), null, desglose.ventas(), null));
        if (desglose.iva().signum() > 0) {
            renglones.add(new Renglon(regla.get().cuentaImpuestoId(), null, desglose.iva(), null));
        }
        var asiento = new Asiento(TipoPoliza.INGRESO, fechaContable(evento.ocurridoEn()),
                "Venta " + corto(venta.ventaId()), renglones);
        var poliza = libro.registrar(negocioId, asiento, new LibroDiario.Registro(OrigenPoliza.AUTOMATICA,
                evento.id(), "VENTA", venta.ventaId(), null, SISTEMA));
        ventas.guardar(new VentaContable(UuidV7.generar(reloj.instant()), negocioId, venta.ventaId(),
                venta.cajaSesionId(), venta.medioPago(), venta.total(), desglose.ventas(), desglose.iva(),
                evento.ocurridoEn(), poliza.id(), false));
        return ResultadoEvento.aplicado(poliza.id());
    }

    private Optional<ReglaContable> reglaVigente(UUID negocioId, Disparador disparador, String condicion) {
        var ahora = reloj.instant();
        return reglas.delNegocio(negocioId).stream()
                .filter(r -> r.disparador() == disparador && Objects.equals(r.condicion(), condicion)
                        && r.vigenteEn(ahora))
                .findFirst();
    }

    private static LocalDate fechaContable(Instant instante) {
        return LocalDate.ofInstant(instante, ZonaMexico.ZONA);
    }

    private static String corto(UUID id) {
        return id.toString().substring(0, 8);
    }
}
