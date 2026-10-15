package mx.negocio360.contabilidad.aplicacion;

import java.time.Clock;
import java.util.Objects;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioConfiguracion;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPeriodos;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.ContabilidadException;
import mx.negocio360.contabilidad.dominio.EstadoPeriodo;
import mx.negocio360.contabilidad.dominio.Periodo;

/**
 * {@code CerrarPeriodo}: cierra un mes contable. Los meses se cierran en orden (T-02). Al cerrar se abre el mes
 * siguiente si no existe, para que las polizas nuevas tengan donde caer.
 */
public final class ManejadorCerrarPeriodo implements ManejadorComando<CerrarPeriodoComando> {

    private final RepositorioConfiguracion configuracion;
    private final RepositorioPeriodos periodos;
    private final LibroDiario libro;
    private final Clock reloj;

    /**
     * Crea el manejador.
     *
     * @param configuracion tabla de configuracion
     * @param periodos      tabla de periodos
     * @param libro         libro diario, para abrir el mes siguiente
     * @param reloj         reloj de la plataforma
     */
    public ManejadorCerrarPeriodo(RepositorioConfiguracion configuracion, RepositorioPeriodos periodos,
                                  LibroDiario libro, Clock reloj) {
        this.configuracion = Objects.requireNonNull(configuracion, "configuracion");
        this.periodos = Objects.requireNonNull(periodos, "periodos");
        this.libro = Objects.requireNonNull(libro, "libro");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public String tipo() {
        return "CerrarPeriodo";
    }

    @Override
    public Class<CerrarPeriodoComando> claseComando() {
        return CerrarPeriodoComando.class;
    }

    /**
     * Revisa que el mes se pueda cerrar, sin cerrarlo.
     *
     * @throws ContabilidadException {@code CONTABILIDAD_NO_ACTIVA}, {@code PERIODO_NO_ENCONTRADO},
     *                               {@code PERIODO_CERRADO} o {@code PERIODO_ANTERIOR_ABIERTO}
     */
    @Override
    public String preparar(ContextoNegocio contexto, CerrarPeriodoComando comando) {
        var periodo = cerrable(contexto, comando);
        return "Al cerrar " + periodo.mes() + " ya no podras registrar polizas en ese mes.";
    }

    /**
     * Revalida, cierra el mes y abre el siguiente si no existe.
     *
     * @throws ContabilidadException los mismos codigos que {@link #preparar}
     */
    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, CerrarPeriodoComando comando) {
        var p = cerrable(contexto, comando);
        periodos.guardar(new Periodo(p.id(), p.negocioId(), p.mes(), EstadoPeriodo.CERRADO, p.abiertoEn(),
                reloj.instant(), contexto.actor(), p.reaperturas(), p.version() + 1));
        libro.abrirPeriodo(contexto.negocioId(), p.mes().plusMonths(1));
        return ResultadoComando.ok("Periodo cerrado.", p.id());
    }

    private Periodo cerrable(ContextoNegocio contexto, CerrarPeriodoComando comando) {
        var negocioId = contexto.negocioId();
        Verificaciones.activa(configuracion, negocioId);
        if (comando.periodo() == null) {
            throw new ContabilidadException(CodigoError.PERIODO_NO_ENCONTRADO);
        }
        var periodo = periodos.delMes(negocioId, comando.periodo())
                .orElseThrow(() -> new ContabilidadException(CodigoError.PERIODO_NO_ENCONTRADO));
        if (!periodo.abierto()) {
            throw new ContabilidadException(CodigoError.PERIODO_CERRADO, periodo.id());
        }
        var anteriorAbierto = periodos.delNegocio(negocioId).stream()
                .filter(p -> p.mes().isBefore(periodo.mes()) && p.abierto()).findFirst();
        if (anteriorAbierto.isPresent()) {
            throw new ContabilidadException(CodigoError.PERIODO_ANTERIOR_ABIERTO, anteriorAbierto.get().id());
        }
        return periodo;
    }
}
