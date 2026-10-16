package mx.negocio360.negocios.infraestructura;

import java.time.Clock;
import mx.negocio360.compartido.transaccion.FronteraTransaccional;
import mx.negocio360.negocios.aplicacion.AltaServicio;
import mx.negocio360.negocios.AdministracionApi;
import mx.negocio360.negocios.DemostracionApi;
import mx.negocio360.negocios.NegociosApi;
import mx.negocio360.negocios.NegociosPublicosApi;
import mx.negocio360.negocios.ResenasPublicasApi;
import mx.negocio360.negocios.aplicacion.AdministracionServicio;
import mx.negocio360.negocios.aplicacion.AltasApi;
import mx.negocio360.negocios.aplicacion.DemostracionServicio;
import mx.negocio360.negocios.aplicacion.NegociosServicio;
import mx.negocio360.negocios.aplicacion.PublicoServicio;
import mx.negocio360.negocios.aplicacion.ResenasPublicasServicio;
import mx.negocio360.negocios.aplicacion.ResenasServicio;
import mx.negocio360.negocios.aplicacion.puertos.ModoDemostracion;
import mx.negocio360.negocios.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioAltas;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioCatalogos;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioDemostracion;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioNegocios;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioPreparacionesResena;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioPublico;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioResenas;
import mx.negocio360.negocios.aplicacion.puertos.RepositorioSucursales;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Cableado de Negocios, igual en todos los perfiles: repositorios en PostgreSQL, publicador de eventos por el bus de
 * Spring y el outbox, y los servicios envueltos en {@link FronteraTransaccional} (D-103): cada llamada a la API es una
 * transaccion que incluye sus escrituras y sus eventos, y los servicios siguen siendo Java puro.
 */
@Configuration
public class NegociosConfiguracion {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock relojDeNegocios() {
        return Clock.systemUTC();
    }

    @Bean
    public EjecutorEnContexto ejecutorEnContextoDeNegocios(JdbcClient jdbc, PlatformTransactionManager transacciones) {
        return new EjecutorEnContexto(jdbc, transacciones);
    }

    @Bean
    public PublicadorEventos publicadorDeEventosDeNegocios(ApplicationEventPublisher bus) {
        return new PublicadorEventosEnBus(bus);
    }

    @Bean
    public RepositorioAltas repositorioDeAltasDeNegocios(JdbcClient jdbc, EjecutorEnContexto contexto) {
        return new RepositorioAltasJdbc(jdbc, contexto);
    }

    @Bean
    public RepositorioNegocios repositorioDeNegocios(JdbcClient jdbc, EjecutorEnContexto contexto) {
        return new RepositorioNegociosJdbc(jdbc, contexto);
    }

    @Bean
    public RepositorioSucursales repositorioDeSucursales(JdbcClient jdbc, EjecutorEnContexto contexto) {
        return new RepositorioSucursalesJdbc(jdbc, contexto);
    }

    @Bean
    public RepositorioCatalogos repositorioDeCatalogosDeNegocios(JdbcClient jdbc) {
        return new RepositorioCatalogosJdbc(jdbc);
    }

    @Bean
    public RepositorioPublico repositorioPublicoDeNegocios(JdbcClient jdbc, PlatformTransactionManager transacciones) {
        return new RepositorioPublicoJdbc(jdbc, transacciones);
    }

    @Bean
    public RepositorioResenas repositorioDeResenas(JdbcClient jdbc, EjecutorEnContexto contexto,
                                                   PlatformTransactionManager transacciones) {
        return new RepositorioResenasJdbc(jdbc, contexto, transacciones);
    }

    @Bean
    public RepositorioPreparacionesResena repositorioDePreparacionesDeResena(JdbcClient jdbc) {
        return new RepositorioPreparacionesResenaJdbc(jdbc);
    }

    @Bean
    public RepositorioDemostracion repositorioDeDemostracion(JdbcClient jdbc, EjecutorEnContexto contexto) {
        return new RepositorioDemostracionJdbc(jdbc, contexto);
    }

    @Bean
    public ResenasServicio resenasServicio(RepositorioResenas resenas, RepositorioNegocios negocios,
                                           PublicadorEventos eventos, Clock reloj) {
        return new ResenasServicio(resenas, negocios, eventos, reloj);
    }

    @Bean
    public NegociosApi negociosApi(RepositorioNegocios negocios, RepositorioSucursales sucursales,
                                   RepositorioCatalogos catalogos, RepositorioAltas altas, ResenasServicio resenas,
                                   PublicadorEventos eventos, Clock reloj, TransactionOperations transacciones) {
        return FronteraTransaccional.envolver(NegociosApi.class,
                new NegociosServicio(negocios, sucursales, catalogos, altas, resenas, eventos, reloj), transacciones);
    }

    @Bean
    public AdministracionApi administracionApi(ResenasServicio resenas, TransactionOperations transacciones) {
        return FronteraTransaccional.envolver(AdministracionApi.class, new AdministracionServicio(resenas),
                transacciones);
    }

    @Bean
    public ResenasPublicasApi resenasPublicasApi(ModoDemostracion modo, RepositorioNegocios negocios,
                                                 RepositorioResenas resenas, RepositorioPreparacionesResena preparaciones,
                                                 PublicadorEventos eventos, Clock reloj,
                                                 TransactionOperations transacciones) {
        return FronteraTransaccional.envolver(ResenasPublicasApi.class,
                new ResenasPublicasServicio(modo, negocios, resenas, preparaciones, eventos, reloj), transacciones);
    }

    /** Sin envolver en una sola transaccion: cada paso es la de un negocio y una transaccion no puede cambiar de negocio. */
    @Bean
    public DemostracionApi demostracionApi(ModoDemostracion modo, AltasApi altas, NegociosApi panel,
                                           RepositorioDemostracion demostracion, RepositorioNegocios negocios,
                                           RepositorioResenas resenas, RepositorioPreparacionesResena preparaciones,
                                           TransactionOperations transacciones, Clock reloj) {
        return new DemostracionServicio(modo, altas, panel, demostracion, negocios, resenas, preparaciones,
                transacciones, reloj);
    }

    @Bean
    public NegociosPublicosApi negociosPublicosApi(RepositorioPublico publico, RepositorioNegocios negocios,
                                                   Clock reloj, TransactionOperations transacciones) {
        return FronteraTransaccional.envolver(NegociosPublicosApi.class, new PublicoServicio(publico, negocios, reloj),
                transacciones);
    }

    @Bean
    public AltasApi altasApiDeNegocios(RepositorioAltas altas, RepositorioNegocios negocios, PublicadorEventos eventos,
                             Clock reloj, TransactionOperations transacciones) {
        return FronteraTransaccional.envolver(AltasApi.class, new AltaServicio(altas, negocios, eventos, reloj),
                transacciones);
    }

    @Bean
    public AvisosDeAlta avisosDeAlta(AltasApi altas) {
        return new AvisosDeAlta(altas);
    }

    /** Los errores de negocio de los resolvedores GraphQL: su codigo y su texto fijos (SG-13). */
    @Bean
    public org.springframework.graphql.execution.DataFetcherExceptionResolver resolvedorDeErroresDeNegocios() {
        return new mx.negocio360.negocios.web.ResolvedorDeErroresDeNegocios();
    }

    @Bean
    public EscuchaDeIdentidad escuchaDeIdentidadEnNegocios(AltasApi altas) {
        return new EscuchaDeIdentidad(altas);
    }
}
