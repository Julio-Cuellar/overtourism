package mx.negocio360.contabilidad.aplicacion;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.compartido.identidad.UuidV7;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioConfiguracion;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioReglas;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.ContabilidadException;
import mx.negocio360.contabilidad.dominio.DesgloseIva;
import mx.negocio360.contabilidad.dominio.ReglaContable;

/**
 * {@code ConfigurarRegla}: agrega una regla de contabilizacion. Las tasas fiscales se capturan con su fuente y nunca
 * se cargan de memoria (F-01). Para un mismo disparador y condicion no hay traslapes: la regla nueva empieza despues
 * de la vigente y la cierra (REGLA_SUPERPUESTA si no).
 */
public final class ManejadorConfigurarRegla implements ManejadorComando<ConfigurarReglaComando> {

    private static final int MAX_FUENTE = 200;
    private static final int MAX_CONDICION = 30;

    private final RepositorioConfiguracion configuracion;
    private final RepositorioReglas reglas;
    private final LibroDiario libro;
    private final Clock reloj;

    /**
     * Crea el manejador.
     *
     * @param configuracion tabla de configuracion
     * @param reglas        tabla de reglas
     * @param libro         libro diario, para revisar las cuentas (A-04)
     * @param reloj         reloj de la plataforma
     */
    public ManejadorConfigurarRegla(RepositorioConfiguracion configuracion, RepositorioReglas reglas,
                                    LibroDiario libro, Clock reloj) {
        this.configuracion = Objects.requireNonNull(configuracion, "configuracion");
        this.reglas = Objects.requireNonNull(reglas, "reglas");
        this.libro = Objects.requireNonNull(libro, "libro");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public String tipo() {
        return "ConfigurarRegla";
    }

    @Override
    public Class<ConfigurarReglaComando> claseComando() {
        return ConfigurarReglaComando.class;
    }

    /**
     * Valida la regla sin guardarla.
     *
     * @throws ContabilidadException {@code CONTABILIDAD_NO_ACTIVA}, {@code TASA_INVALIDA}, {@code REGLA_SUPERPUESTA}
     *                               o los codigos de cuenta de A-04
     * @throws IllegalArgumentException si falta el disparador o la condicion es demasiado larga
     */
    @Override
    public String preparar(ContextoNegocio contexto, ConfigurarReglaComando comando) {
        var regla = nueva(contexto, comando);
        return "Se configurara la regla " + regla.disparador() + (regla.condicion() == null ? ""
                : " " + regla.condicion()) + (regla.tasaIva() == null ? " sin IVA (exenta)"
                : " con tasa " + regla.tasaIva()) + ".";
    }

    /**
     * Revalida, cierra la regla anterior y guarda la nueva.
     *
     * @throws ContabilidadException los mismos codigos que {@link #preparar}
     */
    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, ConfigurarReglaComando comando) {
        var regla = nueva(contexto, comando);
        abierta(contexto.negocioId(), comando).ifPresent(a -> reglas.guardar(new ReglaContable(a.id(),
                a.negocioId(), a.disparador(), a.condicion(), a.cuentaCargoId(), a.cuentaAbonoId(),
                a.cuentaImpuestoId(), a.tasaIva(), a.fuente(), a.vigenteDesde(), regla.vigenteDesde(),
                a.version() + 1)));
        reglas.guardar(regla);
        return ResultadoComando.ok("Regla configurada.", regla.id());
    }

    private ReglaContable nueva(ContextoNegocio contexto, ConfigurarReglaComando comando) {
        var negocioId = contexto.negocioId();
        Verificaciones.activa(configuracion, negocioId);
        if (comando.disparador() == null) {
            throw new IllegalArgumentException("Falta el disparador de la regla");
        }
        var condicion = comando.condicion() == null || comando.condicion().isBlank() ? null
                : comando.condicion().strip();
        if (condicion != null && condicion.length() > MAX_CONDICION) {
            throw new IllegalArgumentException("La condicion tiene hasta " + MAX_CONDICION + " caracteres");
        }
        var fuente = comando.fuente() == null ? "" : comando.fuente().strip();
        if (fuente.isEmpty() || fuente.length() > MAX_FUENTE) {
            throw new ContabilidadException(CodigoError.TASA_INVALIDA);
        }
        DesgloseIva.validarTasa(comando.tasaIva());
        if (comando.tasaIva() != null && comando.tasaIva().signum() > 0 && comando.cuentaImpuestoId() == null) {
            throw new ContabilidadException(CodigoError.TASA_INVALIDA);
        }
        libro.validarCuenta(negocioId, comando.cuentaCargoId());
        libro.validarCuenta(negocioId, comando.cuentaAbonoId());
        if (comando.cuentaImpuestoId() != null) {
            libro.validarCuenta(negocioId, comando.cuentaImpuestoId());
        }
        var inicio = comando.vigenteDesde() == null ? reloj.instant() : comando.vigenteDesde();
        var vigente = abierta(negocioId, new ConfigurarReglaComando(comando.disparador(), condicion, null, null, null,
                null, null, null));
        if (vigente.isPresent() && !inicio.isAfter(vigente.get().vigenteDesde())) {
            throw new ContabilidadException(CodigoError.REGLA_SUPERPUESTA, vigente.get().id());
        }
        return new ReglaContable(UuidV7.generar(reloj.instant()), negocioId, comando.disparador(), condicion,
                comando.cuentaCargoId(), comando.cuentaAbonoId(), comando.cuentaImpuestoId(), comando.tasaIva(),
                fuente, inicio, null, 0);
    }

    /** La regla sin fin de vigencia para el mismo disparador y condicion, si existe. */
    private Optional<ReglaContable> abierta(UUID negocioId, ConfigurarReglaComando comando) {
        var condicion = comando.condicion() == null || comando.condicion().isBlank() ? null
                : comando.condicion().strip();
        return reglas.delNegocio(negocioId).stream()
                .filter(r -> r.disparador() == comando.disparador() && Objects.equals(r.condicion(), condicion)
                        && r.vigenteHasta() == null)
                .max(Comparator.comparing(ReglaContable::vigenteDesde, Instant::compareTo));
    }
}
