package mx.negocio360.contabilidad.fakes;

import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.contabilidad.ContabilidadApi;
import mx.negocio360.contabilidad.aplicacion.Comando;
import mx.negocio360.contabilidad.aplicacion.ContabilidadServicio;
import mx.negocio360.contabilidad.aplicacion.Contabilizador;
import mx.negocio360.contabilidad.aplicacion.LibroDiario;
import mx.negocio360.contabilidad.aplicacion.ManejadorActivarContabilidad;
import mx.negocio360.contabilidad.aplicacion.ManejadorCerrarPeriodo;
import mx.negocio360.contabilidad.aplicacion.ManejadorConfigurarRegla;
import mx.negocio360.contabilidad.aplicacion.ManejadorCrearCuenta;
import mx.negocio360.contabilidad.aplicacion.ManejadorDesactivarCuenta;
import mx.negocio360.contabilidad.aplicacion.ManejadorEditarCuenta;
import mx.negocio360.contabilidad.aplicacion.ManejadorRegistrarPolizaManual;
import mx.negocio360.contabilidad.aplicacion.ManejadorResolverPendiente;
import mx.negocio360.contabilidad.aplicacion.ManejadorReversarPoliza;
import mx.negocio360.contabilidad.aplicacion.ResultadoComando;
import mx.negocio360.contabilidad.aplicacion.ServicioPreparacion;
import mx.negocio360.contabilidad.dominio.Cuenta;

/** Arma el modulo completo con dobles en memoria y un reloj controlado por la prueba. */
public final class Escenario {

    public final RelojMutable reloj = new RelojMutable(Fixtures.AHORA);
    public final UUID negocioA = UUID.randomUUID();
    public final UUID negocioB = UUID.randomUUID();
    public final ContextoNegocio ctxA = new ContextoNegocio(negocioA, "locatario-a");
    public final ContextoNegocio ctxB = new ContextoNegocio(negocioB, "locatario-b");

    public final RepositorioPreparacionesEnMemoria preparaciones = new RepositorioPreparacionesEnMemoria();
    public final RepositorioCuentasEnMemoria cuentas = new RepositorioCuentasEnMemoria();
    public final RepositorioReglasEnMemoria reglas = new RepositorioReglasEnMemoria();
    public final RepositorioConfiguracionEnMemoria configuracion = new RepositorioConfiguracionEnMemoria();
    public final RepositorioPeriodosEnMemoria periodos = new RepositorioPeriodosEnMemoria();
    public final RepositorioPolizasEnMemoria polizas = new RepositorioPolizasEnMemoria();
    public final RepositorioSaldosEnMemoria saldos = new RepositorioSaldosEnMemoria();
    public final RepositorioVentasContablesEnMemoria ventas = new RepositorioVentasContablesEnMemoria();
    public final RepositorioPendientesEnMemoria pendientes = new RepositorioPendientesEnMemoria();
    public final RepositorioEventosProcesadosEnMemoria eventosProcesados =
            new RepositorioEventosProcesadosEnMemoria();

    public final LibroDiario libro = new LibroDiario(cuentas, periodos, polizas, saldos, reloj);
    public final Contabilizador contabilizador = new Contabilizador(eventosProcesados, pendientes, reglas, ventas,
            libro, reloj);
    public final ContabilidadApi api;

    public Escenario() {
        var preparacion = new ServicioPreparacion(preparaciones,
                List.of(new ManejadorActivarContabilidad(configuracion, cuentas, reglas, reloj),
                        new ManejadorCrearCuenta(configuracion, cuentas, reloj),
                        new ManejadorEditarCuenta(configuracion, cuentas, polizas),
                        new ManejadorDesactivarCuenta(configuracion, cuentas, reglas, reloj),
                        new ManejadorRegistrarPolizaManual(configuracion, libro),
                        new ManejadorReversarPoliza(configuracion, polizas, libro, reloj),
                        new ManejadorResolverPendiente(configuracion, pendientes, libro, reloj),
                        new ManejadorConfigurarRegla(configuracion, reglas, libro, reloj),
                        new ManejadorCerrarPeriodo(configuracion, periodos, libro, reloj)),
                reloj);
        api = new ContabilidadServicio(preparacion, cuentas, reglas, polizas, libro, contabilizador,
                pendientes, configuracion, periodos);
    }

    /** Prepara y confirma un comando, como lo haria el panel en dos pasos. */
    public ResultadoComando confirmar(ContextoNegocio contexto, Comando comando) {
        return api.confirmar(contexto, api.preparar(contexto, comando).token());
    }

    /** Cuenta del negocio por codigo; falla si no existe. */
    public Cuenta cuenta(ContextoNegocio contexto, String codigo) {
        return api.catalogo(contexto).stream().filter(c -> c.codigo().equals(codigo)).findFirst().orElseThrow();
    }

    /** Activa la contabilidad del negocio con la tasa de ejemplo. */
    public Escenario activar(ContextoNegocio contexto) {
        confirmar(contexto, Fixtures.activar());
        return this;
    }
}
