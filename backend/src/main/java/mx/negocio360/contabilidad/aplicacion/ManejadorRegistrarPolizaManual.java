package mx.negocio360.contabilidad.aplicacion;

import java.util.Objects;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioConfiguracion;
import mx.negocio360.contabilidad.dominio.Asiento;
import mx.negocio360.contabilidad.dominio.Renglon;

/** {@code RegistrarPolizaManual}: registra una poliza capturada por una persona (A-01 a A-05, T-01). */
public final class ManejadorRegistrarPolizaManual implements ManejadorComando<RegistrarPolizaManualComando> {

    private static final int MAX_CONCEPTO = 200;

    private final RepositorioConfiguracion configuracion;
    private final LibroDiario libro;

    /**
     * Crea el manejador.
     *
     * @param configuracion tabla de configuracion
     * @param libro         libro diario
     */
    public ManejadorRegistrarPolizaManual(RepositorioConfiguracion configuracion, LibroDiario libro) {
        this.configuracion = Objects.requireNonNull(configuracion, "configuracion");
        this.libro = Objects.requireNonNull(libro, "libro");
    }

    @Override
    public String tipo() {
        return "RegistrarPolizaManual";
    }

    @Override
    public Class<RegistrarPolizaManualComando> claseComando() {
        return RegistrarPolizaManualComando.class;
    }

    /**
     * Valida la poliza sin registrarla.
     *
     * @throws mx.negocio360.contabilidad.dominio.ContabilidadException {@code CONTABILIDAD_NO_ACTIVA}, los codigos de
     *         partida doble (A-01, A-02) y los de cuentas (A-04)
     * @throws IllegalArgumentException si falta el tipo, la fecha o el concepto
     */
    @Override
    public String preparar(ContextoNegocio contexto, RegistrarPolizaManualComando comando) {
        var asiento = asiento(contexto, comando);
        libro.validarCuentas(contexto.negocioId(), asiento);
        return "Se registrara una poliza de " + asiento.tipo().name().toLowerCase() + " por " + asiento.total()
                + ".";
    }

    /**
     * Revalida y registra la poliza.
     *
     * @throws mx.negocio360.contabilidad.dominio.ContabilidadException los mismos codigos que {@link #preparar}
     */
    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, RegistrarPolizaManualComando comando) {
        var poliza = libro.registrar(contexto.negocioId(), asiento(contexto, comando),
                LibroDiario.Registro.manual(contexto.actor()));
        return ResultadoComando.ok("Poliza registrada.", poliza.id());
    }

    private Asiento asiento(ContextoNegocio contexto, RegistrarPolizaManualComando comando) {
        Verificaciones.activa(configuracion, contexto.negocioId());
        var concepto = comando.concepto() == null ? "" : comando.concepto().strip();
        if (comando.tipo() == null || comando.fecha() == null || concepto.isEmpty()
                || concepto.length() > MAX_CONCEPTO) {
            throw new IllegalArgumentException("La poliza necesita tipo, fecha y un concepto de 1 a " + MAX_CONCEPTO
                    + " caracteres");
        }
        var renglones = comando.renglones() == null ? null : comando.renglones().stream()
                .map(r -> new Renglon(r.cuentaId(), r.cargo(), r.abono(), r.concepto())).toList();
        return new Asiento(comando.tipo(), comando.fecha(), concepto, renglones);
    }
}
