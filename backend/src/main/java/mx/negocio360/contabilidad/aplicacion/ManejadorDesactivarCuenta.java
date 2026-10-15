package mx.negocio360.contabilidad.aplicacion;

import java.time.Clock;
import java.util.Objects;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioConfiguracion;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioReglas;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.ContabilidadException;
import mx.negocio360.contabilidad.dominio.Cuenta;
import mx.negocio360.contabilidad.dominio.EstadoCuenta;

/**
 * {@code DesactivarCuenta}: una cuenta no se borra, se desactiva (C-03). Una cuenta que usa una regla vigente no se
 * desactiva, para que ningun evento quede sin cuenta (C-04).
 */
public final class ManejadorDesactivarCuenta implements ManejadorComando<DesactivarCuentaComando> {

    private final RepositorioConfiguracion configuracion;
    private final RepositorioCuentas cuentas;
    private final RepositorioReglas reglas;
    private final Clock reloj;

    /**
     * Crea el manejador.
     *
     * @param configuracion tabla de configuracion
     * @param cuentas       tabla de cuentas
     * @param reglas        tabla de reglas
     * @param reloj         reloj de la plataforma
     */
    public ManejadorDesactivarCuenta(RepositorioConfiguracion configuracion, RepositorioCuentas cuentas,
                                     RepositorioReglas reglas, Clock reloj) {
        this.configuracion = Objects.requireNonNull(configuracion, "configuracion");
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.reglas = Objects.requireNonNull(reglas, "reglas");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
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
     *         {@code CUENTA_NO_ENCONTRADA}, {@code CONCURRENCIA} o {@code CUENTA_EN_USO}
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
        var cuenta = Verificaciones.cuentaVigente(cuentas, contexto.negocioId(), comando.cuentaId(),
                comando.version());
        var ahora = reloj.instant();
        var enUso = reglas.delNegocio(contexto.negocioId()).stream()
                .anyMatch(r -> r.vigenteEn(ahora) && r.usaCuenta(cuenta.id()));
        if (enUso) {
            throw new ContabilidadException(CodigoError.CUENTA_EN_USO, cuenta.id());
        }
        return cuenta;
    }
}
