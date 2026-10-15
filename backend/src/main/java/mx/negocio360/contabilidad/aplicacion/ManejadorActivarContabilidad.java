package mx.negocio360.contabilidad.aplicacion;

import java.time.Clock;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioConfiguracion;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.contabilidad.aplicacion.puertos.RepositorioReglas;
import mx.negocio360.contabilidad.dominio.CatalogoBase;
import mx.negocio360.contabilidad.dominio.CodigoError;
import mx.negocio360.contabilidad.dominio.Configuracion;
import mx.negocio360.contabilidad.dominio.ContabilidadException;
import mx.negocio360.contabilidad.dominio.DesgloseIva;

/**
 * {@code ActivarContabilidad}: copia al negocio el catalogo base y las reglas por defecto, y guarda su configuracion
 * (C-01). Activar un negocio ya activo no copia nada otra vez.
 */
public final class ManejadorActivarContabilidad implements ManejadorComando<ActivarContabilidadComando> {

    private static final int MAX_FUENTE = 200;
    private static final int ENERO = 1;
    private static final int DICIEMBRE = 12;

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
    public ManejadorActivarContabilidad(RepositorioConfiguracion configuracion, RepositorioCuentas cuentas,
                                        RepositorioReglas reglas, Clock reloj) {
        this.configuracion = Objects.requireNonNull(configuracion, "configuracion");
        this.cuentas = Objects.requireNonNull(cuentas, "cuentas");
        this.reglas = Objects.requireNonNull(reglas, "reglas");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
    }

    @Override
    public String tipo() {
        return "ActivarContabilidad";
    }

    @Override
    public Class<ActivarContabilidadComando> claseComando() {
        return ActivarContabilidadComando.class;
    }

    /**
     * Valida la tasa, su fuente y el mes de inicio del ejercicio.
     *
     * @throws ContabilidadException {@code TASA_INVALIDA} si la tasa esta fuera de rango o no trae fuente
     * @throws IllegalArgumentException si el mes de inicio no esta entre 1 y 12
     */
    @Override
    public String preparar(ContextoNegocio contexto, ActivarContabilidadComando comando) {
        validar(comando);
        if (configuracion.delNegocio(contexto.negocioId()).isPresent()) {
            return "La contabilidad de este negocio ya esta activa. No se copiara nada otra vez.";
        }
        return "Se activara la contabilidad con el catalogo base y las reglas por defecto.";
    }

    /**
     * Copia el catalogo y las reglas, y guarda la configuracion. Si el negocio ya estaba activo, no hace nada.
     *
     * @throws ContabilidadException {@code TASA_INVALIDA} si la tasa esta fuera de rango o no trae fuente
     */
    @Override
    public ResultadoComando confirmar(ContextoNegocio contexto, ActivarContabilidadComando comando) {
        validar(comando);
        var negocioId = contexto.negocioId();
        if (configuracion.delNegocio(negocioId).isPresent()) {
            return ResultadoComando.ok("La contabilidad ya estaba activa.", negocioId);
        }
        var ahora = reloj.instant();
        var porCodigo = new HashMap<String, UUID>();
        for (var cuenta : CatalogoBase.cuentasPara(negocioId, ahora)) {
            cuentas.guardar(cuenta);
            porCodigo.put(cuenta.codigo(), cuenta.id());
        }
        var fuente = comando.fuenteTasa().strip();
        CatalogoBase.reglasPara(negocioId, porCodigo, comando.tasaIvaDefault(), fuente, ahora).forEach(reglas::guardar);
        configuracion.guardar(new Configuracion(negocioId, ahora, !Boolean.FALSE.equals(comando.preciosConIva()),
                comando.tasaIvaDefault(), fuente, Boolean.TRUE.equals(comando.propinasEnTotal()),
                comando.ejercicioIniciaMes() == null ? ENERO : comando.ejercicioIniciaMes(), 0));
        return ResultadoComando.ok("Contabilidad activada.", negocioId);
    }

    private static void validar(ActivarContabilidadComando comando) {
        DesgloseIva.validarTasa(comando.tasaIvaDefault());
        var fuente = comando.fuenteTasa();
        if (fuente == null || fuente.isBlank() || fuente.strip().length() > MAX_FUENTE) {
            throw new ContabilidadException(CodigoError.TASA_INVALIDA);
        }
        var mes = comando.ejercicioIniciaMes();
        if (mes != null && (mes < ENERO || mes > DICIEMBRE)) {
            throw new IllegalArgumentException("El ejercicio debe iniciar en un mes de 1 a 12");
        }
    }
}
