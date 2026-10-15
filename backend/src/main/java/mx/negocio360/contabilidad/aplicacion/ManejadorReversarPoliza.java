package mx.negocio360.contabilidad.aplicacion;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Objects;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.tiempo.ZonaMexico;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioConfiguracion;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPolizas;
import mx.negocio360.contabilidad.dominio.Asiento;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.ContabilidadException;
import mx.negocio360.contabilidad.dominio.OrigenPoliza;
import mx.negocio360.contabilidad.dominio.Poliza;
import mx.negocio360.contabilidad.dominio.Renglon;

/**
 * {@code ReversarPoliza}: registra una poliza que invierte cada renglon de otra, con fecha de hoy, para que caiga en
 * el periodo abierto aunque la original sea de un mes cerrado (A-06).
 */
public final class ManejadorReversarPoliza implements ManejadorComando<ReversarPolizaComando> {

    private static final int MAX_MOTIVO = 150;

    private final RepositorioConfiguracion configuracion;
    private final RepositorioPolizas polizas;
    private final LibroDiario libro;
    private final Clock reloj;

    /**
     * Crea el manejador.
     *
     * @param configuracion tabla de configuracion
     * @param polizas       tablas de polizas
     * @param libro         libro diario
     * @param reloj         reloj de la plataforma
     */
    public ManejadorReversarPoliza(RepositorioConfiguracion configuracion, RepositorioPolizas polizas,
                                   LibroDiario libro, Clock reloj) {
        this.configuracion = Objects.requireNonNull(configuracion, "configuracion");
        this.polizas = Objects.requireNonNull(polizas, "polizas");
        this.libro = Objects.requireNonNull(libro, "libro");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public String tipo() {
        return "ReversarPoliza";
    }

    @Override
    public Class<ReversarPolizaComando> claseComando() {
        return ReversarPolizaComando.class;
    }

    /**
     * Valida la reversa sin registrarla.
     *
     * @throws ContabilidadException {@code CONTABILIDAD_NO_ACTIVA}, {@code MOTIVO_REQUERIDO},
     *                               {@code POLIZA_NO_ENCONTRADA}, {@code POLIZA_YA_REVERTIDA} o los codigos de
     *                               {@link LibroDiario#validar}
     */
    @Override
    public String preparar(ContextoNegocio contexto, ReversarPolizaComando comando) {
        var original = original(contexto, comando);
        libro.validar(contexto.negocioId(), asiento(original, comando));
        return "Se revertira la poliza " + original.tipo().name().toLowerCase() + " " + original.folio() + " por "
                + original.total() + ".";
    }

    /**
     * Revalida y registra la reversa.
     *
     * @throws ContabilidadException los mismos codigos que {@link #preparar}
     */
    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, ReversarPolizaComando comando) {
        var original = original(contexto, comando);
        var reversa = libro.registrar(contexto.negocioId(), asiento(original, comando),
                new LibroDiario.Registro(OrigenPoliza.REVERSA, null, original.referenciaTipo(),
                        original.referenciaId(), original.id(), contexto.actor()));
        return ResultadoComando.ok("Poliza revertida.", reversa.id());
    }

    private Poliza original(ContextoNegocio contexto, ReversarPolizaComando comando) {
        var negocioId = contexto.negocioId();
        Verificaciones.activa(configuracion, negocioId);
        var motivo = comando.motivo() == null ? "" : comando.motivo().strip();
        if (motivo.isEmpty() || motivo.length() > MAX_MOTIVO) {
            throw new ContabilidadException(CodigoError.MOTIVO_REQUERIDO);
        }
        var original = comando.polizaId() == null ? null
                : polizas.porId(negocioId, comando.polizaId()).orElse(null);
        if (original == null) {
            throw new ContabilidadException(CodigoError.POLIZA_NO_ENCONTRADA);
        }
        var reversa = polizas.reversaDe(negocioId, original.id());
        if (reversa.isPresent()) {
            throw new ContabilidadException(CodigoError.POLIZA_YA_REVERTIDA, reversa.get().id());
        }
        return original;
    }

    private Asiento asiento(Poliza original, ReversarPolizaComando comando) {
        var invertidos = original.renglones().stream()
                .map(r -> new Renglon(r.cuentaId(), r.abono(), r.cargo(), r.concepto())).toList();
        var concepto = "Reversa de " + original.tipo().name().toLowerCase() + " " + original.folio() + ": "
                + comando.motivo().strip();
        return new Asiento(original.tipo(), LocalDate.ofInstant(reloj.instant(), ZonaMexico.ZONA), concepto,
                invertidos);
    }
}
