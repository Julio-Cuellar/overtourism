package mx.negocio360.contabilidad.fakes;

import java.util.List;
import java.util.UUID;
import mx.negocio360.compartido.contexto.ContextoNegocio;
import mx.negocio360.contabilidad.ContabilidadApi;
import mx.negocio360.contabilidad.aplicacion.Comando;
import mx.negocio360.contabilidad.aplicacion.ContabilidadServicio;
import mx.negocio360.contabilidad.aplicacion.ManejadorActivarContabilidad;
import mx.negocio360.contabilidad.aplicacion.ManejadorCrearCuenta;
import mx.negocio360.contabilidad.aplicacion.ResultadoComando;
import mx.negocio360.contabilidad.aplicacion.ServicioPreparacion;

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

    public final ContabilidadApi api;

    public Escenario() {
        var preparacion = new ServicioPreparacion(preparaciones,
                List.of(new ManejadorActivarContabilidad(configuracion, cuentas, reglas, reloj),
                        new ManejadorCrearCuenta(configuracion, cuentas, reloj)),
                reloj);
        api = new ContabilidadServicio(preparacion, cuentas, reglas);
    }

    /** Prepara y confirma un comando, como lo haria el panel en dos pasos. */
    public ResultadoComando confirmar(ContextoNegocio contexto, Comando comando) {
        return api.confirmar(contexto, api.preparar(contexto, comando).token());
    }

    /** Activa la contabilidad del negocio con la tasa de ejemplo. */
    public Escenario activar(ContextoNegocio contexto) {
        confirmar(contexto, Fixtures.activar());
        return this;
    }
}
