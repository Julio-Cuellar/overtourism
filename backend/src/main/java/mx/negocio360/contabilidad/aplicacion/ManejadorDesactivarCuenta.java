package mx.negocio360.contabilidad.aplicacion;

import java.util.Objects;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioConfiguracion;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.contabilidad.dominio.Cuenta;
import mx.negocio360.contabilidad.dominio.EstadoCuenta;

/** {@code DesactivarCuenta}: una cuenta no se borra, se desactiva (C-03). */
public final class ManejadorDesactivarCuenta implements ManejadorComando<DesactivarCuentaComando> {

    private final RepositorioConfiguracion configuracion;
    private final RepositorioCuentas cuentas;

    /**
     * Crea el manejador.
     *
     * @param configuracion tabla de configuracion
     * @param cuentas       tabla de cuentas
     */
    public ManejadorDesactivarCuenta(RepositorioConfiguracion configuracion, RepositorioCuentas cuentas) {
        this.configuracion = Objects.requireNonNull(configuracion, "configuracion");
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
    }

    @Override
    public String tipo() {
        return "DesactivarCuenta";
    }

    @Override
    public Class<DesactivarCuentaComando> claseComando() {
        return DesactivarCuentaComando.class;
    }

    /**
     * Valida la desactivacion sin guardarla.
     *
     * @throws mx.negocio360.contabilidad.dominio.ContabilidadException {@code CONTABILIDAD_NO_ACTIVA},
     *         {@code CUENTA_NO_ENCONTRADA} o {@code CONCURRENCIA}
     */
    @Override
    public String preparar(ContextoNegocio contexto, DesactivarCuentaComando comando) {
        var cuenta = vigente(contexto, comando);
        return "Se desactivara la cuenta " + cuenta.codigo() + " " + cuenta.nombre()
                + ". Ya no recibira movimientos.";
    }

    /**
     * Revalida y desactiva la cuenta.
     *
     * @throws mx.negocio360.contabilidad.dominio.ContabilidadException los mismos codigos que {@link #preparar}
     */
    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, DesactivarCuentaComando comando) {
        var c = vigente(contexto, comando);
        cuentas.guardar(new Cuenta(c.id(), c.negocioId(), c.codigo(), c.nombre(), c.tipo(), c.naturaleza(),
                c.padreId(), c.afectable(), c.deSistema(), c.codigoAgrupadorSat(), EstadoCuenta.INACTIVA,
                c.version() + 1));
        return ResultadoComando.ok("Cuenta desactivada.", c.id());
    }

    private Cuenta vigente(ContextoNegocio contexto, DesactivarCuentaComando comando) {
        Verificaciones.activa(configuracion, contexto.negocioId());
        return Verificaciones.cuentaVigente(cuentas, contexto.negocioId(), comando.cuentaId(), comando.version());
    }
}
