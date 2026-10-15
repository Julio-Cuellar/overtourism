package mx.negocio360.contabilidad.aplicacion;

import java.time.Clock;
import java.util.Objects;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioConfiguracion;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPendientes;
import mx.negocio360.contabilidad.dominio.Asiento;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.ContabilidadException;
import mx.negocio360.contabilidad.dominio.EstadoPendiente;
import mx.negocio360.contabilidad.dominio.OrigenPoliza;
import mx.negocio360.contabilidad.dominio.Pendiente;
import mx.negocio360.contabilidad.dominio.Renglon;
import mx.negocio360.contabilidad.dominio.TipoPoliza;

/**
 * {@code ResolverPendiente}: contabiliza un pendiente con las cuentas que elige la persona, con la fecha del evento
 * original, o lo descarta con motivo (E-04).
 */
public final class ManejadorResolverPendiente implements ManejadorComando<ResolverPendienteComando> {

    private static final int MAX_MOTIVO = 200;

    private final RepositorioConfiguracion configuracion;
    private final RepositorioPendientes pendientes;
    private final LibroDiario libro;
    private final Clock reloj;

    /**
     * Crea el manejador.
     *
     * @param configuracion tabla de configuracion
     * @param pendientes    tabla de pendientes
     * @param libro         libro diario
     * @param reloj         reloj de la plataforma
     */
    public ManejadorResolverPendiente(RepositorioConfiguracion configuracion, RepositorioPendientes pendientes,
                                      LibroDiario libro, Clock reloj) {
        this.configuracion = Objects.requireNonNull(configuracion, "configuracion");
        this.pendientes = Objects.requireNonNull(pendientes, "pendientes");
        this.libro = Objects.requireNonNull(libro, "libro");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public String tipo() {
        return "ResolverPendiente";
    }

    @Override
    public Class<ResolverPendienteComando> claseComando() {
        return ResolverPendienteComando.class;
    }

    /**
     * Valida la resolucion sin aplicarla.
     *
     * @throws ContabilidadException {@code CONTABILIDAD_NO_ACTIVA}, {@code PENDIENTE_NO_ENCONTRADO},
     *                               {@code PENDIENTE_YA_RESUELTO}, {@code MOTIVO_REQUERIDO} o los codigos de la
     *                               poliza (A-01, A-02, A-04, T-01)
     * @throws IllegalArgumentException si falta la accion
     */
    @Override
    public String preparar(ContextoNegocio contexto, ResolverPendienteComando comando) {
        var pendiente = abierto(contexto, comando);
        if (comando.accion() == ResolverPendienteComando.Accion.DESCARTAR) {
            motivo(comando);
            return "Se descartara el pendiente de " + pendiente.tipoEvento() + " sin registrar poliza.";
        }
        var asiento = asiento(pendiente, comando);
        libro.validar(contexto.negocioId(), asiento);
        return "Se registrara una poliza por " + asiento.total() + " para el pendiente de " + pendiente.tipoEvento()
                + ".";
    }

    /**
     * Revalida y aplica la resolucion.
     *
     * @throws ContabilidadException los mismos codigos que {@link #preparar}
     */
    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, ResolverPendienteComando comando) {
        var pendiente = abierto(contexto, comando);
        var ahora = reloj.instant();
        if (comando.accion() == ResolverPendienteComando.Accion.DESCARTAR) {
            pendientes.guardar(pendiente.resuelto(EstadoPendiente.DESCARTADO, null, motivo(comando), ahora));
            return ResultadoComando.ok("Pendiente descartado.", pendiente.id());
        }
        var poliza = libro.registrar(contexto.negocioId(), asiento(pendiente, comando),
                new LibroDiario.Registro(OrigenPoliza.MANUAL, null, "PENDIENTE", pendiente.id(), null,
                        contexto.actor()));
        pendientes.guardar(pendiente.resuelto(EstadoPendiente.CONTABILIZADO, poliza.id(), null, ahora));
        return ResultadoComando.ok("Pendiente contabilizado.", poliza.id());
    }

    private Pendiente abierto(ContextoNegocio contexto, ResolverPendienteComando comando) {
        Verificaciones.activa(configuracion, contexto.negocioId());
        if (comando.accion() == null) {
            throw new IllegalArgumentException("Falta la accion: CONTABILIZAR o DESCARTAR");
        }
        var pendiente = comando.pendienteId() == null ? null
                : pendientes.porId(contexto.negocioId(), comando.pendienteId()).orElse(null);
        if (pendiente == null) {
            throw new ContabilidadException(CodigoError.PENDIENTE_NO_ENCONTRADO);
        }
        if (pendiente.estado() != EstadoPendiente.ABIERTO) {
            throw new ContabilidadException(CodigoError.PENDIENTE_YA_RESUELTO, pendiente.id());
        }
        return pendiente;
    }

    private static String motivo(ResolverPendienteComando comando) {
        var motivo = comando.motivo() == null ? "" : comando.motivo().strip();
        if (motivo.isEmpty() || motivo.length() > MAX_MOTIVO) {
            throw new ContabilidadException(CodigoError.MOTIVO_REQUERIDO);
        }
        return motivo;
    }

    private static Asiento asiento(Pendiente pendiente, ResolverPendienteComando comando) {
        var renglones = comando.renglones() == null ? null : comando.renglones().stream()
                .map(r -> new Renglon(r.cuentaId(), r.cargo(), r.abono(), r.concepto())).toList();
        var tipo = "venta.registrada".equals(pendiente.tipoEvento()) ? TipoPoliza.INGRESO : TipoPoliza.DIARIO;
        return new Asiento(tipo, pendiente.fechaEvento(), "Pendiente de " + pendiente.tipoEvento() + " resuelto",
                renglones);
    }
}
