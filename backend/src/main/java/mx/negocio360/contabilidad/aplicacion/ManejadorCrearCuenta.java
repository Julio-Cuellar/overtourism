package mx.negocio360.contabilidad.aplicacion;

import java.time.Clock;
import java.util.Objects;
import java.util.regex.Pattern;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioConfiguracion;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.ContabilidadException;
import mx.negocio360.contabilidad.dominio.Cuenta;
import mx.negocio360.contabilidad.dominio.EstadoCuenta;
import mx.negocio360.contabilidad.dominio.Naturaleza;
import mx.negocio360.contabilidad.dominio.TipoCuenta;

/**
 * {@code CrearCuenta}: agrega una cuenta al catalogo del negocio. El codigo es unico por negocio y una hija tiene el
 * tipo y la naturaleza de su padre (C-02).
 */
public final class ManejadorCrearCuenta implements ManejadorComando<CrearCuentaComando> {

    private static final Pattern CODIGO = Pattern.compile("[0-9A-Za-z][0-9A-Za-z.\\-]{0,19}");
    private static final int MAX_NOMBRE = 120;
    private static final int MAX_AGRUPADOR = 10;

    private final RepositorioConfiguracion configuracion;
    private final RepositorioCuentas cuentas;
    private final Clock reloj;

    /**
     * Crea el manejador.
     *
     * @param configuracion tabla de configuracion
     * @param cuentas       tabla de cuentas
     * @param reloj         reloj de la plataforma
     */
    public ManejadorCrearCuenta(RepositorioConfiguracion configuracion, RepositorioCuentas cuentas, Clock reloj) {
        this.configuracion = Objects.requireNonNull(configuracion, "configuracion");
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public String tipo() {
        return "CrearCuenta";
    }

    @Override
    public Class<CrearCuentaComando> claseComando() {
        return CrearCuentaComando.class;
    }

    /**
     * Valida la cuenta nueva sin guardarla.
     *
     * @throws ContabilidadException {@code CONTABILIDAD_NO_ACTIVA}, {@code CODIGO_CUENTA_DUPLICADO},
     *                               {@code CUENTA_NO_ENCONTRADA} (padre) o {@code NATURALEZA_INVALIDA}
     * @throws IllegalArgumentException si el codigo, el nombre o el codigo agrupador no tienen forma valida
     */
    @Override
    public String preparar(ContextoNegocio contexto, CrearCuentaComando comando) {
        var cuenta = armar(contexto, comando);
        return "Se creara la cuenta " + cuenta.codigo() + " " + cuenta.nombre() + ".";
    }

    /**
     * Revalida y guarda la cuenta.
     *
     * @throws ContabilidadException los mismos codigos que {@link #preparar}
     */
    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, CrearCuentaComando comando) {
        var cuenta = armar(contexto, comando);
        cuentas.guardar(cuenta);
        return ResultadoComando.ok("Cuenta creada.", cuenta.id());
    }

    private Cuenta armar(ContextoNegocio contexto, CrearCuentaComando comando) {
        var negocioId = contexto.negocioId();
        Verificaciones.activa(configuracion, negocioId);
        var codigo = comando.codigo() == null ? "" : comando.codigo().strip();
        var nombre = comando.nombre() == null ? "" : comando.nombre().strip();
        if (!CODIGO.matcher(codigo).matches()) {
            throw new IllegalArgumentException("El codigo de cuenta no tiene una forma valida");
        }
        if (nombre.isEmpty() || nombre.length() > MAX_NOMBRE) {
            throw new IllegalArgumentException("El nombre de la cuenta debe tener de 1 a " + MAX_NOMBRE + " caracteres");
        }
        var agrupador = comando.codigoAgrupadorSat();
        if (agrupador != null && (agrupador.isBlank() || agrupador.length() > MAX_AGRUPADOR)) {
            throw new IllegalArgumentException("El codigo agrupador del SAT tiene de 1 a " + MAX_AGRUPADOR
                    + " caracteres");
        }
        if (cuentas.porCodigo(negocioId, codigo).isPresent()) {
            throw new ContabilidadException(CodigoError.CODIGO_CUENTA_DUPLICADO);
        }
        TipoCuenta tipo = comando.tipo();
        Naturaleza naturaleza = comando.naturaleza();
        if (comando.padreId() != null) {
            var padre = cuentas.porId(negocioId, comando.padreId())
                    .orElseThrow(() -> new ContabilidadException(CodigoError.CUENTA_NO_ENCONTRADA));
            if ((tipo != null && tipo != padre.tipo()) || (naturaleza != null && naturaleza != padre.naturaleza())) {
                throw new ContabilidadException(CodigoError.NATURALEZA_INVALIDA);
            }
            tipo = padre.tipo();
            naturaleza = padre.naturaleza();
        } else if (tipo == null || naturaleza == null) {
            throw new ContabilidadException(CodigoError.NATURALEZA_INVALIDA);
        }
        return new Cuenta(UuidV7.generar(reloj.instant()), negocioId, codigo, nombre, tipo, naturaleza,
                comando.padreId(), !Boolean.FALSE.equals(comando.afectable()), false, agrupador,
                EstadoCuenta.ACTIVA, 0);
    }
}
