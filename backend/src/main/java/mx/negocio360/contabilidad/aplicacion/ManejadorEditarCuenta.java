package mx.negocio360.contabilidad.aplicacion;

import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioConfiguracion;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioPolizas;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.ContabilidadException;
import mx.negocio360.contabilidad.dominio.Cuenta;
import mx.negocio360.contabilidad.dominio.Naturaleza;
import mx.negocio360.contabilidad.dominio.TipoCuenta;

/**
 * {@code EditarCuenta}: cambia nombre, codigo agrupador, tipo, naturaleza o padre. Una cuenta con movimientos no cambia
 * tipo, naturaleza ni padre (C-03), y una hija siempre tiene el tipo y la naturaleza de su padre (C-02).
 */
public final class ManejadorEditarCuenta implements ManejadorComando<EditarCuentaComando> {

    private static final int MAX_NOMBRE = 120;
    private static final int MAX_AGRUPADOR = 10;

    private final RepositorioConfiguracion configuracion;
    private final RepositorioCuentas cuentas;
    private final RepositorioPolizas polizas;

    /**
     * Crea el manejador.
     *
     * @param configuracion tabla de configuracion
     * @param cuentas       tabla de cuentas
     * @param polizas       tablas de polizas, para saber si la cuenta tiene movimientos
     */
    public ManejadorEditarCuenta(RepositorioConfiguracion configuracion, RepositorioCuentas cuentas,
                                 RepositorioPolizas polizas) {
        this.configuracion = Objects.requireNonNull(configuracion, "configuracion");
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.polizas = Objects.requireNonNull(polizas, "polizas");
    }

    @Override
    public String tipo() {
        return "EditarCuenta";
    }

    @Override
    public Class<EditarCuentaComando> claseComando() {
        return EditarCuentaComando.class;
    }

    /**
     * Valida el cambio sin guardarlo.
     *
     * @throws ContabilidadException {@code CONTABILIDAD_NO_ACTIVA}, {@code CUENTA_NO_ENCONTRADA},
     *                               {@code CONCURRENCIA}, {@code NATURALEZA_CON_MOVIMIENTOS} o
     *                               {@code NATURALEZA_INVALIDA}
     * @throws IllegalArgumentException si el nombre o el codigo agrupador no tienen forma valida
     */
    @Override
    public String preparar(ContextoNegocio contexto, EditarCuentaComando comando) {
        var editada = editada(contexto, comando);
        return "Se actualizara la cuenta " + editada.codigo() + " " + editada.nombre() + ".";
    }

    /**
     * Revalida y guarda el cambio.
     *
     * @throws ContabilidadException los mismos codigos que {@link #preparar}
     */
    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, EditarCuentaComando comando) {
        var editada = editada(contexto, comando);
        cuentas.guardar(editada);
        return ResultadoComando.ok("Cuenta actualizada.", editada.id());
    }

    private Cuenta editada(ContextoNegocio contexto, EditarCuentaComando comando) {
        var negocioId = contexto.negocioId();
        Verificaciones.activa(configuracion, negocioId);
        var actual = Verificaciones.cuentaVigente(cuentas, negocioId, comando.cuentaId(), comando.version());
        var nombre = comando.nombre() == null ? actual.nombre() : comando.nombre().strip();
        if (nombre.isEmpty() || nombre.length() > MAX_NOMBRE) {
            throw new IllegalArgumentException("El nombre de la cuenta debe tener de 1 a " + MAX_NOMBRE + " caracteres");
        }
        var agrupador = comando.codigoAgrupadorSat() == null ? actual.codigoAgrupadorSat()
                : comando.codigoAgrupadorSat().strip();
        if (agrupador != null && (agrupador.isEmpty() || agrupador.length() > MAX_AGRUPADOR)) {
            throw new IllegalArgumentException("El codigo agrupador del SAT tiene de 1 a " + MAX_AGRUPADOR
                    + " caracteres");
        }
        var tipo = comando.tipo() == null ? actual.tipo() : comando.tipo();
        var naturaleza = comando.naturaleza() == null ? actual.naturaleza() : comando.naturaleza();
        var padreId = comando.padreId() == null ? actual.padreId() : comando.padreId();
        var cambiaEstructura = tipo != actual.tipo() || naturaleza != actual.naturaleza()
                || !Objects.equals(padreId, actual.padreId());
        if (cambiaEstructura) {
            if (polizas.cuentaTieneMovimientos(negocioId, actual.id())) {
                throw new ContabilidadException(CodigoError.NATURALEZA_CON_MOVIMIENTOS, actual.id());
            }
            validarJerarquia(negocioId, actual, tipo, naturaleza, padreId);
        }
        return new Cuenta(actual.id(), negocioId, actual.codigo(), nombre, tipo, naturaleza, padreId,
                actual.afectable(), actual.deSistema(), agrupador, actual.estado(), actual.version() + 1);
    }

    private void validarJerarquia(UUID negocioId, Cuenta actual, TipoCuenta tipo, Naturaleza naturaleza,
                                  UUID padreId) {
        if (padreId != null) {
            if (padreId.equals(actual.id())) {
                throw new ContabilidadException(CodigoError.NATURALEZA_INVALIDA);
            }
            var padre = cuentas.porId(negocioId, padreId)
                    .orElseThrow(() -> new ContabilidadException(CodigoError.CUENTA_NO_ENCONTRADA));
            if (padre.tipo() != tipo || padre.naturaleza() != naturaleza) {
                throw new ContabilidadException(CodigoError.NATURALEZA_INVALIDA);
            }
        }
        var tieneHijas = cuentas.delNegocio(negocioId).stream().anyMatch(c -> actual.id().equals(c.padreId()));
        if (tieneHijas && (tipo != actual.tipo() || naturaleza != actual.naturaleza())) {
            throw new ContabilidadException(CodigoError.NATURALEZA_INVALIDA);
        }
    }
}
