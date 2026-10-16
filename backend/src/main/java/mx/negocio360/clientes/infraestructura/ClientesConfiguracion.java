package mx.negocio360.clientes.infraestructura;

import java.time.Clock;
import java.util.List;
import mx.negocio360.clientes.ClientesApi;
import mx.negocio360.clientes.aplicacion.ClientesServicio;
import mx.negocio360.clientes.aplicacion.ManejadorComando;
import mx.negocio360.clientes.aplicacion.ManejadorEditarCliente;
import mx.negocio360.clientes.aplicacion.ManejadorPublicarAviso;
import mx.negocio360.clientes.aplicacion.ManejadorRegistrarCliente;
import mx.negocio360.clientes.aplicacion.ServicioPreparacion;
import mx.negocio360.clientes.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioAvisos;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioClientes;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioConsentimientos;
import mx.negocio360.clientes.aplicacion.puertos.RepositorioPreparaciones;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Cableado de Clientes, igual en todos los perfiles: repositorios en PostgreSQL, publicador de eventos por el bus de
 * Spring y el outbox, manejadores de comandos y la API publica del modulo.
 *
 * <p>Cada {@code confirmar} de un manejador corre en una sola transaccion que incluye sus escrituras y sus eventos
 * ({@link ManejadorTransaccional}, D-103). {@code marcarUsada} y {@code vaciar} del token van en su propia transaccion
 * (lo hace el repositorio de preparaciones), aparte de la del manejador. Las consultas y el paso de preparar no escriben
 * mas que la preparacion y se unen a la transaccion que haya o abren una corta.
 */
@Configuration
public class ClientesConfiguracion {

    /** Eventos de Clientes por el bus de Spring y el outbox, en la transaccion del caso de uso (B-05). */
    @Bean
    public PublicadorEventos publicadorDeEventosDeClientes(ApplicationEventPublisher bus) {
        return new PublicadorEventosEnBus(bus);
    }

    @Bean
    public EjecutorEnNegocio ejecutorEnNegocioDeClientes(JdbcClient jdbc, PlatformTransactionManager transacciones) {
        return new EjecutorEnNegocio(jdbc, transacciones);
    }

    @Bean
    public RepositorioClientes repositorioClientes(JdbcClient jdbc, EjecutorEnNegocio negocio) {
        return new RepositorioClientesJdbc(jdbc, negocio);
    }

    @Bean
    public RepositorioAvisos repositorioAvisos(JdbcClient jdbc, EjecutorEnNegocio negocio) {
        return new RepositorioAvisosJdbc(jdbc, negocio);
    }

    @Bean
    public RepositorioConsentimientos repositorioConsentimientos(JdbcClient jdbc, EjecutorEnNegocio negocio) {
        return new RepositorioConsentimientosJdbc(jdbc, negocio);
    }

    @Bean
    public RepositorioPreparaciones repositorioPreparaciones(JdbcClient jdbc, EjecutorEnNegocio negocio) {
        return new RepositorioPreparacionesJdbc(jdbc, negocio);
    }

    @Bean
    public ServicioPreparacion servicioPreparacion(RepositorioPreparaciones preparaciones, RepositorioClientes clientes,
                                                   RepositorioAvisos avisos, RepositorioConsentimientos consentimientos,
                                                   PublicadorEventos eventos, TransactionOperations transacciones,
                                                   Clock reloj) {
        List<ManejadorComando<?>> manejadores = List.of(
                ManejadorTransaccional.de(new ManejadorPublicarAviso(avisos, reloj), transacciones),
                ManejadorTransaccional.de(
                        new ManejadorRegistrarCliente(clientes, avisos, consentimientos, eventos, reloj), transacciones),
                ManejadorTransaccional.de(new ManejadorEditarCliente(clientes, reloj), transacciones));
        return new ServicioPreparacion(preparaciones, manejadores, reloj);
    }

    @Bean
    public ClientesApi clientesApi(ServicioPreparacion preparacion, RepositorioClientes clientes,
                                   RepositorioAvisos avisos) {
        return new ClientesServicio(preparacion, clientes, avisos);
    }
}
