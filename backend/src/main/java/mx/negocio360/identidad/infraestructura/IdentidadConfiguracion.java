package mx.negocio360.identidad.infraestructura;

import java.time.Clock;
import java.util.List;
import mx.negocio360.compartido.seguridad.Autenticador;
import mx.negocio360.compartido.seguridad.CatalogoDePermisos;
import mx.negocio360.compartido.transaccion.FronteraTransaccional;
import mx.negocio360.identidad.dominio.IdentidadException;
import mx.negocio360.compartido.seguridad.RutasPublicas;
import mx.negocio360.compartido.seguridad.SesionDePeticion;
import mx.negocio360.identidad.aplicacion.EquipoServicio;
import mx.negocio360.identidad.aplicacion.PermisosDeIdentidad;
import mx.negocio360.identidad.aplicacion.puertos.PublicadorEventos;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioMembresias;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioPreregistros;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioNegocios;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioRoles;
import mx.negocio360.identidad.aplicacion.puertos.SolicitudesDeNegocio;
import mx.negocio360.identidad.IdentidadApi;
import mx.negocio360.identidad.IdentidadServicio;
import mx.negocio360.identidad.SesionAutenticada;
import mx.negocio360.identidad.aplicacion.PoliticaDeAcceso;
import mx.negocio360.identidad.aplicacion.puertos.CanalDeCodigos;
import mx.negocio360.identidad.aplicacion.puertos.LimiteDeTasa;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioCodigos;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioCuentas;
import mx.negocio360.identidad.aplicacion.puertos.RepositorioSesiones;
import mx.negocio360.identidad.web.CabecerasDeSeguridad;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Cableado de Identidad que existe en todos los perfiles. Necesita un canal, repositorios y un limite de tasa: en
 * desarrollo y demo los da {@link IdentidadDesarrolloConfiguracion}; en produccion todavia no hay implementaciones
 * (Postgres es IDN-1.1 y el canal real IDN-DOC.1), asi que la aplicacion no arranca, a proposito (AS-10). El unico
 * repositorio que ya vive en Postgres es el de pre-registros (D-86), y por eso se arma aqui.
 */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(PropiedadesDePolitica.class)
public class IdentidadConfiguracion {

    @Bean
    public PoliticaDeAcceso politicaDeAcceso(PropiedadesDePolitica propiedades) {
        return propiedades.comoPolitica();
    }

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock relojDelSistema() {
        return Clock.systemUTC();
    }

    /** SI-17: cabeceras de no-cache en todo lo que sirve el acceso. */
    @Bean
    public FilterRegistrationBean<CabecerasDeSeguridad> cabecerasDeSeguridad() {
        var registro = new FilterRegistrationBean<>(new CabecerasDeSeguridad());
        registro.addUrlPatterns("/auth/*", "/cuenta", "/registro", "/registro/*", "/roles", "/permisos", "/dev/*");
        return registro;
    }

    /**
     * SG-01: la sesion de Identidad es la unica forma de autenticarse. La plataforma solo conoce la interfaz
     * {@link Autenticador}; aqui se traduce {@link SesionAutenticada} a la sesion neutral de la peticion.
     */
    @Bean
    public Autenticador autenticadorDeIdentidad(IdentidadApi identidad) {
        return token -> identidad.autenticar(token)
                .map(sesion -> new SesionDePeticion(sesion.cuentaId().toString(), sesion.negocioId(), sesion.permisos()));
    }

    /**
     * Rutas de acceso que validan su propio token (SI-05): la cadena de filtros las deja pasar. El resto de la
     * plataforma exige autenticacion por omision (SG-01).
     */
    @Bean
    public RutasPublicas rutasPublicasDeIdentidad() {
        return () -> List.of("/auth/**", "/registro/**", "/roles", "/permisos", "/cuenta");
    }

    /** Los permisos de Identidad; los demas modulos declaran los suyos con su propio {@link CatalogoDePermisos}. */
    @Bean
    public CatalogoDePermisos permisosDeIdentidad() {
        return new PermisosDeIdentidad();
    }

    /** AZ-03: si un rol base cita un permiso que ningun modulo declara, la aplicacion no arranca. */
    @Bean
    public RepositorioRoles repositorioRoles(List<CatalogoDePermisos> catalogos) {
        return RepositorioRolesEnMemoria.de(catalogos.stream().flatMap(c -> c.permisos().stream()).toList());
    }

    @Bean
    public EquipoServicio equipoServicio(RepositorioMembresias membresias, RepositorioNegocios negocios,
                                         SolicitudesDeNegocio solicitudes, RepositorioRoles roles,
                                         RepositorioSesiones sesiones, RepositorioCuentas cuentas,
                                         PublicadorEventos eventos, Clock reloj) {
        return new EquipoServicio(membresias, negocios, solicitudes, roles, sesiones, cuentas, eventos, reloj);
    }

    /**
     * Escucha el aviso del modulo de negocios (D-78). Es logica de produccion, no de desarrollo, y por eso vive aqui.
     * Con {@code @ApplicationModuleListener} solo recibe lo que se publica dentro de una transaccion, y corre en otro
     * hilo y en su propia transaccion: Spring Modulith activa {@code @Async} por si solo. La prueba con el bus real
     * es {@code EventosPorOutboxPostgresTest}.
     */
    @Bean
    public EscuchaDeNegocios escuchaDeNegocios(EquipoServicio equipo) {
        return new EscuchaDeNegocios(equipo);
    }

    /** D-86: los pre-registros viven en Postgres desde ya, en todos los perfiles. */
    @Bean
    public RepositorioPreregistros repositorioPreregistros(JdbcClient jdbc) {
        return new RepositorioPreregistrosJdbc(jdbc);
    }

    /** Borra cada minuto los pre-registros vencidos (D-84). */
    @Bean
    public LimpiezaDePreregistros limpiezaDePreregistros(RepositorioPreregistros preregistros, Clock reloj) {
        return new LimpiezaDePreregistros(preregistros, reloj);
    }

    /**
     * Los eventos de Identidad van al bus de Spring y, con Spring Modulith, al outbox en la transaccion de la
     * operacion. En desarrollo y demo lo reemplaza el publicador que ademas los guarda para el simulador.
     */
    @Bean
    public PublicadorEventos publicadorDeEventosEnBus(ApplicationEventPublisher bus) {
        return new PublicadorEventosEnBus(bus);
    }

    /**
     * Un solo objeto es la API de acceso y la de equipo; los controladores piden la interfaz que usan. Cada llamada es
     * una sola transaccion, que incluye los eventos que publica (outbox). Un error de negocio confirma en lugar de
     * deshacer: un intento fallido de codigo que se cuenta tiene que sobrevivir al error que provoca.
     */
    @Bean
    public IdentidadFachada identidadApi(RepositorioCuentas cuentas, RepositorioCodigos codigos,
                                          RepositorioPreregistros preregistros, RepositorioSesiones sesiones,
                                          CanalDeCodigos canal, LimiteDeTasa limite, Clock reloj,
                                          PoliticaDeAcceso politica, EquipoServicio equipo,
                                          TransactionOperations transacciones) {
        var servicio = new IdentidadServicio(cuentas, codigos, preregistros, sesiones, canal, limite, reloj, politica,
                equipo);
        return FronteraTransaccional.envolver(IdentidadFachada.class, servicio, transacciones,
                IdentidadException.class);
    }
}
