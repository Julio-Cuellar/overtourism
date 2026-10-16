package mx.negocio360.compartido.seguridad;

import jakarta.servlet.DispatcherType;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import mx.negocio360.compartido.graphql.EntradaGraphQl;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.security.web.access.intercept.AuthorizationFilter;

/**
 * Cadena de filtros de toda la superficie HTTP (IDN-4, SG-01). Falla cerrado: lo que ningun modulo declare con
 * {@link RutasPublicas} exige autenticacion.
 *
 * <p>Sin estado y sin cookies: el token viaja solo en {@code Authorization: Bearer} (SI-05), por eso CSRF queda
 * desactivado; no hay credencial que el navegador adjunte por su cuenta. Ni formulario de acceso, ni autenticacion
 * basica, ni cierre de sesion de Spring: la sesion es la de Identidad. Las cabeceras de seguridad de Spring Security
 * (sin cache, sin adivinar el tipo, sin marcos) quedan activas.
 */
@Configuration
@EnableWebSecurity
@EnableConfigurationProperties({PropiedadesHttp.class, PropiedadesTasa.class, PropiedadesWebSocket.class})
public class SeguridadHttpConfiguracion {

    public static final String LIMITE_DE_FALLOS_POR_IP = "limitadorDeFallosPorIp";
    public static final String LIMITE_DE_PETICIONES_POR_CUENTA = "limitadorDePeticionesPorCuenta";
    public static final String LIMITE_DE_CONEXIONES_POR_IP = "limitadorDeConexionesPorIp";

    /** SG-10: intentos sin sesion valida por IP, en HTTP y al iniciar una conexion WebSocket. Uno solo para ambos. */
    @Bean(LIMITE_DE_FALLOS_POR_IP)
    public LimitadorDeTasa limitadorDeFallosPorIp(PropiedadesTasa tasa, ObjectProvider<Clock> relojes) {
        return new LimitadorDeTasa(tasa.fallosPorMinutoIp(), Duration.ofMinutes(1), tasa.maximoDeClaves(),
                relojes.getIfAvailable(Clock::systemUTC));
    }

    /** SG-10: peticiones por cuenta, en HTTP y por cada operacion de una conexion WebSocket. */
    @Bean(LIMITE_DE_PETICIONES_POR_CUENTA)
    public LimitadorDeTasa limitadorDePeticionesPorCuenta(PropiedadesTasa tasa, ObjectProvider<Clock> relojes) {
        return new LimitadorDeTasa(tasa.peticionesPorMinutoCuenta(), Duration.ofMinutes(1), tasa.maximoDeClaves(),
                relojes.getIfAvailable(Clock::systemUTC));
    }

    /** SG-15: conexiones WebSocket nuevas por IP. */
    @Bean(LIMITE_DE_CONEXIONES_POR_IP)
    public LimitadorDeTasa limitadorDeConexionesPorIp(PropiedadesWebSocket websocket, PropiedadesTasa tasa,
                                                      ObjectProvider<Clock> relojes) {
        return new LimitadorDeTasa(websocket.conexionesPorMinutoIp(), Duration.ofMinutes(1), tasa.maximoDeClaves(),
                relojes.getIfAvailable(Clock::systemUTC));
    }

    /**
     * SG-12: la documentacion interactiva solo existe con el perfil de desarrollo. Fuera de el estas rutas no existen y deben
     * responder 404, no 401; con el perfil, son publicas como el resto de lo de desarrollo.
     */
    @Bean
    public RutasPublicas rutasPublicasDeDocumentacion() {
        return () -> List.of("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**");
    }

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http, List<RutasPublicas> publicas,
                                                 ObjectProvider<Autenticador> autenticadores,
                                                 PropiedadesHttp propiedades,
                                                 @Qualifier(LIMITE_DE_FALLOS_POR_IP) LimitadorDeTasa fallosPorIp,
                                                 @Qualifier(LIMITE_DE_PETICIONES_POR_CUENTA) LimitadorDeTasa peticionesPorCuenta,
                                                 @Qualifier(LIMITE_DE_CONEXIONES_POR_IP) LimitadorDeTasa conexionesPorIp)
            throws Exception {
        var patrones = publicas.stream().flatMap(rutas -> rutas.patrones().stream()).toArray(String[]::new);
        // Sin autenticador (un contexto sin Identidad) nadie tiene sesion: falla cerrado.
        var autenticador = autenticadores.getIfAvailable(() -> token -> Optional.empty());
        // SG-15: el apreton de manos de un WebSocket pasa sin sesion; la autenticacion es el mensaje connection_init.
        RequestMatcher subidaWebSocket = peticion -> SubidaWebSocket.es(peticion)
                && PathPatternRequestMatcher.withDefaults().matcher("/graphql").matches(peticion);
        var omitidasPorElBearer = java.util.stream.Stream.concat(java.util.Arrays.stream(patrones), java.util.stream.Stream.of("/graphql"))
                .toList();
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(cache -> cache.disable())
                .authorizeHttpRequests(reglas -> reglas
                        // El despacho de error no es una peticion nueva: sin esto, un 404 o un 405 saldrian como 401.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(patrones).permitAll()
                        .requestMatchers(subidaWebSocket).permitAll()
                        // SG-01: /graphql solo recibe peticiones que ya paso el filtro de entrada (FiltroDeEntrada), que
                        // autentica el token y deja la EntradaGraphQl. Si ese filtro no corrio, 401: falla cerrado. Que
                        // la persona tenga sesion, negocio y permiso lo decide la puerta de cada operacion, porque
                        // hay operaciones publicas (la app del turista) y de cuenta.
                        .requestMatchers("/graphql").access((autenticacion, contexto) -> new AuthorizationDecision(
                                contexto.getRequest().getAttribute(EntradaGraphQl.ATRIBUTO) instanceof EntradaGraphQl))
                        .anyRequest().authenticated())
                // Los filtros quedan en este orden (lo fija OrdenDeFiltrosTest). Spring Security ordena por clase y, a igual
                // posicion, por orden de registro. A /graphql ya llego el filtro de entrada (FiltroDeEntrada), que corre antes
                // de toda esta cadena: valido metodo, tipo, tamano y tasa y autentico el token. Lo que sigue lo repite o lo
                // completa (origen, conexiones de WebSocket) y es lo que protege al resto de la API (REST).
                // 1. SG-14 y SG-09: la puerta HTTP; el origen de POST y de la subida de WebSocket es lo que solo se verifica aqui.
                .addFilterBefore(new PeticionGraphqlEstrictaFilter(propiedades), ExceptionTranslationFilter.class)
                // 2. SG-10: intentos sin sesion valida por IP, para las rutas REST. Va antes de ExceptionTranslationFilter, que
                //    es quien escribe el 401 cuando no hay token: asi ve la respuesta final y puede contarla. Para /graphql
                //    cuenta el filtro de entrada; aqui solo ve el 401 de una peticion que no paso por el.
                .addFilterBefore(new LimiteDeTasaPorIpFilter(fallosPorIp), ExceptionTranslationFilter.class)
                // 2b. SG-15: conexiones WebSocket nuevas por IP.
                .addFilterBefore(new LimiteDeConexionesWebSocketFilter(conexionesPorIp), ExceptionTranslationFilter.class)
                // 3. SG-01: la sesion de Identidad.
                //    /graphql no pasa por aqui: su filtro de entrada ya autentico el token (y cuenta sus fallos y su tasa).
                .addFilterBefore(new BearerTokenFilter(autenticador, omitidasPorElBearer), AuthorizationFilter.class)
                // 4. SG-10: peticiones por cuenta de las rutas REST autenticadas (en /graphql las cuenta el filtro de entrada).
                .addFilterBefore(new LimiteDeTasaPorCuentaFilter(peticionesPorCuenta), AuthorizationFilter.class)
                .exceptionHandling(errores -> errores
                        .authenticationEntryPoint(new EntradaNoAutenticada())
                        .accessDeniedHandler(new PermisoDenegadoHttp()))
                .build();
    }

    /**
     * Spring Boot crea un usuario local con contrasena generada si no hay ningun {@link UserDetailsService}. Aqui no
     * hay usuarios locales: el unico acceso es la sesion de Identidad, asi que se declara uno que no conoce a nadie.
     */
    @Bean
    public UserDetailsService sinUsuariosLocales() {
        return usuario -> {
            throw new UsernameNotFoundException("No hay usuarios locales");
        };
    }
}
