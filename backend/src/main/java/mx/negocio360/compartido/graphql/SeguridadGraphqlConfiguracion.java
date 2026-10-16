package mx.negocio360.compartido.graphql;

import java.util.Optional;
import mx.negocio360.compartido.seguridad.Autenticador;
import mx.negocio360.compartido.seguridad.LimitadorDeTasa;
import mx.negocio360.compartido.seguridad.PropiedadesWebSocket;
import mx.negocio360.compartido.seguridad.SeguridadHttpConfiguracion;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.GraphQlSource;
import mx.negocio360.compartido.seguridad.CatalogoDePermisos;
import mx.negocio360.compartido.seguridad.ConfirmacionReforzada;

/**
 * Pone en el motor de GraphQL los limites de consulta (SG-06, SG-08 y SG-09), la autorizacion por operacion (SG-03,
 * SG-04, SG-19 y SG-21), el enmascarado de errores (SG-13) y el registro por operacion (SG-20); Spring los recoge por su
 * tipo. Los interceptores se ordenan asi, de afuera hacia adentro: autenticacion del WebSocket (solo en conexiones), registro,
 * control de suscripciones (solo WebSocket), limites de solicitud, enmascarado y, por dentro, la entrada de HTTP.
 */
@Configuration
@EnableConfigurationProperties(PropiedadesGraphql.class)
public class SeguridadGraphqlConfiguracion {

    @Bean
    public LimitesDeConsultaInstrumentation limitesDeConsultaDeLaPlataforma(PropiedadesGraphql propiedades) {
        return new LimitesDeConsultaInstrumentation(propiedades);
    }

    /** Los campos que son de la plataforma; los de cada modulo los declara el modulo. */
    @Bean
    public OperacionesDePlataforma operacionesDePlataforma() {
        return new OperacionesDePlataforma();
    }

    /** SG-21: juntas las operaciones que declara cada modulo y los permisos que existen. */
    @Bean
    public TablaDeOperaciones tablaDeOperaciones(ObjectProvider<OperacionesGraphql> fuentes,
                                                 ObjectProvider<CatalogoDePermisos> catalogos) {
        var permisos = catalogos.orderedStream().flatMap(catalogo -> catalogo.permisos().stream()).toList();
        return new TablaDeOperaciones(fuentes.orderedStream().toList(), permisos);
    }

    /** SG-21: si la tabla y el esquema no coinciden, la aplicacion no arranca. */
    @Bean
    public VerificadorDeOperaciones verificadorDeOperaciones(TablaDeOperaciones tabla, GraphQlSource fuente) {
        return new VerificadorDeOperaciones(tabla, fuente);
    }

    /**
     * SG-03, SG-04 y SG-19. Sin una {@link ConfirmacionReforzada} (IDN-3.2 aun no existe) se usa una que no confirma
     * nada: toda operacion sensible se niega.
     */
    @Bean
    public AutorizacionDeOperacionesInstrumentation autorizacionDeOperaciones(TablaDeOperaciones tabla,
                                                                              ObjectProvider<ConfirmacionReforzada> confirmaciones) {
        return new AutorizacionDeOperacionesInstrumentation(tabla,
                confirmaciones.getIfAvailable(() -> (cuenta, negocio, permiso) -> false));
    }

    /** Los traductores de errores de negocio los aporta cada modulo como un bean; esto solo los junta. */
    @Bean
    public ResolvedorDeErroresGraphql resolvedorDeErrores(ObjectProvider<TraductorDeErrores> traductores) {
        return new ResolvedorDeErroresGraphql(traductores.orderedStream().toList());
    }

    /** SG-13 en las suscripciones: la misma regla, para los errores del flujo de eventos. */
    @Bean
    public ResolvedorDeErroresDeSuscripcion resolvedorDeErroresDeSuscripcion(
            ObjectProvider<TraductorDeErrores> traductores) {
        return new ResolvedorDeErroresDeSuscripcion(traductores.orderedStream().toList());
    }

    /** SG-15: la autenticacion de una conexion WebSocket, en connection_init. */
    @Bean
    public AutenticacionWebSocketInterceptor autenticacionWebSocket(ObjectProvider<Autenticador> autenticadores,
            @Qualifier(SeguridadHttpConfiguracion.LIMITE_DE_FALLOS_POR_IP) LimitadorDeTasa fallosPorIp,
            @Qualifier(SeguridadHttpConfiguracion.LIMITE_DE_PETICIONES_POR_CUENTA) LimitadorDeTasa peticionesPorCuenta) {
        // Sin autenticador (un contexto sin Identidad) nadie tiene sesion: falla cerrado.
        return new AutenticacionWebSocketInterceptor(autenticadores.getIfAvailable(() -> token -> Optional.empty()),
                fallosPorIp, peticionesPorCuenta);
    }

    /** SG-15: el tope de operaciones abiertas por cuenta. */
    @Bean
    public ControlDeSuscripciones controlDeSuscripciones(PropiedadesWebSocket propiedades) {
        return new ControlDeSuscripciones(propiedades.suscripcionesPorCuenta());
    }

    @Bean
    public ControlDeSuscripcionesInterceptor controlDeSuscripcionesInterceptor(ControlDeSuscripciones control) {
        return new ControlDeSuscripcionesInterceptor(control);
    }

    @Bean
    public EnmascaradoDeErroresInterceptor enmascaradoDeErrores() {
        return new EnmascaradoDeErroresInterceptor();
    }

    @Bean
    public RegistroDeOperacionesInterceptor registroDeOperaciones() {
        return new RegistroDeOperacionesInterceptor();
    }

    @Bean
    public LimitesDeSolicitudInterceptor limitesDeSolicitud(PropiedadesGraphql propiedades) {
        return new LimitesDeSolicitudInterceptor(propiedades);
    }
}
