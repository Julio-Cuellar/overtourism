package mx.negocio360.clientes.fakes;

import java.util.List;
import java.util.UUID;
import mx.negocio360.clientes.ClientesApi;
import mx.negocio360.clientes.aplicacion.ClientesServicio;
import mx.negocio360.clientes.aplicacion.Comando;
import mx.negocio360.clientes.aplicacion.ManejadorEditarCliente;
import mx.negocio360.clientes.aplicacion.ManejadorPublicarAviso;
import mx.negocio360.clientes.aplicacion.ManejadorRegistrarCliente;
import mx.negocio360.clientes.aplicacion.RegistrarClienteComando;
import mx.negocio360.clientes.aplicacion.ResultadoComando;
import mx.negocio360.clientes.aplicacion.ServicioPreparacion;
import mx.negocio360.compartido.contexto.ContextoNegocio;

/** Arma el modulo completo con dobles en memoria y un reloj controlado por la prueba. */
public final class Escenario {

    public final RelojMutable reloj = new RelojMutable(Fixtures.AHORA);
    public final UUID negocioA = UUID.randomUUID();
    public final UUID negocioB = UUID.randomUUID();
    public final ContextoNegocio ctxA = new ContextoNegocio(negocioA, "locatario-a");
    public final ContextoNegocio ctxB = new ContextoNegocio(negocioB, "locatario-b");

    public final RepositorioClientesEnMemoria clientes = new RepositorioClientesEnMemoria();
    public final RepositorioAvisosEnMemoria avisos = new RepositorioAvisosEnMemoria();
    public final RepositorioConsentimientosEnMemoria consentimientos = new RepositorioConsentimientosEnMemoria();
    public final RepositorioPreparacionesEnMemoria preparaciones = new RepositorioPreparacionesEnMemoria();
    public final PublicadorEventosEnMemoria eventos = new PublicadorEventosEnMemoria();

    public final ClientesApi api;

    public Escenario() {
        var preparacion = new ServicioPreparacion(preparaciones,
                List.of(new ManejadorPublicarAviso(avisos, reloj),
                        new ManejadorRegistrarCliente(clientes, avisos, consentimientos, eventos, reloj),
                        new ManejadorEditarCliente(clientes, reloj)),
                reloj);
        api = new ClientesServicio(preparacion, clientes, avisos);
    }

    /** Publica el aviso de privacidad de ejemplo para ese negocio, para que se puedan registrar clientes. */
    public Escenario conAviso(ContextoNegocio contexto) {
        confirmar(contexto, Fixtures.avisoValido());
        return this;
    }

    /** Prepara y confirma un comando, como lo haria el panel en dos pasos. */
    public ResultadoComando confirmar(ContextoNegocio contexto, Comando comando) {
        return api.confirmar(contexto, api.preparar(contexto, comando).token());
    }

    public UUID registrar(ContextoNegocio contexto, RegistrarClienteComando comando) {
        return confirmar(contexto, comando).recursoId();
    }
}
